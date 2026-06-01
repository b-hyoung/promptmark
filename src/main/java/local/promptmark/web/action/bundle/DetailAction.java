package local.promptmark.web.action.bundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dao.BundleDao;
import local.promptmark.dto.Bundle;
import local.promptmark.service.BundleService;
import local.promptmark.web.Action;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/** GET /app/bundle/detail?id=N */
public class DetailAction implements Action {
    private final BundleService bundleService;
    private final BundleDao bundleDao;

    public DetailAction(BundleService bundleService, BundleDao bundleDao) {
        this.bundleService = bundleService;
        this.bundleDao = bundleDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        long id = parseId(req.getParameter("id"));
        Bundle b = bundleService.findByIdWithPlugins(id)
            .orElseThrow(() -> new NotFoundException("Bundle not found: " + id));
        bundleDao.incrementViewCount(id);
        req.setAttribute("bundle", b);
        return ViewResult.forward("bundle/detail");
    }

    private static long parseId(String s) {
        if (s == null) throw new NotFoundException("id required");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("invalid id: " + s); }
    }
}
