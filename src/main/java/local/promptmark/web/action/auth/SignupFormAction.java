package local.promptmark.web.action.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/** GET {@code /app/auth/signup} — show the signup form. */
public class SignupFormAction implements Action {
    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        return ViewResult.forward("auth/signup");
    }
}
