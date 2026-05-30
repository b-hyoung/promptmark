package local.promptmark.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import local.promptmark.dto.Asset;
import local.promptmark.dto.AssetStatus;
import local.promptmark.dto.AssetType;

/**
 * Recorded downloads. Insert participates in the caller's transaction; the
 * recent-list query is read-only and uses the pool directly.
 */
public class DownloadDao {

    private final DataSource ds;

    public DownloadDao(DataSource ds) {
        this.ds = ds;
    }

    public void insert(long userId, long assetId, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO downloads (user_id, asset_id) VALUES (?, ?)")) {
            ps.setLong(1, userId);
            ps.setLong(2, assetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insert download failed: " + e.getMessage(), e);
        }
    }

    /**
     * Most recently downloaded assets for the user. Deduplicates by asset id
     * (a user who pulled the same file twice still sees it once).
     */
    public List<Asset> findRecentByUser(long userId, int limit) {
        String sql = "SELECT a.id, a.seller_id, a.type, a.title, a.summary, a.body, " +
                     "       a.file_key, a.demo_url, a.video_url, a.price, a.status, " +
                     "       a.view_count, a.download_count, a.created_at, a.updated_at, " +
                     "       MAX(d.downloaded_at) AS last_at " +
                     "FROM downloads d JOIN assets a ON a.id = d.asset_id " +
                     "WHERE d.user_id = ? AND a.status <> 'DELETED' " +
                     "GROUP BY a.id " +
                     "ORDER BY last_at DESC LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Asset> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findRecentByUser failed: " + e.getMessage(), e);
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
