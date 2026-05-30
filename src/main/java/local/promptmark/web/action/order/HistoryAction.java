package local.promptmark.web.action.order;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.LoginUser;
import local.promptmark.service.OrderService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.ViewResult;

/** GET {@code /app/order/history} — past orders for the current buyer. */
public class HistoryAction implements Action {

    private final OrderService orderService;

    public HistoryAction(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        req.setAttribute("orders", orderService.history(me));
        return ViewResult.forward("order/history");
    }
}
