package local.promptmark.web.action.info;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/** GET /app/info/about — site identity, curation philosophy, curator intro. */
public class AboutAction implements Action {
    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        return ViewResult.forward("info/about");
    }
}
