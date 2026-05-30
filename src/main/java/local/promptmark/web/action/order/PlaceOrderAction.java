package local.promptmark.web.action.order;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.CartItem;
import local.promptmark.dto.LoginUser;
import local.promptmark.service.OrderService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.ConflictException;
import local.promptmark.web.ViewResult;
import local.promptmark.web.action.cart.CartSupport;

/**
 * POST {@code /app/order/checkout} — call OrderService, clear cart on success,
 * forward to checkout page with error message on conflict.
 */
public class PlaceOrderAction implements Action {

    private final OrderService orderService;

    public PlaceOrderAction(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        List<CartItem> cart = CartSupport.get(req);

        try {
            long orderId = orderService.placeOrder(me, cart);
            CartSupport.clear(req);
            return ViewResult.redirect(req.getContextPath() +
                "/app/order/complete?orderId=" + orderId);
        } catch (ConflictException ce) {
            req.setAttribute("cart", cart);
            req.setAttribute("total", CartSupport.totalAmount(cart));
            req.setAttribute("errorMessage", ce.userMessage());
            return ViewResult.forward("order/checkout");
        }
    }
}
