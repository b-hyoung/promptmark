package local.promptmark.web.action.mypage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dao.AssetDao;
import local.promptmark.dao.DownloadDao;
import local.promptmark.dao.OrderDao;
import local.promptmark.dto.Asset;
import local.promptmark.dto.LoginUser;
import local.promptmark.dto.Order;
import local.promptmark.dto.OrderItem;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.ViewResult;

/**
 * GET {@code /app/mypage} — dashboard for the logged-in user.
 *
 * <p>Loads three lists in parallel via DAO calls: assets I sell, orders I have
 * placed, and recently downloaded files. AuthMap pins this action to USER so
 * the session lookup is always non-null when we get here.
 */
public class IndexAction implements Action {

    private static final int ASSETS_LIMIT = 50;
    private static final int ORDERS_LIMIT = 50;
    private static final int DOWNLOADS_LIMIT = 20;

    private final AssetDao assetDao;
    private final OrderDao orderDao;
    private final DownloadDao downloadDao;

    public IndexAction(AssetDao assetDao, OrderDao orderDao, DownloadDao downloadDao) {
        this.assetDao = assetDao;
        this.orderDao = orderDao;
        this.downloadDao = downloadDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);

        List<Asset> myAssets = assetDao.findBySellerId(me.getId(), ASSETS_LIMIT);
        List<Order> myOrders = orderDao.findByUserId(me.getId(), ORDERS_LIMIT);
        List<Asset> recentDownloads = downloadDao.findRecentByUser(me.getId(), DOWNLOADS_LIMIT);

        // Pre-fetch order items for each order so the JSP can show titles
        // without spawning N+1 fetch logic itself. Keyed by orderId.
        Map<Long, List<OrderItem>> orderItems = new LinkedHashMap<>();
        for (Order o : myOrders) {
            List<OrderItem> items = orderDao.findItems(o.getId());
            orderItems.put(o.getId(), items == null ? new ArrayList<>() : items);
        }

        req.setAttribute("loginUser", me);
        req.setAttribute("myAssets", myAssets);
        req.setAttribute("myOrders", myOrders);
        req.setAttribute("orderItems", orderItems);
        req.setAttribute("recentDownloads", recentDownloads);
        return ViewResult.forward("mypage/index");
    }
}
