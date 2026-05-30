package local.promptmark.service.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import local.promptmark.dao.AssetDao;
import local.promptmark.dao.TagDao;
import local.promptmark.dao.UserDao;
import local.promptmark.dto.Asset;
import local.promptmark.dto.AssetStatus;
import local.promptmark.dto.AssetType;
import local.promptmark.dto.Tag;
import local.promptmark.web.NotFoundException;

class ToolsTest {

    private AssetDao assetDao;
    private TagDao tagDao;
    private UserDao userDao;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        assetDao = Mockito.mock(AssetDao.class);
        tagDao = Mockito.mock(TagDao.class);
        userDao = Mockito.mock(UserDao.class);
    }

    @Test
    void definitions_contain_both_tools_with_correct_names() {
        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        List<ToolDef> defs = tools.definitions();
        assertThat(defs).hasSize(2);
        assertThat(defs.get(0).name()).isEqualTo("search_assets");
        assertThat(defs.get(1).name()).isEqualTo("get_asset_detail");
        assertThat(defs.get(0).parametersSchema().path("required").get(0).asText())
            .isEqualTo("query");
    }

    @Test
    void search_assets_with_embedding_disabled_passes_null_vector() throws Exception {
        AssetCard card = new AssetCard(7, "PROMPT", "Hi", "Sum", 0, 0.7,
            Arrays.asList("AI"), null);
        when(assetDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.singletonList(card));

        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        JsonNode result = tools.searchAssets(mapper.readTree("{\"query\":\"hello world\"}"));

        ArgumentCaptor<float[]> vecCap = ArgumentCaptor.forClass(float[].class);
        verify(assetDao).searchHybrid(any(), vecCap.capture(), any(), any(), Mockito.anyInt());
        assertThat(vecCap.getValue()).isNull();

        assertThat(result.isArray()).isTrue();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).path("id").asLong()).isEqualTo(7L);
        assertThat(result.get(0).path("type").asText()).isEqualTo("PROMPT");
    }

    @Test
    void search_assets_with_embedding_enabled_passes_vector() throws Exception {
        when(assetDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.emptyList());

        Tools tools = new Tools(assetDao, tagDao, userDao, new FakeEmbeddingClient());
        tools.searchAssets(mapper.readTree("{\"query\":\"hello\"}"));

        ArgumentCaptor<float[]> vecCap = ArgumentCaptor.forClass(float[].class);
        verify(assetDao).searchHybrid(any(), vecCap.capture(), any(), any(), Mockito.anyInt());
        assertThat(vecCap.getValue()).isNotNull();
        assertThat(vecCap.getValue().length).isEqualTo(FakeEmbeddingClient.DIM);
    }

    @Test
    void search_assets_limit_clamped_to_max_20() throws Exception {
        when(assetDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.emptyList());

        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        tools.searchAssets(mapper.readTree("{\"query\":\"x\",\"limit\":999}"));

        verify(assetDao).searchHybrid(any(), any(), any(), any(), eq(20));
    }

    @Test
    void search_assets_empty_query_returns_empty_array() throws Exception {
        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        JsonNode r = tools.searchAssets(mapper.readTree("{\"query\":\"\"}"));
        assertThat(r.isArray()).isTrue();
        assertThat(r.size()).isEqualTo(0);
        verify(assetDao, never()).searchHybrid(any(), any(), any(), any(), Mockito.anyInt());
    }

    @Test
    void search_assets_parses_type_and_max_price() throws Exception {
        when(assetDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.emptyList());
        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        tools.searchAssets(mapper.readTree(
            "{\"query\":\"x\",\"type\":\"MD\",\"max_price\":1500}"));
        verify(assetDao).searchHybrid(any(), any(), eq(AssetType.MD), eq(1500),
            Mockito.anyInt());
    }

    @Test
    void get_asset_detail_returns_expected_fields() throws Exception {
        Asset a = new Asset(42, 1, AssetType.PROMPT, "T", "S",
            "body content", null, "https://demo", "https://yt",
            1000, AssetStatus.PUBLIC, 0, 0, Instant.now(), Instant.now());
        when(assetDao.findById(42L)).thenReturn(Optional.of(a));
        when(tagDao.findByAssetId(42L)).thenReturn(Arrays.asList(
            new Tag(1, "AI"), new Tag(2, "기획")));

        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        JsonNode r = tools.getAssetDetail(mapper.readTree("{\"id\":42}"));

        assertThat(r.path("id").asLong()).isEqualTo(42L);
        assertThat(r.path("type").asText()).isEqualTo("PROMPT");
        assertThat(r.path("title").asText()).isEqualTo("T");
        assertThat(r.path("summary").asText()).isEqualTo("S");
        assertThat(r.path("body_preview").asText()).isEqualTo("body content");
        assertThat(r.path("demo_url").asText()).isEqualTo("https://demo");
        assertThat(r.path("video_url").asText()).isEqualTo("https://yt");
        assertThat(r.path("price").asInt()).isEqualTo(1000);
        assertThat(r.path("tags").size()).isEqualTo(2);
    }

    @Test
    void get_asset_detail_body_truncated_to_500_chars() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 700; i++) sb.append('a');
        Asset a = new Asset(1, 1, AssetType.PROMPT, "T", "S",
            sb.toString(), null, null, null,
            0, AssetStatus.PUBLIC, 0, 0, Instant.now(), Instant.now());
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));
        when(tagDao.findByAssetId(1L)).thenReturn(Collections.emptyList());

        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        JsonNode r = tools.getAssetDetail(mapper.readTree("{\"id\":1}"));
        assertThat(r.path("body_preview").asText().length()).isEqualTo(500);
    }

    @Test
    void get_asset_detail_throws_not_found_when_missing() throws Exception {
        when(assetDao.findById(99L)).thenReturn(Optional.empty());
        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> tools.getAssetDetail(mapper.readTree("{\"id\":99}")));
    }

    @Test
    void get_asset_detail_throws_not_found_when_hidden() throws Exception {
        Asset a = new Asset(1, 1, AssetType.PROMPT, "T", "S", "body",
            null, null, null, 0, AssetStatus.HIDDEN, 0, 0, Instant.now(), Instant.now());
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));
        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> tools.getAssetDetail(mapper.readTree("{\"id\":1}")));
    }

    @Test
    void dispatch_routes_to_search_assets() throws Exception {
        when(assetDao.searchHybrid(any(), any(), any(), any(), Mockito.anyInt()))
            .thenReturn(Collections.emptyList());
        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        JsonNode r = tools.dispatch("search_assets", mapper.readTree("{\"query\":\"x\"}"));
        assertThat(r.isArray()).isTrue();
    }

    @Test
    void dispatch_throws_for_unknown_tool() throws Exception {
        Tools tools = new Tools(assetDao, tagDao, userDao, new DisabledEmbeddingClient());
        assertThatExceptionOfType(LlmException.class)
            .isThrownBy(() -> tools.dispatch("bogus", mapper.createObjectNode()));
    }

    @Test
    void tokenize_keeps_korean_and_english_words() {
        String[] t = Tools.tokenize("자기소개서 첨삭 React Hooks");
        assertThat(t).contains("자기소개서", "첨삭", "react", "hooks");
    }

    @Test
    void tokenize_drops_single_char_tokens() {
        String[] t = Tools.tokenize("a hello b");
        assertThat(t).containsExactly("hello");
    }
}
