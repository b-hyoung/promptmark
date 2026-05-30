package local.promptmark.web.action.cart;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import local.promptmark.dto.CartItem;

/**
 * Helper for the session-scoped cart. The cart is a List&lt;CartItem&gt; stored
 * on the session under {@code CART}. All access goes through here so we never
 * surprise an action with a raw cast or stale type.
 */
final class CartSupport {

    static final String SESSION_ATTR = "CART";

    private CartSupport() {}

    /** Always returns a non-null mutable list bound to the session. */
    static List<CartItem> get(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        @SuppressWarnings("unchecked")
        List<CartItem> cart = (List<CartItem>) session.getAttribute(SESSION_ATTR);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(SESSION_ATTR, cart);
        }
        return cart;
    }

    static int totalAmount(List<CartItem> items) {
        int total = 0;
        for (CartItem it : items) total += it.getPrice();
        return total;
    }
}
