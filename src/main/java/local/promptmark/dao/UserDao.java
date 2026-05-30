package local.promptmark.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

import javax.sql.DataSource;

import local.promptmark.dto.User;
import local.promptmark.web.Role;

/**
 * SQL-only access to the {@code users} table. Connection pool has
 * {@code search_path TO promptmark, public, extensions} pinned so unqualified
 * names resolve in our schema.
 */
public class UserDao {

    private final DataSource ds;

    public UserDao(DataSource ds) {
        this.ds = ds;
    }

    public Optional<User> findById(long id) {
        String sql = "SELECT id, email, password_hash, nickname, role, status, created_at " +
                     "FROM users WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + e.getMessage(), e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, nickname, role, status, created_at " +
                     "FROM users WHERE email = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByEmail failed: " + e.getMessage(), e);
        }
    }

    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    public boolean existsByNickname(String nickname) {
        return exists("SELECT 1 FROM users WHERE nickname = ?", nickname);
    }

    public User insert(String email, String passwordHash, String nickname, Role role) {
        String sql = "INSERT INTO users (email, password_hash, nickname, role) " +
                     "VALUES (?, ?, ?, ?) " +
                     "RETURNING id, email, password_hash, nickname, role, status, created_at";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, nickname);
            ps.setString(4, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("INSERT RETURNING produced no row");
                }
                return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("insert user failed: " + e.getMessage(), e);
        }
    }

    private boolean exists(String sql, String param) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("exists query failed: " + e.getMessage(), e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new User(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("nickname"),
            Role.fromDb(rs.getString("role")),
            User.statusFromDb(rs.getString("status")),
            ts == null ? null : ts.toInstant()
        );
    }
}
