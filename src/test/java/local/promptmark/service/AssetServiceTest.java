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

import local.promptmark.dao.AssetDao;
import local.promptmark.dao.TagDao;
import local.promptmark.dto.Asset;
import local.promptmark.dto.AssetStatus;
import local.promptmark.dto.AssetType;
import local.promptmark.dto.LoginUser;
import local.promptmark.web.ForbiddenException;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.Role;
import local.promptmark.web.ValidationException;

class AssetServiceTest {

    private AssetDao assetDao;
    private TagDao tagDao;
    private DataSource ds;
    private Connection conn;
    private AssetService svc;

    @BeforeEach
    void setUp() throws Exception {
        assetDao = Mockito.mock(AssetDao.class);
        tagDao = Mockito.mock(TagDao.class);
        ds = Mockito.mock(DataSource.class);
        conn = Mockito.mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.getAutoCommit()).thenReturn(true);
        svc = new AssetService(ds, assetDao, tagDao);
    }

    private LoginUser seller(long id) {
        return new LoginUser(id, "s@b.com", "seller" + id, Role.SELLER);
    }

    private LoginUser admin() {
        return new LoginUser(99L, "a@b.com", "admin", Role.ADMIN);
    }

    private Asset assetOf(long id, long sellerId, AssetType type, AssetStatus status, int price) {
        return new Asset(id, sellerId, type, "Sample Title", "Sample Summary",
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
            svc.createAsset(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("title");
        verify(assetDao, never()).insert(any());
    }

    @Test
    void create_rejects_short_summary() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("summary", "x");
        ValidationException ex = catchValidation(() ->
            svc.createAsset(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("summary");
    }

    @Test
    void create_rejects_negative_price() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("price", "-5");
        ValidationException ex = catchValidation(() ->
            svc.createAsset(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("price");
    }

    @Test
    void create_rejects_non_numeric_price() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("price", "abc");
        ValidationException ex = catchValidation(() ->
            svc.createAsset(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("price");
    }

    @Test
    void create_rejects_unknown_type() {
        Map<String, String> form = baseForm("VIDEO");
        ValidationException ex = catchValidation(() ->
            svc.createAsset(seller(1L), form, null, null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("type");
    }

    @Test
    void create_prompt_requires_body() {
        Map<String, String> form = baseForm("PROMPT");
        ValidationException ex = catchValidation(() ->
            svc.createAsset(seller(1L), form, "   ", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("body");
    }

    @Test
    void create_md_requires_file_key() {
        Map<String, String> form = baseForm("MD");
        ValidationException ex = catchValidation(() ->
            svc.createAsset(seller(1L), form, null, null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("file");
    }

    @Test
    void create_rejects_malformed_demo_url() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("demo_url", "javascript:alert(1)");
        ValidationException ex = catchValidation(() ->
            svc.createAsset(seller(1L), form, "body", null, Collections.emptyList()));
        assertThat(ex.fieldErrors()).containsKey("demo_url");
    }

    @Test
    void create_happy_path_inserts_and_replaces_tags() {
        Map<String, String> form = baseForm("PROMPT");
        form.put("price", "1500");
        form.put("demo_url", "https://example.com/demo");
        when(assetDao.insert(any(Asset.class))).thenReturn(42L);

        long id = svc.createAsset(seller(7L), form, "Hello body",
            null, Arrays.asList("AI", "기획"));

        assertThat(id).isEqualTo(42L);
        verify(assetDao).insert(any(Asset.class));
        verify(tagDao).replaceForAsset(eq(42L), eq(Arrays.asList("AI", "기획")), eq(conn));
    }

    @Test
    void create_md_happy_path_passes_file_key_not_body() {
        Map<String, String> form = baseForm("MD");
        when(assetDao.insert(any(Asset.class))).thenReturn(7L);

        long id = svc.createAsset(seller(1L), form, null, "uploads/assets/2026/01/01/x.md",
            Collections.emptyList());

        assertThat(id).isEqualTo(7L);
        verify(assetDao).insert(any(Asset.class));
    }

    @Test
    void create_calls_embedding_when_enabled() {
        Map<String, String> form = baseForm("PROMPT");
        when(assetDao.insert(any(Asset.class))).thenReturn(99L);
        local.promptmark.service.llm.EmbeddingClient embed =
            Mockito.mock(local.promptmark.service.llm.EmbeddingClient.class);
        when(embed.enabled()).thenReturn(true);
        when(embed.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});

        AssetService withEmbed = new AssetService(ds, assetDao, tagDao, embed);
        withEmbed.createAsset(seller(1L), form, "hello body", null,
            Collections.emptyList());

        verify(embed).embed(anyString());
        verify(assetDao).updateEmbedding(eq(99L), any(float[].class));
    }

    @Test
    void create_swallows_embedding_failure() {
        Map<String, String> form = baseForm("PROMPT");
        when(assetDao.insert(any(Asset.class))).thenReturn(100L);
        local.promptmark.service.llm.EmbeddingClient embed =
            Mockito.mock(local.promptmark.service.llm.EmbeddingClient.class);
        when(embed.enabled()).thenReturn(true);
        when(embed.embed(anyString()))
            .thenThrow(new local.promptmark.service.llm.LlmException("network down"));

        AssetService withEmbed = new AssetService(ds, assetDao, tagDao, embed);
        long id = withEmbed.createAsset(seller(1L), form, "hello body", null,
            Collections.emptyList());

        assertThat(id).isEqualTo(100L);
        verify(assetDao, never()).updateEmbedding(anyLong(), any(float[].class));
    }

    @Test
    void create_skips_embedding_when_disabled() {
        Map<String, String> form = baseForm("PROMPT");
        when(assetDao.insert(any(Asset.class))).thenReturn(11L);
        local.promptmark.service.llm.EmbeddingClient embed =
            Mockito.mock(local.promptmark.service.llm.EmbeddingClient.class);
        when(embed.enabled()).thenReturn(false);

        AssetService withEmbed = new AssetService(ds, assetDao, tagDao, embed);
        withEmbed.createAsset(seller(1L), form, "hello body", null,
            Collections.emptyList());

        verify(embed, never()).embed(anyString());
        verify(assetDao, never()).updateEmbedding(anyLong(), any(float[].class));
    }

    // ───── getDetailAndIncrementView ─────────────────────────────────

    @Test
    void getDetail_throws_not_found_when_missing() {
        when(assetDao.findById(1L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> svc.getDetailAndIncrementView(1L));
        verify(assetDao, never()).incrementViewCount(anyLong());
    }

    @Test
    void getDetail_returns_asset_and_bumps_view() {
        Asset a = assetOf(2L, 9L, AssetType.PROMPT, AssetStatus.PUBLIC, 0);
        when(assetDao.findById(2L)).thenReturn(Optional.of(a));
        Asset got = svc.getDetailAndIncrementView(2L);
        assertThat(got.getId()).isEqualTo(2L);
        verify(assetDao).incrementViewCount(2L);
    }

    // ───── getEditable / ownership ───────────────────────────────────

    @Test
    void getEditable_owner_can_load() {
        Asset a = assetOf(5L, 3L, AssetType.PROMPT, AssetStatus.PUBLIC, 0);
        when(assetDao.findByIdForOwner(5L, 3L)).thenReturn(Optional.of(a));
        Asset got = svc.getEditable(5L, seller(3L));
        assertThat(got.getId()).isEqualTo(5L);
    }

    @Test
    void getEditable_non_owner_throws_forbidden() {
        when(assetDao.findByIdForOwner(5L, 8L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> svc.getEditable(5L, seller(8L)));
    }

    @Test
    void getEditable_admin_bypasses_owner_check() {
        Asset a = assetOf(5L, 3L, AssetType.PROMPT, AssetStatus.PUBLIC, 0);
        when(assetDao.findById(5L)).thenReturn(Optional.of(a));
        Asset got = svc.getEditable(5L, admin());
        assertThat(got.getSellerId()).isEqualTo(3L);
    }

    @Test
    void getEditable_admin_404_when_missing() {
        when(assetDao.findById(5L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> svc.getEditable(5L, admin()));
    }

    // ───── deleteAsset ────────────────────────────────────────────────

    @Test
    void delete_owner_can_soft_delete() {
        Asset a = assetOf(5L, 3L, AssetType.PROMPT, AssetStatus.PUBLIC, 0);
        when(assetDao.findByIdForOwner(5L, 3L)).thenReturn(Optional.of(a));
        svc.deleteAsset(5L, seller(3L));
        verify(assetDao).softDelete(5L);
    }

    @Test
    void delete_non_owner_forbidden() {
        when(assetDao.findByIdForOwner(5L, 8L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> svc.deleteAsset(5L, seller(8L)));
        verify(assetDao, never()).softDelete(anyLong());
    }

    // ───── search clamps ─────────────────────────────────────────────

    @Test
    void search_clamps_limit_and_offset() {
        when(assetDao.search(anyString(), any(), anyString(), anyString(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        svc.search("foo", AssetType.PROMPT, "AI", "recent", -10, 999);
        verify(assetDao).search("foo", AssetType.PROMPT, "AI", "recent", 0, 50);
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
