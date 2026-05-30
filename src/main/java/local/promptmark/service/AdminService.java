package local.promptmark.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import local.promptmark.dao.AssetDao;
import local.promptmark.dao.ReportDao;
import local.promptmark.dao.UserDao;
import local.promptmark.dto.AssetStatus;
import local.promptmark.dto.ReportRow;
import local.promptmark.dto.User;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ValidationException;

/**
 * Admin operations: report queue, resolve/reject, and user ban. Encapsulates
 * the small bit of transactional glue between {@link ReportDao} and {@link
 * AssetDao} (resolving a report often flips the asset's status, and both
 * updates must succeed or fail together).
 */
public class AdminService {

    /** Allowed resolve actions. */
    public static final String ACTION_HIDE = "HIDE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_REJECT = "REJECT";

    private final DataSource ds;
    private final ReportDao reportDao;
    @SuppressWarnings("unused") // assetDao is used through inline SQL so we
    // keep a reference around for future direct calls without breaking the API.
    private final AssetDao assetDao;
    private final UserDao userDao;

    public AdminService(DataSource ds,
                        ReportDao reportDao,
                        AssetDao assetDao,
                        UserDao userDao) {
        this.ds = ds;
        this.reportDao = reportDao;
        this.assetDao = assetDao;
        this.userDao = userDao;
    }

    /** Pass-through to {@link ReportDao#findOpen(int)}. */
    public List<ReportRow> listOpenReports(int limit) {
        return reportDao.findOpen(limit);
    }

    /**
     * Apply an admin action to a report. {@code HIDE} sets the asset to
     * HIDDEN, {@code DELETE} soft-deletes the asset, {@code REJECT} just
     * closes the report. All paths run in a single transaction.
     *
     * @throws ValidationException when {@code action} is not one of the three values
     * @throws NotFoundException   when the report does not exist
     */
    public void resolveReport(long reportId, String action) {
        if (action == null) {
            throw new ValidationException("action 파라미터가 필요합니다",
                java.util.Collections.singletonMap("action", "required"));
        }
        String act = action.trim().toUpperCase();
        if (!ACTION_HIDE.equals(act)
                && !ACTION_DELETE.equals(act)
                && !ACTION_REJECT.equals(act)) {
            throw new ValidationException("알 수 없는 처리 동작입니다: " + action,
                java.util.Collections.singletonMap("action", "invalid"));
        }

        Optional<Long> assetIdOpt = reportDao.findAssetId(reportId);
        if (!assetIdOpt.isPresent()) {
            throw new NotFoundException("신고를 찾을 수 없습니다");
        }
        long assetId = assetIdOpt.get();

        try (Connection c = ds.getConnection()) {
            boolean prevAuto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                switch (act) {
                    case ACTION_HIDE:
                        updateAssetStatus(c, assetId, AssetStatus.HIDDEN.name());
                        reportDao.resolve(reportId, "RESOLVED", c);
                        break;
                    case ACTION_DELETE:
                        updateAssetStatus(c, assetId, AssetStatus.DELETED.name());
                        reportDao.resolve(reportId, "RESOLVED", c);
                        break;
                    case ACTION_REJECT:
                        reportDao.resolve(reportId, "REJECTED", c);
                        break;
                    default:
                        // Already validated above; defensive.
                        throw new ValidationException("알 수 없는 처리 동작입니다",
                            java.util.Collections.singletonMap("action", "invalid"));
                }
                c.commit();
            } catch (RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prevAuto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("resolveReport failed: " + e.getMessage(), e);
        }
    }

    /**
     * Mark the user BANNED. AuthFilter will block their next request — already-
     * active sessions stop working as soon as the marker flips.
     *
     * @throws NotFoundException when the user does not exist
     */
    public void banUser(long userId) {
        User u = userDao.findById(userId)
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));
        userDao.updateStatus(u.getId(), "BANNED");
    }

    /**
     * Inline transactional asset.status update — the existing AssetDao
     * mutators all open their own connection, which would break the report
     * transaction. We bundle the update with the resolve so a failing one
     * rolls the other back.
     */
    private static void updateAssetStatus(Connection c, long assetId, String newStatus)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE assets SET status = ?, updated_at = now() WHERE id = ?")) {
            ps.setString(1, newStatus);
            ps.setLong(2, assetId);
            ps.executeUpdate();
        }
    }
}
