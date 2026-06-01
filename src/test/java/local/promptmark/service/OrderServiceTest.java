package local.promptmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.OrderDao;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.PluginType;
import local.promptmark.dto.CartItem;
import local.promptmark.dto.LoginUser;
import local.promptmark.dto.OrderStatus;
import local.promptmark.web.ConflictException;
import local.promptmark.web.Role;

class OrderServiceTest {

    private PluginDao pluginDao;
    private OrderDao orderDao;
    private DataSource ds;
    private Connection conn;
    private OrderService svc;

    @BeforeEach
    void setUp() throws Exception {
        pluginDao = Mockito.mock(PluginDao.class);
        orderDao = Mockito.mock(OrderDao.class);
        ds = Mockito.mock(DataSource.class);
        conn = Mockito.mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.getAutoCommit()).thenReturn(true);
        svc = new OrderService(ds, pluginDao, orderDao);
    }

    private LoginUser buyer(long id) {
        return new LoginUser(id, "u@b.com", "buyer", Role.USER);
    }

    private Plugin pluginOf(long id, long sellerId, PluginStatus status, int price) {
        return new Plugin(id, sellerId, PluginType.PROMPT, "Plugin " + id,
            "summary", "body", null, null, null, price, status, 0, 0,
            Instant.now(), Instant.now());
    }

    @Test
    void place_order_happy_path_inserts_order_and_items() {
        Plugin a1 = pluginOf(10L, 99L, PluginStatus.PUBLIC, 1500);
        Plugin a2 = pluginOf(11L, 99L, PluginStatus.PUBLIC, 2500);
        when(pluginDao.findById(10L)).thenReturn(Optional.of(a1));
        when(pluginDao.findById(11L)).thenReturn(Optional.of(a2));
        when(orderDao.userHasPurchased(anyLong(), anyLong())).thenReturn(false);
        when(orderDao.insertOrder(eq(7L), eq(4000), eq(OrderStatus.PAID), any(Connection.class)))
            .thenReturn(555L);

        long id = svc.placeOrder(buyer(7L), Arrays.asList(
            new CartItem(10L, "Plugin 10", 1500, PluginType.PROMPT),
            new CartItem(11L, "Plugin 11", 2500, PluginType.PROMPT)));

        assertThat(id).isEqualTo(555L);
        verify(orderDao).insertOrderItem(eq(555L), eq(10L), eq(1500), any(Connection.class));
        verify(orderDao).insertOrderItem(eq(555L), eq(11L), eq(2500), any(Connection.class));
    }

    @Test
    void place_order_uses_live_price_not_session_snapshot() {
        Plugin live = pluginOf(10L, 99L, PluginStatus.PUBLIC, 5000); // price changed
        when(pluginDao.findById(10L)).thenReturn(Optional.of(live));
        when(orderDao.userHasPurchased(anyLong(), anyLong())).thenReturn(false);
        when(orderDao.insertOrder(anyLong(), anyInt(), any(OrderStatus.class), any(Connection.class)))
            .thenReturn(1L);

        svc.placeOrder(buyer(7L), Collections.singletonList(
            new CartItem(10L, "Plugin 10", 999, PluginType.PROMPT))); // stale snapshot

        verify(orderDao).insertOrder(eq(7L), eq(5000), eq(OrderStatus.PAID), any(Connection.class));
        verify(orderDao).insertOrderItem(anyLong(), eq(10L), eq(5000), any(Connection.class));
    }

    @Test
    void place_order_throws_when_buyer_is_seller() {
        Plugin own = pluginOf(10L, 7L, PluginStatus.PUBLIC, 1500);
        when(pluginDao.findById(10L)).thenReturn(Optional.of(own));
        when(orderDao.userHasPurchased(anyLong(), anyLong())).thenReturn(false);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() ->
            svc.placeOrder(buyer(7L), Collections.singletonList(
                new CartItem(10L, "Plugin 10", 1500, PluginType.PROMPT))));
        verify(orderDao, never()).insertOrder(anyLong(), anyInt(), any(), any());
    }

    @Test
    void place_order_throws_when_already_purchased() {
        Plugin a = pluginOf(10L, 99L, PluginStatus.PUBLIC, 1500);
        when(pluginDao.findById(10L)).thenReturn(Optional.of(a));
        when(orderDao.userHasPurchased(7L, 10L)).thenReturn(true);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() ->
            svc.placeOrder(buyer(7L), Collections.singletonList(
                new CartItem(10L, "Plugin 10", 1500, PluginType.PROMPT))));
        verify(orderDao, never()).insertOrder(anyLong(), anyInt(), any(), any());
    }

    @Test
    void place_order_throws_when_plugin_hidden() {
        Plugin hidden = pluginOf(10L, 99L, PluginStatus.HIDDEN, 1500);
        when(pluginDao.findById(10L)).thenReturn(Optional.of(hidden));
        when(orderDao.userHasPurchased(anyLong(), anyLong())).thenReturn(false);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() ->
            svc.placeOrder(buyer(7L), Collections.singletonList(
                new CartItem(10L, "Plugin 10", 1500, PluginType.PROMPT))));
    }

    @Test
    void place_order_throws_when_plugin_missing() {
        when(pluginDao.findById(10L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() ->
            svc.placeOrder(buyer(7L), Collections.singletonList(
                new CartItem(10L, "Plugin 10", 1500, PluginType.PROMPT))));
    }

    @Test
    void place_order_throws_when_cart_empty() {
        assertThatExceptionOfType(ConflictException.class).isThrownBy(() ->
            svc.placeOrder(buyer(7L), Collections.emptyList()));
    }
}
