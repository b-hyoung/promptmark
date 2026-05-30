package local.promptmark.web.action.auth;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.service.AuthService;
import local.promptmark.web.Action;
import local.promptmark.web.ValidationException;
import local.promptmark.web.ViewResult;

/** POST {@code /app/auth/signup} — create the user, redirect to login. */
public class SignupAction implements Action {

    private final AuthService authService;

    public SignupAction(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String passwordConfirm = req.getParameter("password_confirm");
        String nickname = req.getParameter("nickname");
        String role = req.getParameter("role");

        try {
            authService.signup(email, password, passwordConfirm, nickname, role);
            String ctx = req.getContextPath();
            return ViewResult.redirect(ctx + "/app/auth/login?msg=signup_ok");
        } catch (ValidationException ve) {
            // Re-show the form with errors + echoed inputs (minus password).
            Map<String, String> form = new LinkedHashMap<>();
            form.put("email", email == null ? "" : email);
            form.put("nickname", nickname == null ? "" : nickname);
            form.put("role", role == null ? "" : role);
            req.setAttribute("form", form);
            req.setAttribute("errors", ve.fieldErrors());
            req.setAttribute("errorMessage", ve.userMessage());
            return ViewResult.forward("auth/signup");
        }
        // ConflictException bubbles up to FrontController → 409 page.
    }
}
