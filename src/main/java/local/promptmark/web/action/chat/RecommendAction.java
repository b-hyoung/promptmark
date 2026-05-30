package local.promptmark.web.action.chat;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import local.promptmark.service.RecommendService;
import local.promptmark.service.llm.AgentResult;
import local.promptmark.web.Action;
import local.promptmark.web.AppException;
import local.promptmark.web.ValidationException;
import local.promptmark.web.ViewResult;

/**
 * {@code POST /app/chat/recommend} — JSON in, JSON out. CSRF is verified by
 * {@code CsrfFilter} via the {@code X-CSRF-Token} header.
 */
public final class RecommendAction implements Action {

    private final RecommendService service;
    private final ObjectMapper mapper = new ObjectMapper();

    public RecommendAction(RecommendService service) {
        this.service = service;
    }

    @Override
    public boolean producesJson() {
        return true;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String message = readMessage(req);
        if (message == null) {
            throw new ValidationException("message 필드가 필요합니다",
                java.util.Collections.singletonMap("message", "필수 입력"));
        }
        AgentResult result = service.recommend(message);
        return ViewResult.json(result.toMap());
    }

    private String readMessage(HttpServletRequest req) throws IOException {
        try (InputStream in = req.getInputStream()) {
            if (in == null) return null;
            JsonNode node;
            try {
                node = mapper.readTree(in);
            } catch (Exception parseErr) {
                throw new AppException(400, "JSON 본문을 해석할 수 없습니다");
            }
            if (node == null || node.isMissingNode() || node.isNull()) return null;
            JsonNode msg = node.path("message");
            if (msg.isMissingNode() || msg.isNull()) return null;
            return msg.asText();
        }
    }
}
