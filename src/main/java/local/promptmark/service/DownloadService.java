package local.promptmark.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.DownloadDao;
import local.promptmark.dao.OrderDao;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.PluginType;
import local.promptmark.dto.LoginUser;
import local.promptmark.web.ForbiddenException;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.Role;

/**
 * Permission check + file resolution for {@code /app/plugin/download}.
 *
 * <p>Free plugins (price 0) are open to any logged-in user; paid plugins require
 * either ownership, an ADMIN session, or a prior PAID order. PROMPT plugins
 * return the body as text; MD plugins are read from {@code uploads/}.
 */
public class DownloadService {

    /** Bundle of bytes + filename + content type returned by {@link #resolveDownload}. */
    public static final class DownloadPayload {
        public final String filename;
        public final String contentType;
        public final byte[] content;

        public DownloadPayload(String filename, String contentType, byte[] content) {
            this.filename = filename;
            this.contentType = contentType;
            this.content = content;
        }
    }

    private final DataSource ds;
    private final PluginDao pluginDao;
    private final OrderDao orderDao;
    private final DownloadDao downloadDao;

    public DownloadService(DataSource ds, PluginDao pluginDao,
                           OrderDao orderDao, DownloadDao downloadDao) {
        this.ds = ds;
        this.pluginDao = pluginDao;
        this.orderDao = orderDao;
        this.downloadDao = downloadDao;
    }

    /**
     * Look up the plugin, check the permission matrix, and read the bytes.
     * Throws {@link ForbiddenException} for paid plugins a user can't access,
     * and {@link NotFoundException} when the plugin is missing or the MD file
     * is gone from disk.
     */
    public DownloadPayload resolveDownload(long pluginId, LoginUser user) {
        if (user == null) throw new ForbiddenException("로그인이 필요합니다");

        Optional<Plugin> opt = pluginDao.findById(pluginId);
        if (!opt.isPresent()) {
            throw new NotFoundException("자산을 찾을 수 없습니다");
        }
        Plugin a = opt.get();

        if (a.getStatus() != PluginStatus.PUBLIC) {
            // Owner / admin can still access for sanity but business-wise hidden
            // plugins are no longer downloadable.
            if (!(user.getRole() == Role.ADMIN || a.getSellerId() == user.getId())) {
                throw new ForbiddenException("판매중인 자산이 아닙니다");
            }
        }

        boolean allowed =
            user.getRole() == Role.ADMIN ||
            a.getSellerId() == user.getId() ||
            a.getPrice() == 0 ||
            orderDao.userHasPurchased(user.getId(), a.getId());
        if (!allowed) {
            throw new ForbiddenException("다운로드 권한이 없습니다");
        }

        if (a.getType() == PluginType.PROMPT) {
            String body = a.getBody() == null ? "" : a.getBody();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            return new DownloadPayload(a.getTitle() + ".txt", "text/plain; charset=UTF-8", bytes);
        }
        // MD
        String key = a.getFileKey();
        if (key == null || key.isEmpty()) {
            throw new NotFoundException("자산 파일이 누락되었습니다");
        }
        Path p = Paths.get(key);
        if (!Files.isRegularFile(p)) {
            throw new NotFoundException("자산 파일이 누락되었습니다");
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(p);
        } catch (IOException ioe) {
            throw new RuntimeException("자산 파일 읽기 실패: " + ioe.getMessage(), ioe);
        }
        String fname = p.getFileName().toString();
        return new DownloadPayload(fname, "text/markdown; charset=UTF-8", bytes);
    }

    /** Insert a downloads row + bump the plugin's download_count in one tx. */
    public void recordDownload(long pluginId, long userId) {
        try (Connection c = ds.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                downloadDao.insert(userId, pluginId, c);
                pluginDao.incrementDownloadCount(pluginId, c);
                c.commit();
            } catch (RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new RuntimeException("recordDownload failed: " + e.getMessage(), e);
        }
    }
}
