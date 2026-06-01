package local.promptmark.web.action.plugin;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dao.TagDao;
import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/** GET {@code /app/plugin/new} — empty form for plugin creation. */
public class CreateFormAction implements Action {

    private final TagDao tagDao;

    public CreateFormAction(TagDao tagDao) {
        this.tagDao = tagDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        req.setAttribute("mode", "create");
        req.setAttribute("tags", tagDao.findAllOrLimit(100));
        req.setAttribute("form", Collections.emptyMap());
        req.setAttribute("errors", Collections.emptyMap());
        return ViewResult.forward("plugin/form");
    }
}
