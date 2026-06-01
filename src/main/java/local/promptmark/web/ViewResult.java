package local.promptmark.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Value object describing how an Action's outcome should be rendered.
 * Four flavours: forward to a JSP, 302 redirect, write JSON, stream bytes.
 */
public final class ViewResult {

    enum Kind { FORWARD, REDIRECT, JSON, BINARY }

    private final Kind kind;
    private final String target;     // view name (FORWARD) or url (REDIRECT)
    private final Object body;       // JSON payload
    private final byte[] content;    // binary payload
    private final String filename;   // binary attachment name
    private final String contentType;// binary content type

    private ViewResult(Kind kind, String target, Object body,
                       byte[] content, String filename, String contentType) {
        this.kind = kind;
        this.target = target;
        this.body = body;
        this.content = content;
        this.filename = filename;
        this.contentType = contentType;
    }

    /** Forward to {@code /WEB-INF/view/<viewName>.jsp}. */
    public static ViewResult forward(String viewName) {
        return new ViewResult(Kind.FORWARD, viewName, null, null, null, null);
    }

    /** 302 redirect to {@code url} (absolute or context-relative). */
    public static ViewResult redirect(String url) {
        return new ViewResult(Kind.REDIRECT, url, null, null, null, null);
    }

    /** Write {@code body} as JSON with {@code application/json; charset=UTF-8}. */
    public static ViewResult json(Object body) {
        return new ViewResult(Kind.JSON, null, body, null, null, null);
    }

    /**
     * Stream {@code content} as an attachment. Sets {@code Content-Type},
     * {@code Content-Disposition: attachment; filename="..."} (UTF-8 + RFC 5987
     * fallback for non-ASCII names), and {@code Content-Length}.
     */
    public static ViewResult binary(byte[] content, String filename, String contentType) {
        return new ViewResult(Kind.BINARY, null, null, content, filename, contentType);
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
            case BINARY:
                writeBinary(res);
                return;
            default:
                throw new IllegalStateException("Unknown ViewResult kind: " + kind);
        }
    }

    private void writeBinary(HttpServletResponse res) throws IOException {
        String safeName = filename == null ? "download" : filename;
        String mime = contentType == null ? "application/octet-stream" : contentType;
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType(mime);
        res.setContentLength(content == null ? 0 : content.length);
        // Belt-and-braces: ASCII filename + RFC 5987 UTF-8 filename* so browsers
        // get a sensible save name even when the plugin title is non-ASCII.
        String asciiName = toAscii(safeName);
        String encoded = java.net.URLEncoder.encode(safeName, StandardCharsets.UTF_8.name())
            .replace("+", "%20");
        res.setHeader("Content-Disposition",
            "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encoded);
        if (content != null) {
            try (OutputStream os = res.getOutputStream()) {
                os.write(content);
                os.flush();
            }
        }
    }

    private static String toAscii(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < 0x20 || ch >= 0x7F || ch == '"' || ch == '\\') {
                sb.append('_');
            } else {
                sb.append(ch);
            }
        }
        String out = sb.toString();
        return out.isEmpty() ? "download" : out;
    }
}
