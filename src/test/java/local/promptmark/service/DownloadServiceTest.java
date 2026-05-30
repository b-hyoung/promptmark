package local.promptmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import local.promptmark.dao.AssetDao;
import local.promptmark.dao.DownloadDao;
import local.promptmark.dao.OrderDao;
import local.promptmark.dto.Asset;
import local.promptmark.dto.AssetStatus;
import local.promptmark.dto.AssetType;
import local.promptmark.dto.LoginUser;
import local.promptmark.web.ForbiddenException;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.Role;

class DownloadServiceTest {

    private AssetDao assetDao;
    private OrderDao orderDao;
    private DownloadDao downloadDao;
    private DataSource ds;
    private DownloadService svc;

    @BeforeEach
    void setUp() {
        assetDao = Mockito.mock(AssetDao.class);
        orderDao = Mockito.mock(OrderDao.class);
        downloadDao = Mockito.mock(DownloadDao.class);
        ds = Mockito.mock(DataSource.class);
        svc = new DownloadService(ds, assetDao, orderDao, downloadDao);
    }

    private LoginUser user(long id, Role role) {
        return new LoginUser(id, "u" + id + "@b.com", "n" + id, role);
    }

    private Asset prompt(long id, long sellerId, int price, String body) {
        return new Asset(id, sellerId, AssetType.PROMPT, "Prompt " + id, "sum",
            body, null, null, null, price, AssetStatus.PUBLIC, 0, 0,
            Instant.now(), Instant.now());
    }

    private Asset md(long id, long sellerId, int price, String fileKey) {
        return new Asset(id, sellerId, AssetType.MD, "MD " + id, "sum",
            null, fileKey, null, null, price, AssetStatus.PUBLIC, 0, 0,
            Instant.now(), Instant.now());
    }

    // ───── permission matrix ─────────────────────────────────────────

    @Test
    void free_asset_any_user_can_download() {
        Asset a = prompt(1L, 99L, 0, "hello");
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));

        DownloadService.DownloadPayload p = svc.resolveDownload(1L, user(7L, Role.USER));
        assertThat(p.content).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        assertThat(p.contentType).startsWith("text/plain");
        assertThat(p.filename).isEqualTo("Prompt 1.txt");
    }

    @Test
    void owner_can_download_paid_asset() {
        Asset a = prompt(1L, 7L, 5000, "hello");
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));

        DownloadService.DownloadPayload p = svc.resolveDownload(1L, user(7L, Role.SELLER));
        assertThat(p.content).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void purchaser_can_download_paid_asset() {
        Asset a = prompt(1L, 99L, 5000, "hello");
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));
        when(orderDao.userHasPurchased(7L, 1L)).thenReturn(true);

        DownloadService.DownloadPayload p = svc.resolveDownload(1L, user(7L, Role.USER));
        assertThat(p.content).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void random_user_cannot_download_paid_asset() {
        Asset a = prompt(1L, 99L, 5000, "hello");
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));
        when(orderDao.userHasPurchased(7L, 1L)).thenReturn(false);

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> svc.resolveDownload(1L, user(7L, Role.USER)));
    }

    @Test
    void admin_can_download_any_asset() {
        Asset a = prompt(1L, 99L, 5000, "hello");
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));

        DownloadService.DownloadPayload p = svc.resolveDownload(1L, user(2L, Role.ADMIN));
        assertThat(p.content).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void anonymous_user_is_forbidden() {
        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> svc.resolveDownload(1L, null));
    }

    @Test
    void hidden_asset_blocked_for_non_owner_non_admin() {
        Asset hidden = new Asset(1L, 99L, AssetType.PROMPT, "t", "s",
            "body", null, null, null, 0, AssetStatus.HIDDEN, 0, 0,
            Instant.now(), Instant.now());
        when(assetDao.findById(1L)).thenReturn(Optional.of(hidden));

        assertThatExceptionOfType(ForbiddenException.class)
            .isThrownBy(() -> svc.resolveDownload(1L, user(7L, Role.USER)));
    }

    @Test
    void missing_asset_throws_not_found() {
        when(assetDao.findById(1L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> svc.resolveDownload(1L, user(7L, Role.USER)));
    }

    // ───── content / file resolution ─────────────────────────────────

    @Test
    void prompt_bytes_come_from_body_field() {
        Asset a = prompt(1L, 99L, 0, "안녕하세요");
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));

        DownloadService.DownloadPayload p = svc.resolveDownload(1L, user(7L, Role.USER));
        assertThat(new String(p.content, StandardCharsets.UTF_8)).isEqualTo("안녕하세요");
    }

    @Test
    void md_reads_file_from_disk(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("note.md");
        Files.write(file, "# Hello".getBytes(StandardCharsets.UTF_8));

        Asset a = md(1L, 99L, 0, file.toString());
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));

        DownloadService.DownloadPayload p = svc.resolveDownload(1L, user(7L, Role.USER));
        assertThat(new String(p.content, StandardCharsets.UTF_8)).isEqualTo("# Hello");
        assertThat(p.filename).isEqualTo("note.md");
        assertThat(p.contentType).startsWith("text/markdown");
    }

    @Test
    void md_missing_file_throws_not_found(@TempDir Path tmp) {
        Asset a = md(1L, 99L, 0, tmp.resolve("missing.md").toString());
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));

        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> svc.resolveDownload(1L, user(7L, Role.USER)));
    }

    @Test
    void md_null_file_key_throws_not_found() {
        Asset a = md(1L, 99L, 0, null);
        when(assetDao.findById(1L)).thenReturn(Optional.of(a));

        assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> svc.resolveDownload(1L, user(7L, Role.USER)));
    }
}
