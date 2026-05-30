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

import local.promptmark.dto.Order;
import local.promptmark.dto.OrderItem;
import local.promptmark.dto.OrderStatus;

/**
 * SQL access for {@code orders} + {@code order_items}. Insert APIs take an
 * external {@link Connection} so OrderService can wrap order + items in one
 * transaction; read APIs use the pool directly.
 */
public class OrderDao {

    private final DataSource ds;

    public OrderDao(DataSource ds) {
        this.ds = ds;
    }

    public long insertOrder(long userId, int totalAmount, OrderStatus status, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO orders (user_id, total_amount, status) " +
                "VALUES (?, ?, ?) RETURNING id")) {
            ps.setLong(1, userId);
            ps.setInt(2, totalAmount);
            ps.setString(3, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("INSERT RETURNING produced no row");
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("insertOrder failed: " + e.getMessage(), e);
        }
    }

    public void insertOrderItem(long orderId, long assetId, int pricePaid, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO order_items (order_id, asset_id, price_paid) VALUES (?, ?, ?)")) {
            ps.setLong(1, orderId);
            ps.setLong(2, assetId);
            ps.setInt(3, pricePaid);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertOrderItem failed: " + e.getMessage(), e);
        }
    }

    public Optional<Order> findById(long id) {
        String sql = "SELECT id, user_id, total_amount, status, created_at FROM orders WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Timestamp ts = rs.getTimestamp("created_at");
                return Optional.of(new Order(
                    rs.getLong("id"),
                    rs.getLong("user_id"),
                    rs.getInt("total_amount"),
                    OrderStatus.fromDb(rs.getString("status")),
                    ts == null ? null : ts.toInstant()));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + e.getMessage(), e);
        }
    }

    /** Order-line view joining assets for the title column. */
    public List<OrderItem> findItems(long orderId) {
        String sql = "SELECT oi.id, oi.order_id, oi.asset_id, oi.price_paid, a.title " +
                     "FROM order_items oi " +
                     "JOIN assets a ON a.id = oi.asset_id " +
                     "WHERE oi.order_id = ? " +
                     "ORDER BY oi.id";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new OrderItem(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getLong("asset_id"),
                        rs.getInt("price_paid"),
                        rs.getString("title")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findItems failed: " + e.getMessage(), e);
        }
    }

    public List<Order> findByUserId(long userId, int limit) {
        String sql = "SELECT id, user_id, total_amount, status, created_at FROM orders " +
                     "WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Order> out = new ArrayList<>();
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    out.add(new Order(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getInt("total_amount"),
                        OrderStatus.fromDb(rs.getString("status")),
                        ts == null ? null : ts.toInstant()));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUserId failed: " + e.getMessage(), e);
        }
    }

    /** True if the user has ever bought this asset in a PAID order. */
    public boolean userHasPurchased(long userId, long assetId) {
        String sql = "SELECT 1 FROM orders o " +
                     "JOIN order_items oi ON oi.order_id = o.id " +
                     "WHERE o.user_id = ? AND oi.asset_id = ? AND o.status = 'PAID' " +
                     "LIMIT 1";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, assetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("userHasPurchased failed: " + e.getMessage(), e);
        }
    }
}
