package local.promptmark.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mindrot.jbcrypt.BCrypt;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminSeederIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("promptmark_test")
        .withUsername("test")
        .withPassword("test");

    private HikariDataSource ds;

    @BeforeAll
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
    }

    @AfterAll
    void tearDown() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        }
        if (ds != null) ds.close();
    }

    @Test
    void insertsAdminWhenAbsent() throws Exception {
        AdminSeeder.seed(ds, "boss@local", "pwd123!");

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT email, role, password_hash FROM users WHERE email='boss@local'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("email")).isEqualTo("boss@local");
            assertThat(rs.getString("role")).isEqualTo("ADMIN");
            assertThat(BCrypt.checkpw("pwd123!", rs.getString("password_hash"))).isTrue();
        }
    }

    @Test
    void isIdempotent() throws Exception {
        AdminSeeder.seed(ds, "boss2@local", "pwd123!");
        AdminSeeder.seed(ds, "boss2@local", "different_password");  // second call: must not throw

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT count(*) FROM users WHERE email='boss2@local'");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);   // no duplicates
        }
    }
}
