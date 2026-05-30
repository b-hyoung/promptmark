package local.promptmark.web.action.cart;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.CartItem;
import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/** GET {@code /app/cart/view} — render the cart contents + sum. */
public class ViewAction implements Action {

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        List<CartItem> cart = CartSupport.get(req);
        req.setAttribute("cart", cart);
        req.setAttribute("total", CartSupport.totalAmount(cart));
        return ViewResult.forward("cart/view");
    }
}
