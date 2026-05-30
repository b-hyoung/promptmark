package local.promptmark.dao;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.sql.Connection;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

/**
 * Pure-logic checks. The DB-touching paths are exercised by Phase 4's
 * Testcontainers ITs; here we only verify the {@link ReportDao#resolve}
 * input-validation gate.
 */
class ReportDaoTest {

    private final ReportDao dao = new ReportDao(mock(DataSource.class));

    @Test
    void resolve_rejects_null_status() {
        Connection c = mock(Connection.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dao.resolve(1L, null, c));
    }

    @Test
    void resolve_rejects_unknown_status() {
        Connection c = mock(Connection.class);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> dao.resolve(1L, "FROBNICATE", c));
    }

    @Test
    void resolve_accepts_RESOLVED_and_REJECTED_at_validation_layer() throws Exception {
        // Stub a connection whose prepareStatement returns a no-op PreparedStatement.
        java.sql.PreparedStatement ps = mock(java.sql.PreparedStatement.class);
        Connection c = mock(Connection.class);
        org.mockito.Mockito.when(c.prepareStatement(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(ps);

        // No throw means we passed validation and reached the executeUpdate path.
        dao.resolve(1L, "RESOLVED", c);
        dao.resolve(1L, "REJECTED", c);
    }
}
