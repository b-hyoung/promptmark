package local.promptmark.web.action.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.web.Action;
import local.promptmark.web.SafeNext;
import local.promptmark.web.ViewResult;

/** GET {@code /app/auth/login} — show the login form. */
public class LoginFormAction implements Action {

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        // Sanitise ?next so the hidden field can't smuggle in an open-redirect.
        String next = SafeNext.resolve(req, "");
        req.setAttribute("next", next);

        String msg = req.getParameter("msg");
        if ("signup_ok".equals(msg)) {
            req.setAttribute("notice", "회원가입이 완료되었습니다. 로그인해주세요.");
        }
        return ViewResult.forward("auth/login");
    }
}
