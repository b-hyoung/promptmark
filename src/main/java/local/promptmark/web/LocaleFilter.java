package local.promptmark.web;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.jstl.core.Config;

/**
 * Propagates the request's {@link Locale} to JSTL's fmt-tag config so
 * {@code <fmt:message>} resolves against the right {@code messages_xx.properties}
 * bundle. Reads {@code Accept-Language} indirectly via
 * {@link ServletRequest#getLocale()} (container default).
 *
 * <p>Maps at {@code /*} — fmt only kicks in when a JSP uses {@code <fmt:message>}.
 * Order is not strictly important; we install it before AuthFilter to keep the
 * locale visible to the (rare) error forward path that bypasses the front
 * controller.
 */
public class LocaleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        Locale loc = req.getLocale();
        if (loc != null) {
            Config.set(req, Config.FMT_LOCALE, loc);
        }
        chain.doFilter(req, res);
    }
}
