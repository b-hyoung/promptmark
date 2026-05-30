package local.promptmark.web.action.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import local.promptmark.dto.LoginUser;
import local.promptmark.service.AuthService;
import local.promptmark.web.Action;
import local.promptmark.web.SafeNext;
import local.promptmark.web.UnauthorizedException;
import local.promptmark.web.ViewResult;

/** POST {@code /app/auth/login} — verify, set session, redirect to {@code next}. */
public class LoginAction implements Action {

    private final AuthService authService;

    public LoginAction(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        try {
            LoginUser lu = authService.login(email, password);

            // Roll the session id to prevent fixation, then bind LOGIN_USER.
            req.changeSessionId();
            HttpSession session = req.getSession(true);
            session.setAttribute("LOGIN_USER", lu);

            String ctx = req.getContextPath();
            String target = SafeNext.resolve(req, ctx + "/");
            // If SafeNext returned a /app/... path, prefix the context so the
            // redirect lands inside our webapp.
            if (target.startsWith("/app/")) {
                target = ctx + target;
            }
            return ViewResult.redirect(target);

        } catch (UnauthorizedException ue) {
            req.setAttribute("errorMessage", ue.userMessage());
            req.setAttribute("emailEcho", email == null ? "" : email);
            String next = SafeNext.resolve(req, "");
            req.setAttribute("next", next);
            return ViewResult.forward("auth/login");
        }
    }
}
