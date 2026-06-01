package local.promptmark.web.action.bundle;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.Bundle;
import local.promptmark.service.BundleService;
import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/** GET /app/bundle/list — catalogue. */
public class ListAction implements Action {
    private static final int PAGE_SIZE = 12;
    private final BundleService bundleService;

    public ListAction(BundleService bundleService) { this.bundleService = bundleService; }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        String sort = req.getParameter("sort");
        if (sort == null || sort.isEmpty()) sort = "recent";
        int page = parsePage(req.getParameter("page"));
        int offset = (page - 1) * PAGE_SIZE;

        List<Bundle> bundles = bundleService.listPublic(sort, offset, PAGE_SIZE);
        int total = bundleService.countPublic();
        int totalPages = total == 0 ? 1 : (total + PAGE_SIZE - 1) / PAGE_SIZE;

        req.setAttribute("bundles", bundles);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", PAGE_SIZE);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("sort", sort);

        return ViewResult.forward("bundle/list");
    }

    private static int parsePage(String s) {
        if (s == null) return 1;
        try { int p = Integer.parseInt(s.trim()); return p < 1 ? 1 : p; }
        catch (NumberFormatException nfe) { return 1; }
    }
}
