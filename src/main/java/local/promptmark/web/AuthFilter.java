package local.promptmark.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import local.promptmark.dto.LoginUser;

/**
 * Gatekeeper for {@code /app/*}. Resolves the required role from
 * {@link AuthMap}, then:
 * <ul>
 *   <li>logged out + anonymous-allowed → pass through</li>
 *   <li>logged out + login required → 302 to login with safe {@code next}</li>
 *   <li>logged in + role sufficient → pass through</li>
 *   <li>logged in + role insufficient → 403 forbidden.jsp</li>
 * </ul>
 *
 * <p>BANNED sessions are detected by an out-of-band marker on the LoginUser
 * (set during login). Phase 2 only puts ACTIVE users on the session, so this
 * filter additionally invalidates and bounces to login if some path slipped a
 * banned user through (defence in depth).
 */
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    /** Session attribute name agreed across the auth surface. */
    public static final String LOGIN_USER_ATTR = "LOGIN_USER";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest hreq = (HttpServletRequest) req;
        HttpServletResponse hres = (HttpServletResponse) res;

        String pathInfo = hreq.getPathInfo();
        String actionKey = FrontController.parseActionKey(pathInfo);

        // No mappable key (404 fall-through): let FrontController emit the 404.
        if (actionKey == null) {
            chain.doFilter(req, res);
            return;
        }

        Role required = AuthMap.required(actionKey);

        HttpSession session = hreq.getSession(false);
        LoginUser user = (session == null)
            ? null
            : (LoginUser) session.getAttribute(LOGIN_USER_ATTR);

        Role current = (user == null) ? Role.ANONYMOUS : user.getRole();

        // Bounce anyone whose session ended up banned (defence in depth).
        if (user != null && session != null) {
            Object banned = session.getAttribute("STATUS_BANNED");
            if (Boolean.TRUE.equals(banned)) {
                String traceId = String.valueOf(hreq.getAttribute("traceId"));
                log.info("Invalidating banned session userId={} [trace={}]",
                         user.getId(), traceId);
                session.invalidate();
                redirectToLogin(hreq, hres, "정지된 계정입니다");
                return;
            }
        }

        if (required.satisfiedBy(current)) {
            chain.doFilter(req, res);
            return;
        }

        // Insufficient role.
        if (user == null) {
            // Not logged in — bounce to login with the original target preserved.
            redirectToLogin(hreq, hres, null);
            return;
        }
        // Logged in but lacking the role → 403.
        hres.setStatus(HttpServletResponse.SC_FORBIDDEN);
        hreq.setAttribute("errorMessage", "이 작업을 수행할 권한이 없습니다");
        hreq.getRequestDispatcher("/WEB-INF/view/error/forbidden.jsp").forward(hreq, hres);
    }

    private void redirectToLogin(HttpServletRequest req,
                                 HttpServletResponse res,
                                 String message) throws IOException {
        StringBuilder current = new StringBuilder(req.getRequestURI());
        String qs = req.getQueryString();
        if (qs != null && !qs.isEmpty()) {
            current.append('?').append(qs);
        }
        String ctx = req.getContextPath();
        // Strip the context path off so SafeNext sees a /app/... value.
        String full = current.toString();
        String next = ctx != null && !ctx.isEmpty() && full.startsWith(ctx)
            ? full.substring(ctx.length())
            : full;
        if (!next.startsWith("/app/")) {
            next = "/app/asset/list";
        }
        String encoded = URLEncoder.encode(next, StandardCharsets.UTF_8.name());
        String target = ctx + "/app/auth/login?next=" + encoded;
        if (message != null) {
            target += "&msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8.name());
        }
        res.sendRedirect(target);
    }
}
