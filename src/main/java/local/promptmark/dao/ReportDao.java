package local.promptmark.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Insert-only DAO for {@code reports}. Admin queue read APIs live in Phase 5.
 */
public class ReportDao {

    private final DataSource ds;

    public ReportDao(DataSource ds) {
        this.ds = ds;
    }

    public void insert(long reporterId, long assetId, String reason) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO reports (reporter_id, asset_id, reason) VALUES (?, ?, ?)")) {
            ps.setLong(1, reporterId);
            ps.setLong(2, assetId);
            ps.setString(3, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insert report failed: " + e.getMessage(), e);
        }
    }
}
