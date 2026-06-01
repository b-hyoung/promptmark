package local.promptmark.web.action.cart;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.OrderDao;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.CartItem;
import local.promptmark.dto.LoginUser;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.ConflictException;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/**
 * POST {@code /app/cart/add?pluginId=N}. Re-validates against the live row so
 * a HIDDEN/DELETED/own/already-bought plugin can't slip into the cart.
 */
public class AddAction implements Action {

    private final PluginDao pluginDao;
    private final OrderDao orderDao;

    public AddAction(PluginDao pluginDao, OrderDao orderDao) {
        this.pluginDao = pluginDao;
        this.orderDao = orderDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        long pluginId = parseLong(req.getParameter("pluginId"));

        Plugin a = pluginDao.findById(pluginId)
            .orElseThrow(() -> new NotFoundException("자산을 찾을 수 없습니다"));

        if (a.getStatus() != PluginStatus.PUBLIC) {
            throw new ConflictException("판매중인 자산이 아닙니다");
        }
        if (a.getSellerId() == me.getId()) {
            throw new ConflictException("자기 자산은 구매할 수 없습니다");
        }
        if (orderDao.userHasPurchased(me.getId(), a.getId())) {
            throw new ConflictException("이미 구매한 자산입니다");
        }

        List<CartItem> cart = CartSupport.get(req);
        for (CartItem existing : cart) {
            if (existing.getPluginId() == a.getId()) {
                throw new ConflictException("이미 장바구니에 있는 자산입니다");
            }
        }
        cart.add(new CartItem(a.getId(), a.getTitle(), a.getPrice(), a.getType()));

        return ViewResult.redirect(req.getContextPath() + "/app/cart/view");
    }

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("자산을 찾을 수 없습니다");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("자산을 찾을 수 없습니다"); }
    }
}
