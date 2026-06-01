package local.promptmark.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.OrderDao;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.CartItem;
import local.promptmark.dto.LoginUser;
import local.promptmark.dto.Order;
import local.promptmark.dto.OrderItem;
import local.promptmark.dto.OrderStatus;
import local.promptmark.web.ConflictException;
import local.promptmark.web.ForbiddenException;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.Role;

/**
 * Mock-payment checkout flow. {@link #placeOrder(LoginUser, List)} re-fetches
 * every cart plugin from the DB so we never trust the session snapshot for the
 * price or status, then writes the order + items in one transaction.
 */
public class OrderService {

    private final DataSource ds;
    private final PluginDao pluginDao;
    private final OrderDao orderDao;

    public OrderService(DataSource ds, PluginDao pluginDao, OrderDao orderDao) {
        this.ds = ds;
        this.pluginDao = pluginDao;
        this.orderDao = orderDao;
    }

    /**
     * Validate every cart item, sum the live price, then write the order. On
     * any per-item failure (hidden/deleted plugin, buyer is the seller, plugin
     * already purchased) throws {@link ConflictException} with a message that
     * names the offending title so the UI can surface it.
     */
    public long placeOrder(LoginUser buyer, List<CartItem> cart) {
        if (buyer == null) throw new ForbiddenException("로그인이 필요합니다");
        if (cart == null || cart.isEmpty()) {
            throw new ConflictException("장바구니가 비어 있습니다");
        }

        int total = 0;
        // Validate up front before opening the tx so we don't roll back work.
        for (CartItem item : cart) {
            Plugin live = pluginDao.findById(item.getPluginId())
                .orElseThrow(() -> new ConflictException(
                    "자산을 찾을 수 없습니다: " + item.getTitle()));
            if (live.getStatus() != PluginStatus.PUBLIC) {
                throw new ConflictException("판매중인 자산이 아닙니다: " + live.getTitle());
            }
            if (live.getSellerId() == buyer.getId()) {
                throw new ConflictException("자기 자산은 구매할 수 없습니다: " + live.getTitle());
            }
            if (orderDao.userHasPurchased(buyer.getId(), live.getId())) {
                throw new ConflictException("이미 구매한 자산입니다: " + live.getTitle());
            }
            total = Math.addExact(total, live.getPrice());
        }

        try (Connection c = ds.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                long orderId = orderDao.insertOrder(buyer.getId(), total, OrderStatus.PAID, c);
                for (CartItem item : cart) {
                    Plugin live = pluginDao.findById(item.getPluginId())
                        .orElseThrow(() -> new ConflictException(
                            "자산을 찾을 수 없습니다: " + item.getTitle()));
                    orderDao.insertOrderItem(orderId, live.getId(), live.getPrice(), c);
                }
                c.commit();
                return orderId;
            } catch (RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new RuntimeException("placeOrder failed: " + e.getMessage(), e);
        }
    }

    public Order findOrder(long orderId, LoginUser buyer) {
        if (buyer == null) throw new ForbiddenException("로그인이 필요합니다");
        Optional<Order> opt = orderDao.findById(orderId);
        if (!opt.isPresent()) {
            throw new NotFoundException("주문을 찾을 수 없습니다");
        }
        Order o = opt.get();
        if (o.getUserId() != buyer.getId() && buyer.getRole() != Role.ADMIN) {
            throw new NotFoundException("주문을 찾을 수 없습니다");
        }
        return o;
    }

    public List<OrderItem> findOrderItems(long orderId, LoginUser buyer) {
        // Reuses findOrder for the auth check.
        findOrder(orderId, buyer);
        return orderDao.findItems(orderId);
    }

    public List<Order> history(LoginUser buyer) {
        if (buyer == null) throw new ForbiddenException("로그인이 필요합니다");
        return orderDao.findByUserId(buyer.getId(), 50);
    }
}
