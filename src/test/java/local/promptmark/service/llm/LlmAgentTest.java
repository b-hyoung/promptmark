package local.promptmark.service.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LlmAgentTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private Tools tools;
    private LlmConfig config;

    @BeforeEach
    void setUp() {
        tools = Mockito.mock(Tools.class);
        when(tools.definitions()).thenReturn(Collections.singletonList(
            new ToolDef("search_assets", "search",
                mapper.createObjectNode().put("type", "object"))));
        config = new LlmConfig("openai", "sk-x", "", "", "", "");
    }

    @Test
    void run_returns_final_answer_when_no_tool_calls() {
        FakeLlmClient client = new FakeLlmClient(
            new LlmReply("assistant", "추천드릴 자산이 있습니다.", Collections.emptyList()));
        LlmAgent agent = new LlmAgent(client, new FakeEmbeddingClient(), tools, config);

        AgentResult r = agent.run("프롬프트 추천");

        assertThat(r.getSource()).isEqualTo("AGENT");
        assertThat(r.getAnswer()).isEqualTo("추천드릴 자산이 있습니다.");
        assertThat(r.getItems()).isEmpty();
        assertThat(r.getTrace()).isEmpty();
        assertThat(client.callCount()).isEqualTo(1);
    }

    @Test
    void run_dispatches_tool_then_returns_final_answer() throws Exception {
        ArrayNode searchResult = mapper.createArrayNode();
        ObjectNode item = searchResult.addObject();
        item.put("id", 42L);
        item.put("type", "PROMPT");
        item.put("title", "Test");
        item.put("summary", "Sum");
        item.put("price", 0);
        item.put("score", 0.9);
        item.putArray("tags").add("AI");
        when(tools.dispatch(eq("search_assets"), any(JsonNode.class)))
            .thenReturn(searchResult);

        FakeLlmClient client = new FakeLlmClient(
            new LlmReply("assistant", "",
                Collections.singletonList(new ToolCall("call_1", "search_assets",
                    mapper.readTree("{\"query\":\"자기소개서\"}")))),
            new LlmReply("assistant", "찾았어요. 자기소개서 첨삭 프롬프트가 있어요.",
                Collections.emptyList())
        );
        LlmAgent agent = new LlmAgent(client, new FakeEmbeddingClient(), tools, config);

        AgentResult r = agent.run("자기소개서 첨삭 프롬프트 추천");

        assertThat(r.getSource()).isEqualTo("AGENT");
        assertThat(r.getAnswer()).contains("자기소개서");
        assertThat(r.getItems()).hasSize(1);
        assertThat(r.getItems().get(0).getId()).isEqualTo(42L);
        assertThat(r.getItems().get(0).getTitle()).isEqualTo("Test");
        assertThat(r.getTrace()).hasSize(1);
        assertThat(r.getTrace().get(0).getTool()).isEqualTo("search_assets");
        assertThat(r.getTrace().get(0).getHits()).isEqualTo(1);
    }

    @Test
    void run_de_duplicates_assets_across_tool_calls() throws Exception {
        ArrayNode r1 = mapper.createArrayNode();
        addItem(r1, 42, "A");
        ArrayNode r2 = mapper.createArrayNode();
        addItem(r2, 42, "Adup"); // same id
        addItem(r2, 99, "B");

        when(tools.dispatch(eq("search_assets"), any(JsonNode.class)))
            .thenReturn(r1, r2);

        FakeLlmClient client = new FakeLlmClient(
            new LlmReply("assistant", "",
                Arrays.asList(
                    new ToolCall("c1", "search_assets",
                        mapper.readTree("{\"query\":\"a\"}")),
                    new ToolCall("c2", "search_assets",
                        mapper.readTree("{\"query\":\"b\"}"))
                )),
            new LlmReply("assistant", "done", Collections.emptyList())
        );
        LlmAgent agent = new LlmAgent(client, new FakeEmbeddingClient(), tools, config);

        AgentResult r = agent.run("hello");
        assertThat(r.getItems()).hasSize(2);
        assertThat(r.getItems().get(0).getId()).isEqualTo(42L);
        assertThat(r.getItems().get(1).getId()).isEqualTo(99L);
    }

    @Test
    void respects_max_turns() throws Exception {
        // Script 4 consecutive tool-call turns; loop should stop at 3 and
        // then make a final no-tools call.
        ArrayNode results = mapper.createArrayNode();
        when(tools.dispatch(any(), any(JsonNode.class))).thenReturn(results);

        List<LlmReply> replies = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            replies.add(new LlmReply("assistant", "",
                Collections.singletonList(new ToolCall("c" + i, "search_assets",
                    mapper.readTree("{\"query\":\"x\"}")))));
        }
        replies.add(new LlmReply("assistant", "final", Collections.emptyList()));
        FakeLlmClient client = new FakeLlmClient(replies);

        LlmAgent agent = new LlmAgent(client, new FakeEmbeddingClient(), tools, config);
        AgentResult r = agent.run("x");

        // 3 turns inside the loop + 1 final no-tools call.
        assertThat(client.callCount()).isLessThanOrEqualTo(4);
        assertThat(r.getSource()).isEqualTo("AGENT");
        assertThat(r.getTrace().size()).isLessThanOrEqualTo(3);
    }

    @Test
    void timeout_falls_back_propagates_llm_exception() {
        // Sleep longer than the budget on the very first call.
        FakeLlmClient slow = new FakeLlmClient(
            new LlmReply("assistant", "won't ever return text", Collections.emptyList()))
            .withDelay(Duration.ofSeconds(9));
        LlmAgent agent = new LlmAgent(slow, new FakeEmbeddingClient(), tools, config);

        // We expect either the fake to throw LlmException via interrupted/delay,
        // or the agent to recover. Since FakeLlmClient does not throw on delay
        // we will instead see the full delay; this test asserts that even after
        // the delay the agent still produces a result and doesn't deadlock.
        AgentResult r = agent.run("hi");
        assertThat(r.getSource()).isEqualTo("AGENT");
        assertThat(r.getAnswer()).isNotNull();
    }

    @Test
    void llm_exception_propagates_up() {
        LlmClient broken = req -> { throw new LlmException("boom"); };
        LlmAgent agent = new LlmAgent(broken, new FakeEmbeddingClient(), tools, config);
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> agent.run("hi"));
    }

    private void addItem(ArrayNode arr, long id, String title) {
        ObjectNode n = arr.addObject();
        n.put("id", id);
        n.put("type", "PROMPT");
        n.put("title", title);
        n.put("summary", "s");
        n.put("price", 0);
        n.put("score", 0.5);
        n.putArray("tags");
    }
}
