package local.promptmark.web.action.home;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.service.BundleService;
import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/**
 * GET /app/home/index — true marketplace landing.
 *
 * <p>Loads all public bundles with their plugins so the home page can show
 * the catalog as the primary content (no separate plugin grid — bundles are
 * the curation story, plugins are the components inside).
 */
public class IndexAction implements Action {
    private final BundleService bundleService;

    public IndexAction(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        req.setAttribute("bundles", bundleService.listPublic("recent", 0, 12));
        req.setAttribute("bundleCount", bundleService.countPublic());
        return ViewResult.forward("home/index");
    }
}
