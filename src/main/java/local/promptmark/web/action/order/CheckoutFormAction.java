package local.promptmark.web.action.order;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.CartItem;
import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;
import local.promptmark.web.action.cart.CartSupport;

/** GET {@code /app/order/checkout} — review cart before confirming. */
public class CheckoutFormAction implements Action {

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        List<CartItem> cart = CartSupport.get(req);
        req.setAttribute("cart", cart);
        req.setAttribute("total", CartSupport.totalAmount(cart));
        return ViewResult.forward("order/checkout");
    }
}
