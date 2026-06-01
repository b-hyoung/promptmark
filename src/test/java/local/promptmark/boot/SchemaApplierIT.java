package local.promptmark.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaApplierIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("promptmark_test")
        .withUsername("test")
        .withPassword("test");

    private HikariDataSource ds;

    @BeforeAll
    void setUp() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);
    }

    @AfterAll
    void tearDown() {
        if (ds != null) ds.close();
    }

    @Test
    void createsAllTables() throws Exception {
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT count(*) FROM information_schema.tables " +
                "WHERE table_schema='public' AND table_name IN " +
                "('users','plugins','tags','plugin_tags','orders','order_items','downloads','reports')");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(8);
        }
    }

    @Test
    void isIdempotent() throws Exception {
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");  // second run, must not throw
    }

    @Test
    void vectorColumnExists() throws Exception {
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_name='plugins' AND column_name='embedding'");
            rs.next();
            assertThat(rs.getString(1)).isEqualToIgnoringCase("USER-DEFINED");  // vector is a custom type
        }
    }
}
