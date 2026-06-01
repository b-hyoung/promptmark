package local.promptmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.TagDao;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.PluginType;
import local.promptmark.dto.LoginUser;
import local.promptmark.web.ForbiddenException;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.Role;
import local.promptmark.web.ValidationException;

class PluginServiceTest {

    private PluginDao pluginDao;
    private TagDao tagDao;
    private DataSource ds;
    private Connection conn;
    private PluginService svc;

    @BeforeEach
    void setUp() throws Exception {
        pluginDao = Mockito.mock(PluginDao.class);
        tagDao = Mockito.mock(TagDao.class);
        ds = Mockito.mock(DataSource.class);
        conn = Mockito.mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.getAutoCommit()).thenReturn(true);
        svc = new PluginService(ds, pluginDao, tagDao);
    }

    private LoginUser seller(long id) {
        return new LoginUser(id, "s@b.com", "seller" + id, Role.SELLER);
    }

    private LoginUser admin() {
        return new LoginUser(99L, "a@b.com", "admin", Role.ADMIN);
    }

    private Plugin pluginOf(long id, long sellerId, PluginType type, PluginStatus status, int price) {
        return new Plugin(id, sellerId, type, "Sample Title", "Sample Summary",
            "Body", null, null, null, price, status, 0, 0,
            Instant.now(), Instant.now());
    }

    private static Map<String, String> baseForm(String type) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("title", "Hello title");
        m.put("summary", "Hello summary");
        m.put("price", "0");
        return m;
    }

    // ───── create — validation ───────────────────────────────────────

    @Test
    void create_rejects_short_title() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("title", "x");
        ValidationException ex = catchValidation(() ->
            svc.createPlugin(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("title");
        verify(pluginDao, never()).insert(any());
    }

    @Test
    void create_rejects_short_summary() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("summary", "x");
        ValidationException ex = catchValidation(() ->
            svc.createPlugin(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("summary");
    }

    @Test
    void create_rejects_negative_price() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("price", "-5");
        ValidationException ex = catchValidation(() ->
            svc.createPlugin(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("price");
    }

    @Test
    void create_rejects_non_numeric_price() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("price", "abc");
        ValidationException ex = catchValidation(() ->
            svc.createPlugin(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("price");
    }

    @Test
    void create_rejects_unknown_type() {
        Map<String, String> form = baseForm("VIDEO");
        ValidationException ex = catchValidation(() ->
            svc.createPlugin(seller(1L), form, null, null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("type");
    }

    @Test
    void create_prompt_requires_body() {
        Map<String, String> form = baseForm("PROMPT");
        ValidationException ex = catchValidation(() ->
            svc.createPlugin(seller(1L), form, "   ", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("body");
    }

    @Test
    void create_md_requires_file_key() {
        Map<String, String> form = baseForm("MD");
        ValidationException ex = catchValidation(() ->
            svc.createPlugin(seller(1L), form, null, null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("file");
    }

    @Test
    void create_rejects_malformed_demo_url() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("demo_url", "javascript:alert(1)");
        ValidationException ex = catchValidation(() ->
            svc.createPlugin(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("demo_url");
    }

    @Test
    void create_happy_path_inserts_and_replaces_tags() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("price", "1500");
        form.put("demo_url", "https://example.com/demo");
        when(pluginDao.insert(any(Plugin.class))).thenReturn(42L);

        long id = svc.createPlugin(seller(7L), form, "Hello body",
            null, Arrays.asList("AI", "기획"));

        assertThat(id).isEqualTo(42L);
        verify(pluginDao).insert(any(Plugin.class));
        verify(tagDao).replaceForPlugin(eq(42L), eq(Arrays.asList("AI", "기획")), eq(conn));
    }

    @Test
    void create_md_happy_path_passes_file_key_not_body() {
        Map<String, String> form = baseForm("MD");
        when(pluginDao.insert(any(Plugin.class))).thenReturn(7L);

        long id = svc.createPlugin(seller(1L), form, null, "uploads/plugins/2026/01/01/x.md",
            Collections.emptyList());

        assertThat(id).isEqualTo(7L);
        verify(pluginDao).insert(any(Plugin.class));
    }

    @Test
    void create_calls_embedding_when_enabled() {
        Map<String, String> form = baseForm("PROMPT");
        when(pluginDao.insert(any(Plugin.class))).thenReturn(99L);
        local.promptmark.service.llm.EmbeddingClient embed =
            Mockito.mock(local.promptmark.service.llm.EmbeddingClient.class);
        when(embed.enabled()).thenReturn(true);
        when(embed.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        PluginService withEmbed = new PluginService(ds, pluginDao, tagDao, embed);
        withEmbed.createPlugin(seller(1L), form, "hello body", null,
            Collections.emptyList());

        verify(embed).embed(anyString());
        verify(pluginDao).updateEmbedding(eq(99L), any(float[].class));
    }

    @Test
    void create_swallows_embedding_failure() {
        Map<String, String> form = baseForm("PROMPT");
        when(pluginDao.insert(any(Plugin.class))).thenReturn(100L);
        local.promptmark.service.llm.EmbeddingClient embed =
            Mockito.mock(local.promptmark.service.llm.EmbeddingClient.class);
        when(embed.enabled()).thenReturn(true);
        when(embed.embed(anyString()))
            .thenThrow(new local.promptmark.service.llm.LlmException("network down"));

        PluginService withEmbed = new PluginService(ds, pluginDao, tagDao, embed);
        long id = withEmbed.createPlugin(seller(1L), form, "hello body", null,
            Collections.emptyList());

        assertThat(id).isEqualTo(100L);
        verify(pluginDao, never()).updateEmbedding(anyLong(), any(float[].class));
    }

    @Test
    void create_skips_embedding_when_disabled() {
        Map<String, String> form = baseForm("PROMPT");
        when(pluginDao.insert(any(Plugin.class))).thenReturn(11L);
        local.promptmark.service.llm.EmbeddingClient embed =
            Mockito.mock(local.promptmark.service.llm.EmbeddingClient.class);
        when(embed.enabled()).thenReturn(false);

        PluginService withEmbed = new PluginService(ds, pluginDao, tagDao, embed);
        withEmbed.createPlugin(seller(1L), form, "hello body", null,
            Collections.emptyList());

        verify(embed, never()).embed(anyString());
        verify(pluginDao, never()).updateEmbedding(anyLong(), any(float[].class));
    }

    // ───── getDetailAndIncrementView ─────────────────────────────────

    @Test
    void getDetail_throws_not_found_when_missing() {
        when(pluginDao.findById(1L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> svc.getDetailAndIncrementView(1L));
        verify(pluginDao, never()).incrementViewCount(anyLong());
    }

    @Test
    void getDetail_returns_plugin_and_bumps_view() {
        Plugin a = pluginOf(2L, 9L, PluginType.PROMPT, PluginStatus.PUBLIC, 0);
        when(pluginDao.findById(2L)).thenReturn(Optional.of(a));
        Plugin got = svc.getDetailAndIncrementView(2L);
        assertThat(got.getId()).isEqualTo(2L);
        verify(pluginDao).incrementViewCount(2L);
    }

    // ───── getEditable / ownership ───────────────────────────────────

    @Test
    void getEditable_owner_can_load() {
        Plugin a = pluginOf(5L, 3L, PluginType.PROMPT, PluginStatus.PUBLIC, 0);
        when(pluginDao.findByIdForOwner(5L, 3L)).thenReturn(Optional.of(a));
        Plugin got = svc.getEditable(5L, seller(3L));
        assertThat(got.getId()).isEqualTo(5L);
    }

    @Test
    void getEditable_non_owner_throws_forbidden() {
        when(pluginDao.findByIdForOwner(5L, 8L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> svc.getEditable(5L, seller(8L)));
    }

    @Test
    void getEditable_admin_bypasses_owner_check() {
        Plugin a = pluginOf(5L, 3L, PluginType.PROMPT, PluginStatus.PUBLIC, 0);
        when(pluginDao.findById(5L)).thenReturn(Optional.of(a));
        Plugin got = svc.getEditable(5L, admin());
        assertThat(got.getSellerId()).isEqualTo(3L);
    }

    @Test
    void getEditable_admin_404_when_missing() {
        when(pluginDao.findById(5L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> svc.getEditable(5L, admin()));
    }

    // ───── deletePlugin ────────────────────────────────────────────────

    @Test
    void delete_owner_can_soft_delete() {
        Plugin a = pluginOf(5L, 3L, PluginType.PROMPT, PluginStatus.PUBLIC, 0);
        when(pluginDao.findByIdForOwner(5L, 3L)).thenReturn(Optional.of(a));
        svc.deletePlugin(5L, seller(3L));
        verify(pluginDao).softDelete(5L);
    }

    @Test
    void delete_non_owner_forbidden() {
        when(pluginDao.findByIdForOwner(5L, 8L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> svc.deletePlugin(5L, seller(8L)));
        verify(pluginDao, never()).softDelete(anyLong());
    }

    // ───── search clamps ─────────────────────────────────────────────

    @Test
    void search_clamps_limit_and_offset() {
        when(pluginDao.search(anyString(), any(), anyString(), anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        svc.search("foo", PluginType.PROMPT, "AI", "recent", -10, 999);
        verify(pluginDao).search("foo", PluginType.PROMPT, "AI", "recent", 0, 50);
    }

    // ───── helpers ────────────────────────────────────────────────────

    private static ValidationException catchValidation(Runnable r) {
        try {
            r.run();
            throw new AssertionError("Expected ValidationException");
        } catch (ValidationException e) {
            return e;
        }
    }
}
