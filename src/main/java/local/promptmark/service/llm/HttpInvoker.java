package local.promptmark.service.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single-abstraction-method seam around {@link HttpClient#send} so the OpenAI
 * and Claude clients can be unit-tested without real network I/O. Production
 * code uses {@link #real()}; tests inject a lambda that returns canned
 * responses.
 */
@FunctionalInterface
public interface HttpInvoker {

    /**
     * Send {@code body} as POST and return {@code (status, body)} pair.
     * Implementations are expected to use {@code application/json; charset=utf-8}
     * unless overridden via {@code headers}.
     */
    Response post(String url, Map<String, String> headers, String body, Duration timeout)
        throws IOException, InterruptedException;

    /** Real JDK HttpClient-backed invoker used in production. */
    static HttpInvoker real() {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
        return (url, headers, body, timeout) -> {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=utf-8");
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    if (e.getKey() == null || e.getValue() == null) continue;
                    if ("Content-Type".equalsIgnoreCase(e.getKey())) continue;
                    b.header(e.getKey(), e.getValue());
                }
            }
            HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            return new Response(res.statusCode(), res.body());
        };
    }

    /** Status + body pair returned by {@link #post}. */
    final class Response {
        private final int status;
        private final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }

        public int status() { return status; }
        public String body() { return body; }
        public boolean isSuccess() { return status >= 200 && status < 300; }
    }

    /**
     * Helper for callers building headers maps inline.
     */
    static Map<String, String> headers(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        if (kv == null) return m;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }
}
