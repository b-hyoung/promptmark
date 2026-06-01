package local.promptmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.ReportDao;
import local.promptmark.dao.UserDao;
import local.promptmark.dto.User;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.Role;
import local.promptmark.web.ValidationException;

class AdminServiceTest {

    private ReportDao reportDao;
    private PluginDao pluginDao;
    private UserDao userDao;
    private DataSource ds;
    private Connection conn;
    private PreparedStatement ps;
    private AdminService svc;

    @BeforeEach
    void setUp() throws Exception {
        reportDao = Mockito.mock(ReportDao.class);
        pluginDao = Mockito.mock(PluginDao.class);
        userDao = Mockito.mock(UserDao.class);
        ds = Mockito.mock(DataSource.class);
        conn = Mockito.mock(Connection.class);
        ps = Mockito.mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.getAutoCommit()).thenReturn(true);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        svc = new AdminService(ds, reportDao, pluginDao, userDao);
    }

    private User user(long id, User.Status status) {
        return new User(id, "u" + id + "@b.com", "hash", "user" + id,
            Role.USER, status, Instant.now());
    }

    // ───── listOpenReports ─────────────────────────────────────────────

    @Test
    void listOpenReports_passes_through() {
        when(reportDao.findOpen(50)).thenReturn(java.util.Collections.emptyList());
        assertThat(svc.listOpenReports(50)).isEmpty();
        verify(reportDao).findOpen(50);
    }

    // ───── resolveReport — validation ──────────────────────────────────

    @Test
    void resolveReport_rejects_null_action() {
        assertThatExceptionOfType(ValidationException.class).isThrownBy(
            () -> svc.resolveReport(1L, null));
    }

    @Test
    void resolveReport_rejects_unknown_action() {
        assertThatExceptionOfType(ValidationException.class).isThrownBy(
            () -> svc.resolveReport(1L, "BOGUS"));
    }

    @Test
    void resolveReport_404s_when_report_missing() {
        when(reportDao.findPluginId(99L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(
            () -> svc.resolveReport(99L, "HIDE"));
        verify(reportDao, never()).resolve(eq(99L), anyString(), any());
    }

    // ───── resolveReport — happy paths ─────────────────────────────────

    @Test
    void resolveReport_HIDE_flips_plugin_to_HIDDEN_and_resolves() throws SQLException {
        when(reportDao.findPluginId(1L)).thenReturn(Optional.of(42L));

        svc.resolveReport(1L, "HIDE");

        // The inline UPDATE plugins SQL is the first prepareStatement call.
        verify(conn, atLeastOnce()).prepareStatement(
            "UPDATE plugins SET status = ?, updated_at = now() WHERE id = ?");
        verify(ps).setString(1, "HIDDEN");
        verify(ps).setLong(2, 42L);
        verify(reportDao).resolve(eq(1L), eq("RESOLVED"), any(Connection.class));
        verify(conn).commit();
    }

    @Test
    void resolveReport_DELETE_flips_plugin_to_DELETED_and_resolves() throws SQLException {
        when(reportDao.findPluginId(1L)).thenReturn(Optional.of(42L));

        svc.resolveReport(1L, "DELETE");

        verify(ps).setString(1, "DELETED");
        verify(ps).setLong(2, 42L);
        verify(reportDao).resolve(eq(1L), eq("RESOLVED"), any(Connection.class));
        verify(conn).commit();
    }

    @Test
    void resolveReport_REJECT_only_closes_report() throws SQLException {
        when(reportDao.findPluginId(1L)).thenReturn(Optional.of(42L));

        svc.resolveReport(1L, "REJECT");

        // REJECT does not touch plugins; the inline plugin SQL must not be prepared.
        verify(conn, never()).prepareStatement(
            "UPDATE plugins SET status = ?, updated_at = now() WHERE id = ?");
        verify(reportDao).resolve(eq(1L), eq("REJECTED"), any(Connection.class));
        verify(conn).commit();
    }

    @Test
    void resolveReport_rolls_back_on_failure() throws SQLException {
        when(reportDao.findPluginId(1L)).thenReturn(Optional.of(42L));
        Mockito.doThrow(new RuntimeException("boom"))
            .when(reportDao).resolve(eq(1L), anyString(), any(Connection.class));

        try {
            svc.resolveReport(1L, "HIDE");
        } catch (RuntimeException expected) {
            // expected
        }
        verify(conn).rollback();
        verify(conn, never()).commit();
    }

    // ───── banUser ─────────────────────────────────────────────────────

    @Test
    void banUser_updates_status_when_user_exists() {
        when(userDao.findById(7L)).thenReturn(Optional.of(user(7L, User.Status.ACTIVE)));
        svc.banUser(7L);
        verify(userDao).updateStatus(7L, "BANNED");
    }

    @Test
    void banUser_404s_when_user_missing() {
        when(userDao.findById(7L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NotFoundException.class).isThrownBy(
            () -> svc.banUser(7L));
        verify(userDao, never()).updateStatus(eq(7L), anyString());
    }
}
