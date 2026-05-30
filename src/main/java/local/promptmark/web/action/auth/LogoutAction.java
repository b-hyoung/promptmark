package local.promptmark.web.action.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/** POST {@code /app/auth/logout} — invalidate session, redirect to home. */
public class LogoutAction implements Action {

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ViewResult.redirect(req.getContextPath() + "/");
    }
}
