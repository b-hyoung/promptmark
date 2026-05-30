package local.promptmark.web;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

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

/**
 * CSRF defence for state-changing requests under {@code /app/*}.
 * <ul>
 *   <li>GET: ensures the session has a {@code CSRF_TOKEN}; emits one if absent.</li>
 *   <li>POST (and DELETE/PUT/PATCH): compares the {@code csrf_token} form
 *       parameter or {@code X-CSRF-Token} header against the session value.
 *       Mismatch → 403 forbidden.jsp.</li>
 * </ul>
 */
public class CsrfFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);

    public static final String SESSION_ATTR = "CSRF_TOKEN";
    public static final String FORM_PARAM   = "csrf_token";
    public static final String HEADER       = "X-CSRF-Token";

    private final SecureRandom random = new SecureRandom();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest hreq = (HttpServletRequest) req;
        HttpServletResponse hres = (HttpServletResponse) res;
        String method = hreq.getMethod();

        HttpSession session = hreq.getSession(true);
        Object existing = session.getAttribute(SESSION_ATTR);
        if (existing == null) {
            session.setAttribute(SESSION_ATTR, mintToken());
        }

        if (isStateChanging(method)) {
            String sessionToken = String.valueOf(session.getAttribute(SESSION_ATTR));
            String submitted = hreq.getParameter(FORM_PARAM);
            if (submitted == null || submitted.isEmpty()) {
                submitted = hreq.getHeader(HEADER);
            }
            if (submitted == null || !constantTimeEquals(sessionToken, submitted)) {
                String traceId = String.valueOf(hreq.getAttribute("traceId"));
                log.warn("CSRF rejection method={} uri={} [trace={}]",
                         method, hreq.getRequestURI(), traceId);
                hres.setStatus(HttpServletResponse.SC_FORBIDDEN);
                hreq.setAttribute("errorMessage", "CSRF 토큰이 올바르지 않습니다");
                hreq.getRequestDispatcher("/WEB-INF/view/error/forbidden.jsp")
                    .forward(hreq, hres);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private String mintToken() {
        byte[] buf = new byte[16];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static boolean isStateChanging(String method) {
        return "POST".equalsIgnoreCase(method)
            || "PUT".equalsIgnoreCase(method)
            || "PATCH".equalsIgnoreCase(method)
            || "DELETE".equalsIgnoreCase(method);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
