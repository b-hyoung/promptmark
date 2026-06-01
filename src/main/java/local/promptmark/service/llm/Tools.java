package local.promptmark.service.llm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.TagDao;
import local.promptmark.dao.UserDao;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.PluginType;
import local.promptmark.dto.Tag;
import local.promptmark.web.NotFoundException;

/**
 * The agent's two callable tools: {@code search_plugins} and
 * {@code get_plugin_detail}. Each returns a {@link JsonNode} that gets fed back
 * into the next LLM turn as the tool result message.
 *
 * <p>UserDao is held even though we don't query it yet — keeping the
 * constructor stable lets future tools (seller stats etc) plug in without
 * touching call sites.
 */
public final class Tools {

    public static final String TOOL_SEARCH_PLUGINS = "search_plugins";
    public static final String TOOL_GET_PLUGIN_DETAIL = "get_plugin_detail";

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final int BODY_PREVIEW_MAX = 500;

    private final PluginDao pluginDao;
    private final TagDao tagDao;
    @SuppressWarnings("unused")
    private final UserDao userDao;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public Tools(PluginDao pluginDao, TagDao tagDao, UserDao userDao,
                 EmbeddingClient embeddingClient) {
        this.pluginDao = pluginDao;
        this.tagDao = tagDao;
        this.userDao = userDao;
        this.embeddingClient = embeddingClient;
    }

    /** Returns the OpenAI-style schema array used by the agent. */
    public List<ToolDef> definitions() {
        try {
            JsonNode searchSchema = mapper.readTree("{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                +   "\"query\":{\"type\":\"string\"},"
                +   "\"type\":{\"type\":\"string\",\"enum\":[\"PROMPT\",\"MD\"]},"
                +   "\"max_price\":{\"type\":\"integer\"},"
                +   "\"limit\":{\"type\":\"integer\",\"default\":10,\"maximum\":20}"
                + "},"
                + "\"required\":[\"query\"]"
                + "}");
            JsonNode detailSchema = mapper.readTree("{"
                + "\"type\":\"object\","
                + "\"properties\":{\"id\":{\"type\":\"integer\"}},"
                + "\"required\":[\"id\"]"
                + "}");
            return Arrays.asList(
                new ToolDef(TOOL_SEARCH_PLUGINS,
                    "프롬프트/MD 자산 검색. 키워드+벡터 하이브리드.", searchSchema),
                new ToolDef(TOOL_GET_PLUGIN_DETAIL,
                    "특정 자산의 본문 미리보기·태그·데모 URL 조회.", detailSchema)
            );
        } catch (Exception e) {
            throw new LlmException("failed to build tool definitions", e);
        }
    }

    /** Routes a model-issued tool call to the correct method. */
    public JsonNode dispatch(String name, JsonNode args) {
        if (TOOL_SEARCH_PLUGINS.equals(name)) return searchPlugins(args);
        if (TOOL_GET_PLUGIN_DETAIL.equals(name)) return getPluginDetail(args);
        throw new LlmException("unknown tool: " + name);
    }

    /**
     * Tokenise + embed + hybrid search → JSON array of card-shaped objects.
     */
    public JsonNode searchPlugins(JsonNode args) {
        if (args == null || args.isNull()) args = mapper.createObjectNode();
        String query = args.path("query").asText("");
        if (query == null || query.trim().isEmpty()) {
            // Empty query → empty result rather than 400.
            return mapper.createArrayNode();
        }
        PluginType type = null;
        String typeStr = args.path("type").asText(null);
        if (typeStr != null && !typeStr.isEmpty()) {
            try {
                type = PluginType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                type = null;
            }
        }
        Integer maxPrice = null;
        if (args.has("max_price") && args.path("max_price").isNumber()) {
            maxPrice = args.path("max_price").asInt();
        }
        int limit = DEFAULT_LIMIT;
        if (args.has("limit") && args.path("limit").isNumber()) {
            limit = args.path("limit").asInt(DEFAULT_LIMIT);
        }
        if (limit < 1) limit = DEFAULT_LIMIT;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;

        String[] keywords = tokenize(query);
        float[] queryVector = null;
        if (embeddingClient != null && embeddingClient.enabled()) {
            try {
                queryVector = embeddingClient.embed(query);
            } catch (LlmException ignored) {
                queryVector = null;
            }
        }

        List<PluginCard> cards = pluginDao.searchHybrid(
            keywords, queryVector, type, maxPrice, limit);

        ArrayNode out = mapper.createArrayNode();
        for (PluginCard c : cards) {
            ObjectNode n = out.addObject();
            n.put("id", c.getId());
            n.put("type", c.getType());
            n.put("title", c.getTitle());
            n.put("summary", c.getSummary());
            n.put("price", c.getPrice());
            n.put("score", c.getScore());
            ArrayNode tags = n.putArray("tags");
            for (String t : c.getTags()) tags.add(t);
        }
        return out;
    }

    /**
     * Look up a single PUBLIC plugin and return body preview + tags. Throws
     * {@link NotFoundException} when the plugin is missing or non-public, so
     * the agent loop can surface a sensible message to the user.
     */
    public JsonNode getPluginDetail(JsonNode args) {
        if (args == null || !args.has("id") || !args.path("id").isNumber()) {
            throw new NotFoundException("id 인자가 필요합니다");
        }
        long id = args.path("id").asLong();
        Optional<Plugin> opt = pluginDao.findById(id);
        if (!opt.isPresent()) {
            throw new NotFoundException("자산을 찾을 수 없습니다");
        }
        Plugin a = opt.get();
        if (a.getStatus() != PluginStatus.PUBLIC) {
            throw new NotFoundException("자산을 찾을 수 없습니다");
        }
        ObjectNode out = mapper.createObjectNode();
        out.put("id", a.getId());
        out.put("type", a.getType().name());
        out.put("title", a.getTitle());
        out.put("summary", a.getSummary());
        out.put("body_preview", previewBody(a.getBody()));
        if (a.getDemoUrl() != null) out.put("demo_url", a.getDemoUrl());
        if (a.getVideoUrl() != null) out.put("video_url", a.getVideoUrl());
        out.put("price", a.getPrice());
        ArrayNode tagsNode = out.putArray("tags");
        List<Tag> tagList = tagDao.findByPluginId(a.getId());
        for (Tag t : tagList) tagsNode.add(t.getName());
        return out;
    }

    /**
     * Lightweight Korean+English tokenizer: split on whitespace and common
     * punctuation, drop tokens shorter than 2 chars, lowercase ASCII.
     */
    public static String[] tokenize(String text) {
        if (text == null) return new String[0];
        String[] raw = text.split("[\\s,.!?;:()\\[\\]{}<>\"'~`@#$%^&*+=|/\\\\\\-—]+");
        List<String> out = new ArrayList<>();
        for (String t : raw) {
            if (t == null) continue;
            String tt = t.trim();
            if (tt.length() < 2) continue;
            out.add(tt.toLowerCase());
        }
        if (out.isEmpty()) return new String[0];
        return out.toArray(new String[0]);
    }

    private static String previewBody(String body) {
        if (body == null) return "";
        if (body.length() <= BODY_PREVIEW_MAX) return body;
        return body.substring(0, BODY_PREVIEW_MAX);
    }

    /** Convenience for inline empty-map literals. */
    @SuppressWarnings("unused")
    private static Map<String, Object> emptyMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>());
    }
}
