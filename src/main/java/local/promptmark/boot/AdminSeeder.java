package local.promptmark.boot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private AdminSeeder() {}

    public static void seed(DataSource ds, String email, String rawPassword) {
        if (email == null || email.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            log.warn("Skipping admin seed — ADMIN_EMAIL or ADMIN_PWD missing");
            return;
        }
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        String sql = "INSERT INTO users (email, password_hash, nickname, role) " +
                     "VALUES (?, ?, ?, 'ADMIN') " +
                     "ON CONFLICT (email) DO NOTHING";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hash);
            ps.setString(3, "admin");
            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("ADMIN seeded: {}", email);
            } else {
                log.info("ADMIN already exists, skipped: {}", email);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Admin seed failed: " + e.getMessage(), e);
        }
    }
}
