package local.promptmark.web;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;

public class TraceIdFilter implements Filter {

    private static final String MDC_KEY = "traceId";
    private static final String HEADER  = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_KEY, traceId);
        req.setAttribute(MDC_KEY, traceId);
        if (res instanceof HttpServletResponse) {
            ((HttpServletResponse) res).setHeader(HEADER, traceId);
        }
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
