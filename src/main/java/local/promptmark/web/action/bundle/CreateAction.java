package local.promptmark.web.action.bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.LoginUser;
import local.promptmark.service.BundleService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.ViewResult;

/** POST /app/bundle/new */
public class CreateAction implements Action {
    private final BundleService bundleService;

    public CreateAction(BundleService bundleService) { this.bundleService = bundleService; }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser admin = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);

        Map<String, String> form = new HashMap<>();
        form.put("slug",      req.getParameter("slug"));
        form.put("name",      req.getParameter("name"));
        form.put("tagline",   req.getParameter("tagline"));
        form.put("story",     req.getParameter("story"));
        form.put("price",     req.getParameter("price"));
        form.put("thumbnail", req.getParameter("thumbnail"));

        String[] pluginIdParams = req.getParameterValues("pluginIds");
        List<Long> pluginIds = new ArrayList<>();
        if (pluginIdParams != null) {
            for (String s : pluginIdParams) {
                try { pluginIds.add(Long.parseLong(s.trim())); }
                catch (NumberFormatException ignored) {}
            }
        }

        long id = bundleService.createBundle(admin, form, pluginIds);
        return ViewResult.redirect(req.getContextPath() + "/app/bundle/detail?id=" + id);
    }
}
