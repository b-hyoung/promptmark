package local.promptmark.web.action.demo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.web.Action;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/**
 * GET /app/demo/show?slug=<bundle-slug>
 *
 * <p>Forwards to a per-slug JSP that demonstrates what the bundle actually
 * produces (a fake but plausible output crafted to match each bundle's claim).
 * The slug whitelist keeps this from being a path traversal.
 */
public class ShowAction implements Action {

    private static final java.util.Set<String> ALLOWED = java.util.Set.of(
        "blog-automation", "code-quality", "design-ready",
        "ad-copy", "ai-app-launch", "mvp-bootstrap"
    );

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        String slug = req.getParameter("slug");
        if (slug == null || !ALLOWED.contains(slug)) {
            throw new NotFoundException("demo not found: " + slug);
        }
        req.setAttribute("slug", slug);
        return ViewResult.forward("demo/" + slug);
    }
}
