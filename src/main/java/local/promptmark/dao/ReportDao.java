package local.promptmark.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.PluginType;
import local.promptmark.dto.ReportRow;

/**
 * SQL access to {@code reports}. Phase 3 left this insert-only; Phase 5 adds
 * the admin queue read APIs and the resolve / reject update path.
 */
public class ReportDao {

    /** Allowed transitions through this DAO. */
    static final String STATUS_RESOLVED = "RESOLVED";
    static final String STATUS_REJECTED = "REJECTED";

    private final DataSource ds;

    public ReportDao(DataSource ds) {
        this.ds = ds;
    }

    public void insert(long reporterId, long pluginId, String reason) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO reports (reporter_id, plugin_id, reason) VALUES (?, ?, ?)")) {
            ps.setLong(1, reporterId);
            ps.setLong(2, pluginId);
            ps.setString(3, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insert report failed: " + e.getMessage(), e);
        }
    }

    /**
     * Open reports for the admin queue, joined with plugins + users so the JSP
     * can render context. Most recent first.
     */
    public List<ReportRow> findOpen(int limit) {
        int safeLimit = Math.max(1, limit);
        String sql =
            "SELECT r.id AS report_id, r.reason, r.created_at AS reported_at, " +
            "       a.id AS plugin_id, a.title AS plugin_title, a.type AS plugin_type, " +
            "       a.status AS plugin_status, " +
            "       u.id AS reporter_id, u.nickname AS reporter_nickname " +
            "FROM reports r " +
            "JOIN plugins a ON a.id = r.plugin_id " +
            "JOIN users  u ON u.id = r.reporter_id " +
            "WHERE r.status = 'OPEN' " +
            "ORDER BY r.created_at DESC, r.id DESC LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReportRow> out = new ArrayList<>();
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("reported_at");
                    out.add(new ReportRow(
                        rs.getLong("report_id"),
                        rs.getString("reason"),
                        ts == null ? null : ts.toInstant(),
                        rs.getLong("plugin_id"),
                        rs.getString("plugin_title"),
                        PluginType.fromDb(rs.getString("plugin_type")),
                        PluginStatus.fromDb(rs.getString("plugin_status")),
                        rs.getLong("reporter_id"),
                        rs.getString("reporter_nickname")
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findOpen failed: " + e.getMessage(), e);
        }
    }

    /**
     * Transition a report from OPEN to either RESOLVED or REJECTED. Participates
     * in the caller's transaction (mirrors {@code OrderDao.insertOrderItem}
     * conventions).
     *
     * @throws IllegalArgumentException when {@code newStatus} is not RESOLVED or REJECTED
     */
    public void resolve(long reportId, String newStatus, Connection conn) {
        if (newStatus == null
                || (!STATUS_RESOLVED.equals(newStatus) && !STATUS_REJECTED.equals(newStatus))) {
            throw new IllegalArgumentException(
                "newStatus must be RESOLVED or REJECTED, got: " + newStatus);
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE reports SET status = ? WHERE id = ? AND status = 'OPEN'")) {
            ps.setString(1, newStatus);
            ps.setLong(2, reportId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("resolve report failed: " + e.getMessage(), e);
        }
    }

    /**
     * Look up the reported plugin id without pulling the full row. Used by the
     * resolve flow before flipping {@code plugins.status}.
     */
    public Optional<Long> findPluginId(long reportId) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT plugin_id FROM reports WHERE id = ?")) {
            ps.setLong(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findPluginId failed: " + e.getMessage(), e);
        }
    }
}
