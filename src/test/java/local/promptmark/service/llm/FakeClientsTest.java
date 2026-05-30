package local.promptmark.service.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class FakeClientsTest {

    @Test
    void fake_llm_returns_replies_in_order() {
        LlmReply r1 = new LlmReply("assistant", "a", Collections.emptyList());
        LlmReply r2 = new LlmReply("assistant", "b", Collections.emptyList());
        FakeLlmClient c = new FakeLlmClient(r1, r2);

        LlmRequest req = new LlmRequest(Collections.emptyList(), null, Duration.ofSeconds(1));
        assertThat(c.chat(req).content()).isEqualTo("a");
        assertThat(c.chat(req).content()).isEqualTo("b");
    }

    @Test
    void fake_llm_throws_when_exhausted() {
        FakeLlmClient c = new FakeLlmClient(
            new LlmReply("assistant", "x", Collections.emptyList()));
        LlmRequest req = new LlmRequest(Collections.emptyList(), null, Duration.ofSeconds(1));
        c.chat(req);
        assertThatExceptionOfType(LlmException.class).isThrownBy(() -> c.chat(req));
    }

    @Test
    void fake_llm_records_received_requests() {
        FakeLlmClient c = new FakeLlmClient(
            new LlmReply("assistant", "ok", Collections.emptyList()));
        c.chat(new LlmRequest(Collections.singletonList(Message.user("hi")),
            null, Duration.ofSeconds(1)));
        assertThat(c.receivedRequests()).hasSize(1);
        assertThat(c.callCount()).isEqualTo(1);
    }

    @Test
    void disabled_embedding_throws() {
        DisabledEmbeddingClient c = new DisabledEmbeddingClient();
        assertThat(c.enabled()).isFalse();
        assertThatExceptionOfType(LlmException.class).isThrownBy(() -> c.embed("x"));
    }

    @Test
    void fake_embedding_is_deterministic() {
        FakeEmbeddingClient c = new FakeEmbeddingClient();
        float[] a = c.embed("hello");
        float[] b = c.embed("hello");
        assertThat(a).hasSize(FakeEmbeddingClient.DIM);
        assertThat(b).containsExactly(a);
    }

    @Test
    void fake_embedding_records_inputs() {
        FakeEmbeddingClient c = new FakeEmbeddingClient();
        c.embed("alpha");
        c.embed("beta");
        assertThat(c.received()).containsExactly("alpha", "beta");
    }

    @Test
    void fake_embedding_is_unit_normalised() {
        FakeEmbeddingClient c = new FakeEmbeddingClient();
        float[] v = c.embed("hello");
        double sumSq = 0;
        for (float f : v) sumSq += f * f;
        assertThat(Math.sqrt(sumSq)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-3));
    }

    @Test
    void disabled_llm_client_throws() {
        DisabledLlmClient c = new DisabledLlmClient();
        assertThat(c.enabled()).isFalse();
        LlmRequest req = new LlmRequest(Collections.emptyList(), null, Duration.ofSeconds(1));
        assertThatExceptionOfType(LlmException.class).isThrownBy(() -> c.chat(req));
    }
}
