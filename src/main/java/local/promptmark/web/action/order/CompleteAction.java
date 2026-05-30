package local.promptmark.web.action.order;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.LoginUser;
import local.promptmark.dto.Order;
import local.promptmark.service.OrderService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/** GET {@code /app/order/complete?orderId=N} — receipt page after checkout. */
public class CompleteAction implements Action {

    private final OrderService orderService;

    public CompleteAction(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        long orderId = parseLong(req.getParameter("orderId"));
        Order order = orderService.findOrder(orderId, me);
        req.setAttribute("order", order);
        req.setAttribute("items", orderService.findOrderItems(orderId, me));
        return ViewResult.forward("order/complete");
    }

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("주문을 찾을 수 없습니다");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("주문을 찾을 수 없습니다"); }
    }
}
