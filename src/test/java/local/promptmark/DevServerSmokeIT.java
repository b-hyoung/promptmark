package local.promptmark;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import local.promptmark.boot.AdminSeeder;
import local.promptmark.boot.SchemaApplier;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
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
class DevServerSmokeIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("promptmark_test")
        .withUsername("test")
        .withPassword("test");

    private Tomcat tomcat;
    private HikariDataSource ds;
    private int port;
    private String contextPath = "/promptmark";

    @BeforeAll
    void setUp() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);
        SchemaApplier.applyFromClasspath(ds, "db/migration/V1__init.sql");
        AdminSeeder.seed(ds, "admin@local", "pwd123!");

        // Random free port (avoid clash with dev 8080)
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) { port = s.getLocalPort(); }

        tomcat = new Tomcat();
        java.io.File workDir = new java.io.File("build/tomcat-test");
        workDir.mkdirs();
        tomcat.setBaseDir(workDir.getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();
        Context ctx = tomcat.addWebapp(contextPath,
            new java.io.File("src/main/webapp").getAbsoluteFile().getAbsolutePath());
        ctx.setReloadable(false);
        tomcat.start();
    }

    @AfterAll
    void tearDown() throws Exception {
        if (tomcat != null) { tomcat.stop(); tomcat.destroy(); }
        if (ds != null) ds.close();
    }

    @Test
    void homePageReturns200WithTraceIdHeader() throws Exception {
        URL url = new URL("http://localhost:" + port + contextPath + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getHeaderField("X-Trace-Id")).isNotEmpty();

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) body.append(line);
            assertThat(body.toString()).contains("It works");
        }
    }

    @Test
    void adminRowExists() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(
                "SELECT count(*) FROM users WHERE email='admin@local' AND role='ADMIN'");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
