package local.promptmark.web.action.asset;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.Asset;
import local.promptmark.dto.AssetType;
import local.promptmark.service.AssetService;
import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/**
 * GET {@code /app/asset/list} — paginated, filtered catalogue.
 * Query params: {@code q}, {@code type}, {@code tag}, {@code sort}, {@code page}.
 */
public class ListAction implements Action {

    private static final int PAGE_SIZE = 12;

    private final AssetService assetService;

    public ListAction(AssetService assetService) {
        this.assetService = assetService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        String q = trimToNull(req.getParameter("q"));
        AssetType type = parseType(req.getParameter("type"));
        String tag = trimToNull(req.getParameter("tag"));
        String sort = req.getParameter("sort");
        if (sort == null || sort.isEmpty()) sort = "recent";

        int page = parsePage(req.getParameter("page"));
        int offset = (page - 1) * PAGE_SIZE;

        List<Asset> assets = assetService.search(q, type, tag, sort, offset, PAGE_SIZE);
        int total = assetService.countSearch(q, type, tag);
        int totalPages = total == 0 ? 1 : (total + PAGE_SIZE - 1) / PAGE_SIZE;

        req.setAttribute("assets", assets);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", PAGE_SIZE);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("q", q == null ? "" : q);
        req.setAttribute("type", type == null ? "" : type.name());
        req.setAttribute("tag", tag == null ? "" : tag);
        req.setAttribute("sort", sort);

        return ViewResult.forward("asset/list");
    }

    private static AssetType parseType(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase();
        if (t.isEmpty()) return null;
        try { return AssetType.valueOf(t); } catch (IllegalArgumentException e) { return null; }
    }

    private static int parsePage(String s) {
        if (s == null) return 1;
        try {
            int p = Integer.parseInt(s.trim());
            return p < 1 ? 1 : p;
        } catch (NumberFormatException nfe) {
            return 1;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
