package local.promptmark.web.action.plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.LoginUser;
import local.promptmark.service.PluginService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/** POST {@code /app/plugin/delete?id=N} — soft-delete + bounce back to list. */
public class DeleteAction implements Action {

    private final PluginService pluginService;

    public DeleteAction(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        long id = parseLong(req.getParameter("id"));
        pluginService.deletePlugin(id, me);
        return ViewResult.redirect(req.getContextPath() + "/app/plugin/list?msg=deleted_ok");
    }

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("자산을 찾을 수 없습니다");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("자산을 찾을 수 없습니다"); }
    }
}
