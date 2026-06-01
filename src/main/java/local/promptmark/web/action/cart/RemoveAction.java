package local.promptmark.web.action.cart;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.CartItem;
import local.promptmark.web.Action;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/** POST {@code /app/cart/remove?pluginId=N} — drop one entry, redirect to view. */
public class RemoveAction implements Action {

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        long pluginId = parseLong(req.getParameter("pluginId"));
        List<CartItem> cart = CartSupport.get(req);
        Iterator<CartItem> it = cart.iterator();
        while (it.hasNext()) {
            if (it.next().getPluginId() == pluginId) {
                it.remove();
                break;
            }
        }
        return ViewResult.redirect(req.getContextPath() + "/app/cart/view");
    }

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("자산을 찾을 수 없습니다");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("자산을 찾을 수 없습니다"); }
    }
}
