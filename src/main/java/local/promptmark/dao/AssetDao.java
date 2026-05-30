package local.promptmark.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import local.promptmark.dto.Asset;
import local.promptmark.dto.AssetStatus;
import local.promptmark.dto.AssetType;

/**
 * SQL-only access to the {@code assets} table.
 *
 * <p>Search uses ILIKE for case-insensitive title/summary matching and joins
 * tags through {@code asset_tags} when a tag filter is supplied. Sort modes
 * are {@code "recent"} (default) and {@code "popular"} (download_count desc).
 */
public class AssetDao {

    /** Standard list of asset columns used for SELECT * style mappings. */
    private static final String COLS =
        "id, seller_id, type, title, summary, body, file_key, demo_url, video_url, " +
        "price, status, view_count, download_count, created_at, updated_at";

    private final DataSource ds;

    public AssetDao(DataSource ds) {
        this.ds = ds;
    }

    /** Looks up an asset by id, excluding soft-deleted rows. */
    public Optional<Asset> findById(long id) {
        String sql = "SELECT " + COLS + " FROM assets WHERE id = ? AND status <> 'DELETED'";
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
    public Optional<Asset> findByIdForOwner(long id, long sellerId) {
        String sql = "SELECT " + COLS + " FROM assets " +
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

    /** Inserts a new asset row. Returns the generated id. */
    public long insert(Asset a) {
        String sql = "INSERT INTO assets " +
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
            ps.setString(10, a.getStatus() == null ? AssetStatus.PUBLIC.name() : a.getStatus().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("INSERT RETURNING produced no row");
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert asset failed: " + e.getMessage(), e);
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
        String sql = "UPDATE assets SET " +
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
            throw new RuntimeException("update asset failed: " + e.getMessage(), e);
        }
    }

    /** Soft-delete: mark as DELETED. Underlying row stays for FK integrity. */
    public void softDelete(long id) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE assets SET status = 'DELETED', updated_at = now() WHERE id = ?")) {
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
                 "UPDATE assets SET view_count = view_count + 1 WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("incrementViewCount failed: " + e.getMessage(), e);
        }
    }

    /** Transactional variant for the download flow. */
    public void incrementDownloadCount(long id, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE assets SET download_count = download_count + 1 WHERE id = ?")) {
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
    public List<Asset> search(String q,
                              AssetType type,
                              String tag,
                              String sort,
                              int offset,
                              int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT DISTINCT a.").append(COLS.replace(", ", ", a.")).append(" FROM assets a ");
        List<Object> params = new ArrayList<>();
        boolean joinTags = tag != null && !tag.isEmpty();
        if (joinTags) {
            sb.append("JOIN asset_tags at ON at.asset_id = a.id ");
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
                List<Asset> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("search failed: " + e.getMessage(), e);
        }
    }

    /** Row count matching the same search filters (no LIMIT/OFFSET). */
    public int countSearch(String q, AssetType type, String tag) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(DISTINCT a.id) FROM assets a ");
        List<Object> params = new ArrayList<>();
        boolean joinTags = tag != null && !tag.isEmpty();
        if (joinTags) {
            sb.append("JOIN asset_tags at ON at.asset_id = a.id ");
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

    /** Seller's recently-created PUBLIC/HIDDEN assets (DELETED excluded). */
    public List<Asset> findBySellerId(long sellerId, int limit) {
        String sql = "SELECT " + COLS + " FROM assets " +
                     "WHERE seller_id = ? AND status <> 'DELETED' " +
                     "ORDER BY created_at DESC, id DESC LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Asset> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findBySellerId failed: " + e.getMessage(), e);
        }
    }

    private static void setNullable(PreparedStatement ps, int idx, String value)
            throws SQLException {
        if (value == null) {
            ps.setNull(idx, java.sql.Types.VARCHAR);
        } else {
            ps.setString(idx, value);
        }
    }

    private static Asset map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return new Asset(
            rs.getLong("id"),
            rs.getLong("seller_id"),
            AssetType.fromDb(rs.getString("type")),
            rs.getString("title"),
            rs.getString("summary"),
            rs.getString("body"),
            rs.getString("file_key"),
            rs.getString("demo_url"),
            rs.getString("video_url"),
            rs.getInt("price"),
            AssetStatus.fromDb(rs.getString("status")),
            rs.getInt("view_count"),
            rs.getInt("download_count"),
            created == null ? null : created.toInstant(),
            updated == null ? null : updated.toInstant()
        );
    }
}
