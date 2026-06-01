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

import local.promptmark.dao.BundleDao;
import local.promptmark.dao.PluginDao;
import local.promptmark.dao.TagDao;
import local.promptmark.dao.UserDao;
import local.promptmark.dto.Bundle;
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
    public static final String TOOL_SEARCH_BUNDLES = "search_bundles";
    public static final String TOOL_GET_BUNDLE_DETAIL = "get_bundle_detail";

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final int BODY_PREVIEW_MAX = 500;

    private final PluginDao pluginDao;
    private final BundleDao bundleDao;     // nullable for backwards-compat constructor
    private final TagDao tagDao;
    @SuppressWarnings("unused")
    private final UserDao userDao;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Backwards-compat constructor — bundle tools disabled. */
    public Tools(PluginDao pluginDao, TagDao tagDao, UserDao userDao,
                 EmbeddingClient embeddingClient) {
        this(pluginDao, null, tagDao, userDao, embeddingClient);
    }

    public Tools(PluginDao pluginDao, BundleDao bundleDao, TagDao tagDao, UserDao userDao,
                 EmbeddingClient embeddingClient) {
        this.pluginDao = pluginDao;
        this.bundleDao = bundleDao;
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
            JsonNode bundleSearchSchema = mapper.readTree("{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                +   "\"query\":{\"type\":\"string\"},"
                +   "\"limit\":{\"type\":\"integer\",\"default\":5,\"maximum\":20}"
                + "},"
                + "\"required\":[\"query\"]"
                + "}");
            JsonNode bundleDetailSchema = mapper.readTree("{"
                + "\"type\":\"object\","
                + "\"properties\":{\"id\":{\"type\":\"integer\"}},"
                + "\"required\":[\"id\"]"
                + "}");

            List<ToolDef> defs = new ArrayList<>();
            // Bundle tools listed first so the model prefers them per system prompt
            if (bundleDao != null) {
                defs.add(new ToolDef(TOOL_SEARCH_BUNDLES,
                    "큐레이션 셋트 검색 (이름·태그라인·스토리 매칭). 먼저 시도하세요.", bundleSearchSchema));
                defs.add(new ToolDef(TOOL_GET_BUNDLE_DETAIL,
                    "특정 셋트의 포함 플러그인·가격·스토리 조회.", bundleDetailSchema));
            }
            defs.add(new ToolDef(TOOL_SEARCH_PLUGINS,
                "개별 플러그인 검색 (키워드+벡터 하이브리드). 셋트로 안 맞을 때 사용.", searchSchema));
            defs.add(new ToolDef(TOOL_GET_PLUGIN_DETAIL,
                "특정 플러그인의 본문 미리보기·태그·데모 URL 조회.", detailSchema));
            return defs;
        } catch (Exception e) {
            throw new LlmException("failed to build tool definitions", e);
        }
    }

    /** Routes a model-issued tool call to the correct method. */
    public JsonNode dispatch(String name, JsonNode args) {
        if (TOOL_SEARCH_PLUGINS.equals(name)) return searchPlugins(args);
        if (TOOL_GET_PLUGIN_DETAIL.equals(name)) return getPluginDetail(args);
        if (TOOL_SEARCH_BUNDLES.equals(name)) return searchBundles(args);
        if (TOOL_GET_BUNDLE_DETAIL.equals(name)) return getBundleDetail(args);
        throw new LlmException("unknown tool: " + name);
    }

    public JsonNode searchBundles(JsonNode args) {
        if (bundleDao == null) return mapper.createArrayNode();
        if (args == null || args.isNull()) args = mapper.createObjectNode();
        String query = args.path("query").asText("");
        int limit = DEFAULT_LIMIT;
        if (args.has("limit") && args.path("limit").isNumber()) {
            limit = Math.max(1, Math.min(MAX_LIMIT, args.path("limit").asInt(DEFAULT_LIMIT)));
        }

        // Simple keyword-overlap filter on top of recent list (small catalogue).
        // For larger catalogues this should use an indexed query or pgvector.
        List<Bundle> candidates = bundleDao.list("recent", 0, 50);
        String q = (query == null) ? "" : query.toLowerCase().trim();
        ArrayNode out = mapper.createArrayNode();
        int kept = 0;
        for (Bundle b : candidates) {
            boolean match = q.isEmpty()
                || (b.getName() != null && b.getName().toLowerCase().contains(q))
                || (b.getTagline() != null && b.getTagline().toLowerCase().contains(q))
                || (b.getStory() != null && b.getStory().toLowerCase().contains(q));
            if (!match) continue;
            ObjectNode n = out.addObject();
            n.put("id", b.getId());
            n.put("slug", b.getSlug());
            n.put("name", b.getName());
            n.put("tagline", b.getTagline());
            n.put("price", b.getPrice());
            if (++kept >= limit) break;
        }
        return out;
    }

    public JsonNode getBundleDetail(JsonNode args) {
        if (bundleDao == null) throw new NotFoundException("셋트를 찾을 수 없습니다");
        if (args == null || !args.has("id") || !args.path("id").isNumber()) {
            throw new NotFoundException("id 인자가 필요합니다");
        }
        long id = args.path("id").asLong();
        Optional<Bundle> opt = bundleDao.findByIdWithPlugins(id);
        if (!opt.isPresent()) throw new NotFoundException("셋트를 찾을 수 없습니다");
        Bundle b = opt.get();
        ObjectNode out = mapper.createObjectNode();
        out.put("id", b.getId());
        out.put("slug", b.getSlug());
        out.put("name", b.getName());
        out.put("tagline", b.getTagline());
        out.put("story", b.getStory());
        out.put("price", b.getPrice());
        ArrayNode pl = out.putArray("plugins");
        for (Plugin p : b.getPlugins()) {
            ObjectNode pn = pl.addObject();
            pn.put("id", p.getId());
            pn.put("title", p.getTitle());
            pn.put("summary", p.getSummary());
            pn.put("price", p.getPrice());
        }
        return out;
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
