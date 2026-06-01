package local.promptmark.dao;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import com.pgvector.PGvector;

import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.PluginType;
import local.promptmark.service.llm.PluginCard;

/**
 * SQL-only access to the {@code plugins} table.
 *
 * <p>Search uses ILIKE for case-insensitive title/summary matching and joins
 * tags through {@code plugin_tags} when a tag filter is supplied. Sort modes
 * are {@code "recent"} (default) and {@code "popular"} (download_count desc).
 */
public class PluginDao {

    /** Standard list of plugin columns used for SELECT * style mappings. */
    private static final String COLS =
        "id, seller_id, type, title, summary, body, file_key, demo_url, video_url, " +
        "price, status, view_count, download_count, created_at, updated_at";

    private final DataSource ds;

    public PluginDao(DataSource ds) {
        this.ds = ds;
    }

    /** Looks up an plugin by id, excluding soft-deleted rows. */
    public Optional<Plugin> findById(long id) {
        String sql = "SELECT " + COLS + " FROM plugins WHERE id = ? AND status <> 'DELETED'";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + e.getMessage(), e);
        }
    }

    /**
     * Strict owner-scoped lookup used for edit / delete authorisation. Returns
     * empty if the row belongs to a different seller or is soft-deleted.
     */
    public Optional<Plugin> findByIdForOwner(long id, long sellerId) {
        String sql = "SELECT " + COLS + " FROM plugins " +
                     "WHERE id = ? AND seller_id = ? AND status <> 'DELETED'";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByIdForOwner failed: " + e.getMessage(), e);
        }
    }

    /** Inserts a new plugin row. Returns the generated id. */
    public long insert(Plugin a) {
        String sql = "INSERT INTO plugins " +
                     "(seller_id, type, title, summary, body, file_key, demo_url, video_url, " +
                     " price, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "RETURNING id";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getSellerId());
            ps.setString(2, a.getType().name());
            ps.setString(3, a.getTitle());
            ps.setString(4, a.getSummary());
            setNullable(ps, 5, a.getBody());
            setNullable(ps, 6, a.getFileKey());
            setNullable(ps, 7, a.getDemoUrl());
            setNullable(ps, 8, a.getVideoUrl());
            ps.setInt(9, a.getPrice());
            ps.setString(10, a.getStatus() == null ? PluginStatus.PUBLIC.name() : a.getStatus().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("INSERT RETURNING produced no row");
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert plugin failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update mutable fields and bump {@code updated_at}. Type and seller_id are
     * not editable; tags are managed separately via {@link TagDao}.
     */
    public void update(long id,
                       String title,
                       String summary,
                       String body,
                       String fileKey,
                       String demoUrl,
                       String videoUrl,
                       int price) {
        String sql = "UPDATE plugins SET " +
                     "title = ?, summary = ?, body = ?, file_key = ?, " +
                     "demo_url = ?, video_url = ?, price = ?, updated_at = now() " +
                     "WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, summary);
            setNullable(ps, 3, body);
            setNullable(ps, 4, fileKey);
            setNullable(ps, 5, demoUrl);
            setNullable(ps, 6, videoUrl);
            ps.setInt(7, price);
            ps.setLong(8, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update plugin failed: " + e.getMessage(), e);
        }
    }

    /** Soft-delete: mark as DELETED. Underlying row stays for FK integrity. */
    public void softDelete(long id) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE plugins SET status = 'DELETED', updated_at = now() WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("softDelete failed: " + e.getMessage(), e);
        }
    }

    /** Bumps view_count by 1. Non-transactional — fire-and-forget for detail view. */
    public void incrementViewCount(long id) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE plugins SET view_count = view_count + 1 WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("incrementViewCount failed: " + e.getMessage(), e);
        }
    }

    /** Transactional variant for the download flow. */
    public void incrementDownloadCount(long id, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE plugins SET download_count = download_count + 1 WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("incrementDownloadCount failed: " + e.getMessage(), e);
        }
    }

    /**
     * Filtered, paginated catalogue search.
     *
     * @param q     keyword (matched against title/summary via ILIKE) — null skips
     * @param type  exact type filter — null skips
     * @param tag   exact tag name — null skips
     * @param sort  "recent" (default) or "popular"
     */
    public List<Plugin> search(String q,
                              PluginType type,
                              String tag,
                              String sort,
                              int offset,
                              int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT DISTINCT a.").append(COLS.replace(", ", ", a.")).append(" FROM plugins a ");
        List<Object> params = new ArrayList<>();
        boolean joinTags = tag != null && !tag.isEmpty();
        if (joinTags) {
            sb.append("JOIN plugin_tags at ON at.plugin_id = a.id ");
            sb.append("JOIN tags t ON t.id = at.tag_id ");
        }
        sb.append("WHERE a.status = 'PUBLIC' ");
        if (q != null && !q.isEmpty()) {
            sb.append("AND (a.title ILIKE ? OR a.summary ILIKE ?) ");
            String like = "%" + q + "%";
            params.add(like);
            params.add(like);
        }
        if (type != null) {
            sb.append("AND a.type = ? ");
            params.add(type.name());
        }
        if (joinTags) {
            sb.append("AND t.name = ? ");
            params.add(tag);
        }
        if ("popular".equalsIgnoreCase(sort)) {
            sb.append("ORDER BY a.download_count DESC, a.id DESC ");
        } else {
            sb.append("ORDER BY a.created_at DESC, a.id DESC ");
        }
        sb.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Plugin> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("search failed: " + e.getMessage(), e);
        }
    }

    /** Row count matching the same search filters (no LIMIT/OFFSET). */
    public int countSearch(String q, PluginType type, String tag) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(DISTINCT a.id) FROM plugins a ");
        List<Object> params = new ArrayList<>();
        boolean joinTags = tag != null && !tag.isEmpty();
        if (joinTags) {
            sb.append("JOIN plugin_tags at ON at.plugin_id = a.id ");
            sb.append("JOIN tags t ON t.id = at.tag_id ");
        }
        sb.append("WHERE a.status = 'PUBLIC' ");
        if (q != null && !q.isEmpty()) {
            sb.append("AND (a.title ILIKE ? OR a.summary ILIKE ?) ");
            String like = "%" + q + "%";
            params.add(like);
            params.add(like);
        }
        if (type != null) {
            sb.append("AND a.type = ? ");
            params.add(type.name());
        }
        if (joinTags) {
            sb.append("AND t.name = ? ");
            params.add(tag);
        }
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("countSearch failed: " + e.getMessage(), e);
        }
    }

    /** Seller's recently-created PUBLIC/HIDDEN plugins (DELETED excluded). */
    public List<Plugin> findBySellerId(long sellerId, int limit) {
        String sql = "SELECT " + COLS + " FROM plugins " +
                     "WHERE seller_id = ? AND status <> 'DELETED' " +
                     "ORDER BY created_at DESC, id DESC LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Plugin> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findBySellerId failed: " + e.getMessage(), e);
        }
    }

    // ───── hybrid RAG (Phase 4) ──────────────────────────────────────

    /** Per-JVM latch ensuring we register pgvector's type mapping only once. */
    private static final AtomicBoolean PGVECTOR_REGISTERED = new AtomicBoolean(false);

    /**
     * Update the {@code embedding} column for a single plugin. Uses the
     * pgvector-java helper to bind the float[] as a {@code vector} value.
     *
     * @throws RuntimeException on any SQL failure (caller is expected to log
     *                          and continue — embedding is best-effort).
     */
    public void updateEmbedding(long pluginId, float[] vector) {
        if (vector == null) {
            throw new IllegalArgumentException("vector must not be null");
        }
        try (Connection c = ds.getConnection()) {
            ensurePgVectorRegistered(c);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE plugins SET embedding = ? WHERE id = ?")) {
                ps.setObject(1, new PGvector(vector));
                ps.setLong(2, pluginId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("updateEmbedding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Hybrid keyword + vector search for the chat agent.
     *
     * <p><b>Implementation choice:</b> we pull a 1st-stage candidate list of
     * up to 50 PUBLIC rows by keyword match (title/summary ILIKE OR tag exact
     * match), then re-rank in Java. Combining the keyword score with cosine
     * similarity (computed in Java against the embedding column) is simpler
     * and more portable than mixing the pgvector {@code <=>} operator into the
     * SQL — it also keeps the code path the same when the embedding column is
     * NULL or when no query vector is supplied.
     *
     * <p>The final score is {@code max(keywordScore, vectorScore)} clipped to
     * [0,1], so a strong keyword match never gets buried by a slightly better
     * vector match (and vice versa).
     *
     * @param keywords     tokenised user query (each token gets wildcards added)
     * @param queryVector  embedding of the user query, or null to skip vector ranking
     * @param typeOrNull   optional plugin-type filter
     * @param maxPriceOrNull optional max-price filter (inclusive)
     * @param limit        max rows in the result (clamped to ≥1)
     */
    public List<PluginCard> searchHybrid(String[] keywords,
                                        float[] queryVector,
                                        PluginType typeOrNull,
                                        Integer maxPriceOrNull,
                                        int limit) {
        boolean haveKeywords = keywords != null && keywords.length > 0;
        boolean haveVector = queryVector != null && queryVector.length > 0;
        if (!haveKeywords && !haveVector) {
            return new ArrayList<>();
        }
        int safeLimit = Math.max(1, limit);

        List<Candidate> candidates = fetchCandidates(keywords, typeOrNull, maxPriceOrNull);

        // Tokenise for downstream scoring (lowercased).
        String[] lowered = haveKeywords ? toLower(keywords) : new String[0];

        // Score each candidate.
        for (Candidate cand : candidates) {
            double keywordScore = keywordScore(lowered, cand.title, cand.summary, cand.tags);
            double vectorScore = (haveVector && cand.embedding != null)
                ? cosineSimilarity(queryVector, cand.embedding)
                : 0.0;
            cand.score = clipScore(Math.max(keywordScore, vectorScore));
        }
        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        List<PluginCard> out = new ArrayList<>();
        for (int i = 0; i < Math.min(safeLimit, candidates.size()); i++) {
            Candidate c = candidates.get(i);
            out.add(new PluginCard(c.id, c.type, c.title, c.summary, c.price,
                c.score, c.tags, null));
        }
        return out;
    }

    private List<Candidate> fetchCandidates(String[] keywords,
                                            PluginType typeOrNull,
                                            Integer maxPriceOrNull) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT a.id, a.type, a.title, a.summary, a.price, a.download_count, ");
        sb.append("       a.embedding, ");
        sb.append("       COALESCE(ARRAY_AGG(t.name) FILTER (WHERE t.name IS NOT NULL), ARRAY[]::text[]) AS tag_names ");
        sb.append("FROM plugins a ");
        sb.append("LEFT JOIN plugin_tags at ON at.plugin_id = a.id ");
        sb.append("LEFT JOIN tags t        ON t.id = at.tag_id ");
        sb.append("WHERE a.status = 'PUBLIC' ");

        List<Object> params = new ArrayList<>();
        boolean haveKeywords = keywords != null && keywords.length > 0;
        String[] likes = haveKeywords ? toLikePatterns(keywords) : new String[0];

        if (haveKeywords) {
            sb.append("AND (a.title ILIKE ANY(?) OR a.summary ILIKE ANY(?) OR t.name = ANY(?)) ");
            params.add(likes);  // for title
            params.add(likes);  // for summary
            params.add(keywords); // for tag exact match
        }
        if (typeOrNull != null) {
            sb.append("AND a.type = ? ");
            params.add(typeOrNull.name());
        }
        if (maxPriceOrNull != null) {
            sb.append("AND a.price <= ? ");
            params.add(maxPriceOrNull.intValue());
        }
        sb.append("GROUP BY a.id ");
        sb.append("LIMIT 50");

        try (Connection c = ds.getConnection()) {
            ensurePgVectorRegistered(c);
            try (PreparedStatement ps = c.prepareStatement(sb.toString())) {
                int idx = 1;
                for (Object p : params) {
                    if (p instanceof String[]) {
                        Array arr = c.createArrayOf("text", (String[]) p);
                        ps.setArray(idx++, arr);
                    } else {
                        ps.setObject(idx++, p);
                    }
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<Candidate> out = new ArrayList<>();
                    while (rs.next()) {
                        Candidate cand = new Candidate();
                        cand.id = rs.getLong("id");
                        cand.type = rs.getString("type");
                        cand.title = rs.getString("title");
                        cand.summary = rs.getString("summary");
                        cand.price = rs.getInt("price");
                        // embedding column
                        Object emb = rs.getObject("embedding");
                        if (emb instanceof PGvector) {
                            cand.embedding = ((PGvector) emb).toArray();
                        } else if (emb != null) {
                            // Fallback path (e.g. JDBC returns the raw string form)
                            cand.embedding = parseVectorString(emb.toString());
                        }
                        Array tagArr = rs.getArray("tag_names");
                        if (tagArr != null) {
                            Object raw = tagArr.getArray();
                            if (raw instanceof Object[]) {
                                List<String> names = new ArrayList<>();
                                for (Object o : (Object[]) raw) {
                                    if (o != null) names.add(o.toString());
                                }
                                cand.tags = names;
                            }
                        }
                        out.add(cand);
                    }
                    return out;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("searchHybrid candidates failed: " + e.getMessage(), e);
        }
    }

    private static String[] toLikePatterns(String[] kws) {
        String[] out = new String[kws.length];
        for (int i = 0; i < kws.length; i++) {
            out[i] = "%" + (kws[i] == null ? "" : kws[i]) + "%";
        }
        return out;
    }

    private static String[] toLower(String[] kws) {
        String[] out = new String[kws.length];
        for (int i = 0; i < kws.length; i++) {
            out[i] = kws[i] == null ? "" : kws[i].toLowerCase();
        }
        return out;
    }

    private static float[] parseVectorString(String s) {
        // Format like "[0.1,0.2,...]"
        if (s == null || s.length() < 2) return null;
        String trimmed = s.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.isEmpty()) return new float[0];
        String[] parts = trimmed.split(",");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return out;
    }

    private static void ensurePgVectorRegistered(Connection c) throws SQLException {
        if (PGVECTOR_REGISTERED.get()) return;
        synchronized (PGVECTOR_REGISTERED) {
            if (PGVECTOR_REGISTERED.get()) return;
            PGvector.addVectorType(c);
            PGVECTOR_REGISTERED.set(true);
        }
    }

    /**
     * Pure helper for unit tests — computes the keyword-overlap score for a
     * single candidate given the user's tokens. Returns a value in [0,1]:
     * (matched tokens) / (tokens).
     *
     * <p>A token "matches" if it occurs (case-insensitive substring) in the
     * title, the summary, or any tag name. Empty inputs return 0.
     */
    public static double keywordScore(String[] loweredTokens,
                                      String title, String summary, List<String> tags) {
        if (loweredTokens == null || loweredTokens.length == 0) return 0.0;
        String t = title == null ? "" : title.toLowerCase();
        String s = summary == null ? "" : summary.toLowerCase();
        List<String> lcTags = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null) lcTags.add(tag.toLowerCase());
            }
        }
        int matched = 0;
        for (String tok : loweredTokens) {
            if (tok == null || tok.isEmpty()) continue;
            boolean hit = t.contains(tok) || s.contains(tok);
            if (!hit) {
                for (String tag : lcTags) {
                    if (tag.contains(tok)) { hit = true; break; }
                }
            }
            if (hit) matched++;
        }
        return ((double) matched) / loweredTokens.length;
    }

    /** Cosine similarity between two vectors. Returns 0 when shapes mismatch. */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) return 0.0;
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /** Clip a raw score into the [0,1] band used for ranking. */
    public static double clipScore(double s) {
        if (Double.isNaN(s) || Double.isInfinite(s)) return 0.0;
        if (s < 0) return 0.0;
        if (s > 1) return 1.0;
        return s;
    }

    /** Internal candidate carrier used by {@link #searchHybrid}. */
    private static final class Candidate {
        long id;
        String type;
        String title;
        String summary;
        int price;
        List<String> tags = new ArrayList<>();
        float[] embedding;
        double score;
    }

    private static void setNullable(PreparedStatement ps, int idx, String value)
            throws SQLException {
        if (value == null) {
            ps.setNull(idx, java.sql.Types.VARCHAR);
        } else {
            ps.setString(idx, value);
        }
    }

    private static Plugin map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return new Plugin(
            rs.getLong("id"),
            rs.getLong("seller_id"),
            PluginType.fromDb(rs.getString("type")),
            rs.getString("title"),
            rs.getString("summary"),
            rs.getString("body"),
            rs.getString("file_key"),
            rs.getString("demo_url"),
            rs.getString("video_url"),
            rs.getInt("price"),
            PluginStatus.fromDb(rs.getString("status")),
            rs.getInt("view_count"),
            rs.getInt("download_count"),
            created == null ? null : created.toInstant(),
            updated == null ? null : updated.toInstant()
        );
    }
}
