package local.promptmark.web.action.bundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.LoginUser;
import local.promptmark.service.BundleService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/** POST /app/bundle/delete?id=N */
public class DeleteAction implements Action {
    private final BundleService bundleService;

    public DeleteAction(BundleService bundleService) { this.bundleService = bundleService; }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser admin = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        long id = parseId(req.getParameter("id"));
        bundleService.deleteBundle(admin, id);
        return ViewResult.redirect(req.getContextPath() + "/app/bundle/list");
    }

    private static long parseId(String s) {
        try { return Long.parseLong(s.trim()); }
        catch (Exception e) { throw new NotFoundException("invalid id"); }
    }
}
