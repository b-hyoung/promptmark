package local.promptmark.web.action.bundle;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dao.PluginDao;
import local.promptmark.dto.Plugin;
import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/** GET /app/bundle/new — show empty bundle form (ADMIN only). */
public class CreateFormAction implements Action {
    private final PluginDao pluginDao;

    public CreateFormAction(PluginDao pluginDao) { this.pluginDao = pluginDao; }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        List<Plugin> allPlugins = pluginDao.search(null, null, null, "recent", 0, 200);
        req.setAttribute("allPlugins", allPlugins);
        req.setAttribute("mode", "new");
        return ViewResult.forward("bundle/form");
    }
}
