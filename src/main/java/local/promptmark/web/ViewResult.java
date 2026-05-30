package local.promptmark.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Value object describing how an Action's outcome should be rendered.
 * Three flavours: forward to a JSP, send a 302 redirect, write JSON.
 */
public final class ViewResult {

    enum Kind { FORWARD, REDIRECT, JSON }

    private final Kind kind;
    private final String target;     // view name (FORWARD) or url (REDIRECT)
    private final Object body;       // JSON payload

    private ViewResult(Kind kind, String target, Object body) {
        this.kind = kind;
        this.target = target;
        this.body = body;
    }

    /** Forward to {@code /WEB-INF/view/<viewName>.jsp}. */
    public static ViewResult forward(String viewName) {
        return new ViewResult(Kind.FORWARD, viewName, null);
    }

    /** 302 redirect to {@code url} (absolute or context-relative). */
    public static ViewResult redirect(String url) {
        return new ViewResult(Kind.REDIRECT, url, null);
    }

    /** Write {@code body} as JSON with {@code application/json; charset=UTF-8}. */
    public static ViewResult json(Object body) {
        return new ViewResult(Kind.JSON, null, body);
    }

    Kind kind() { return kind; }
    String target() { return target; }
    Object body() { return body; }

    /** Performed by FrontController after the action returns. */
    void apply(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        switch (kind) {
            case FORWARD:
                req.getRequestDispatcher("/WEB-INF/view/" + target + ".jsp")
                   .forward(req, res);
                return;
            case REDIRECT:
                res.sendRedirect(target);
                return;
            case JSON:
                res.setContentType("application/json; charset=UTF-8");
                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                try (PrintWriter w = res.getWriter()) {
                    w.write(JsonWriter.toJson(body));
                }
                return;
            default:
                throw new IllegalStateException("Unknown ViewResult kind: " + kind);
        }
    }
}
