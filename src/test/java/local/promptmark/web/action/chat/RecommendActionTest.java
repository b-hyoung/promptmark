package local.promptmark.web.action.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import local.promptmark.service.RecommendService;
import local.promptmark.service.llm.AgentResult;
import local.promptmark.web.ViewResult;
import local.promptmark.web.ValidationException;

class RecommendActionTest {

    private static ServletInputStream wrap(String body) {
        ByteArrayInputStream raw = new ByteArrayInputStream(body.getBytes());
        return new ServletInputStream() {
            @Override public int read() { return raw.read(); }
            @Override public boolean isFinished() { return raw.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(javax.servlet.ReadListener listener) {}
        };
    }

    @Test
    void produces_json_is_true() {
        RecommendService svc = Mockito.mock(RecommendService.class);
        RecommendAction action = new RecommendAction(svc);
        assertThat(action.producesJson()).isTrue();
    }

    @Test
    void execute_invokes_service_with_message() throws Exception {
        RecommendService svc = Mockito.mock(RecommendService.class);
        AgentResult expected = new AgentResult("ok", "AGENT",
            Collections.emptyList(), Collections.emptyList());
        when(svc.recommend("hello world")).thenReturn(expected);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        when(req.getInputStream()).thenReturn(wrap("{\"message\":\"hello world\"}"));

        RecommendAction action = new RecommendAction(svc);
        ViewResult vr = action.execute(req, res);
        assertThat(vr).isNotNull();
    }

    @Test
    void missing_message_throws_validation() throws Exception {
        RecommendService svc = Mockito.mock(RecommendService.class);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        when(req.getInputStream()).thenReturn(wrap("{}"));

        RecommendAction action = new RecommendAction(svc);
        assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(() -> action.execute(req, res));
    }
}
