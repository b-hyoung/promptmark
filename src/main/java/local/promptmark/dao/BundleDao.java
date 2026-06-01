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

import com.pgvector.PGvector;

import local.promptmark.dto.Bundle;
import local.promptmark.dto.BundleStatus;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.PluginType;

/**
 * SQL-only access to the {@code bundles} table and its N:N mapping {@code bundle_plugin}.
 */
public class BundleDao {

    private static final String COLS =
        "id, curator_id, slug, name, tagline, story, price, thumbnail, " +
        "status, view_count, created_at, updated_at";

    private final DataSource ds;

    public BundleDao(DataSource ds) {
        this.ds = ds;
    }

    public Optional<Bundle> findById(long id) {
        String sql = "SELECT " + COLS + " FROM bundles WHERE id = ? AND status <> 'DELETED'";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("bundle findById failed: " + id, e);
        }
    }

    public Optional<Bundle> findBySlug(String slug) {
        String sql = "SELECT " + COLS + " FROM bundles WHERE slug = ? AND status <> 'DELETED'";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("bundle findBySlug failed: " + slug, e);
        }
    }

    /** Bundle + its plugins (sorted by display_order ASC, plugin.id ASC). */
    public Optional<Bundle> findByIdWithPlugins(long id) {
        Optional<Bundle> opt = findById(id);
        if (opt.isEmpty()) return opt;
        Bundle b = opt.get();
        b.setPlugins(loadPlugins(b.getId()));
        return Optional.of(b);
    }

    private List<Plugin> loadPlugins(long bundleId) {
        String sql =
            "SELECT p.id, p.seller_id, p.type, p.title, p.summary, p.body, p.file_key, " +
            "       p.demo_url, p.video_url, p.price, p.status, p.view_count, p.download_count, " +
            "       p.created_at, p.updated_at " +
            "FROM plugins p " +
            "JOIN bundle_plugin bp ON bp.plugin_id = p.id " +
            "WHERE bp.bundle_id = ? AND p.status <> 'DELETED' " +
            "ORDER BY bp.display_order ASC, p.id ASC";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, bundleId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Plugin> out = new ArrayList<>();
                while (rs.next()) out.add(mapPlugin(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("bundle loadPlugins failed: " + bundleId, e);
        }
    }

    /** Catalogue list, PUBLIC only. Sort: 'recent' (default) or 'popular' (view_count). */
    public List<Bundle> list(String sort, int offset, int limit) {
        String order = "popular".equalsIgnoreCase(sort)
            ? "view_count DESC, id DESC"
            : "created_at DESC, id DESC";
        String sql = "SELECT " + COLS + " FROM bundles " +
                     "WHERE status = 'PUBLIC' ORDER BY " + order + " LIMIT ? OFFSET ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Bundle> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("bundle list failed", e);
        }
    }

    public int countPublic() {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM bundles WHERE status='PUBLIC'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("bundle countPublic failed", e);
        }
    }

    /** Insert a new bundle, returns generated id. Used by Service in a transaction. */
    public long insert(Connection conn, Bundle b) throws SQLException {
        String sql = "INSERT INTO bundles " +
                     "(curator_id, slug, name, tagline, story, price, thumbnail, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (b.getCuratorId() == null) ps.setNull(1, java.sql.Types.BIGINT);
            else ps.setLong(1, b.getCuratorId());
            ps.setString(2, b.getSlug());
            ps.setString(3, b.getName());
            setNullable(ps, 4, b.getTagline());
            setNullable(ps, 5, b.getStory());
            ps.setInt(6, b.getPrice());
            setNullable(ps, 7, b.getThumbnail());
            ps.setString(8, b.getStatus() == null ? BundleStatus.PUBLIC.name() : b.getStatus().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("INSERT RETURNING produced no row");
                return rs.getLong(1);
            }
        }
    }

    public void update(Connection conn, long id, String name, String tagline, String story,
                       int price, String thumbnail) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE bundles SET name=?, tagline=?, story=?, price=?, thumbnail=?, updated_at=now() WHERE id=?")) {
            ps.setString(1, name);
            setNullable(ps, 2, tagline);
            setNullable(ps, 3, story);
            ps.setInt(4, price);
            setNullable(ps, 5, thumbnail);
            ps.setLong(6, id);
            ps.executeUpdate();
        }
    }

    public void softDelete(long id) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE bundles SET status='DELETED', updated_at=now() WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("bundle softDelete failed: " + id, e);
        }
    }

    public void incrementViewCount(long id) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE bundles SET view_count = view_count + 1 WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) { /* best-effort */ }
    }

    /** Replace the full plugin mapping for a bundle (delete + insert in same tx). */
    public void replacePlugins(Connection conn, long bundleId, List<Long> pluginIds) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM bundle_plugin WHERE bundle_id=?")) {
            del.setLong(1, bundleId);
            del.executeUpdate();
        }
        if (pluginIds == null || pluginIds.isEmpty()) return;
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO bundle_plugin (bundle_id, plugin_id, display_order) VALUES (?, ?, ?)")) {
            int order = 0;
            for (Long pid : pluginIds) {
                ins.setLong(1, bundleId);
                ins.setLong(2, pid);
                ins.setInt(3, order++);
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    public void updateEmbedding(long bundleId, float[] vector) {
        if (vector == null) throw new IllegalArgumentException("vector must not be null");
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE bundles SET embedding = ? WHERE id = ?")) {
            ps.setObject(1, new PGvector(vector));
            ps.setLong(2, bundleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("bundle updateEmbedding failed: " + bundleId, e);
        }
    }

    private static void setNullable(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, value);
    }

    private static Bundle map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        long curatorRaw = rs.getLong("curator_id");
        Long curator = rs.wasNull() ? null : curatorRaw;
        return new Bundle(
            rs.getLong("id"),
            curator,
            rs.getString("slug"),
            rs.getString("name"),
            rs.getString("tagline"),
            rs.getString("story"),
            rs.getInt("price"),
            rs.getString("thumbnail"),
            BundleStatus.fromDb(rs.getString("status")),
            rs.getInt("view_count"),
            created == null ? null : created.toInstant(),
            updated == null ? null : updated.toInstant()
        );
    }

    private static Plugin mapPlugin(ResultSet rs) throws SQLException {
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
