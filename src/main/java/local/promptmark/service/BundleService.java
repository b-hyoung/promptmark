package local.promptmark.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import local.promptmark.dao.BundleDao;
import local.promptmark.dto.Bundle;
import local.promptmark.dto.BundleStatus;
import local.promptmark.dto.LoginUser;
import local.promptmark.service.llm.DisabledEmbeddingClient;
import local.promptmark.service.llm.EmbeddingClient;
import local.promptmark.service.llm.LlmException;
import local.promptmark.web.ForbiddenException;
import local.promptmark.web.Role;
import local.promptmark.web.ValidationException;

/**
 * Bundle CRUD. Admin-only. createBundle/updateBundle run in a transaction so
 * the row and its plugin mapping commit together. Embedding is best-effort.
 */
public class BundleService {

    private static final Logger log = LoggerFactory.getLogger(BundleService.class);

    private static final int NAME_MIN = 2, NAME_MAX = 100;
    private static final int SLUG_MAX = 40;
    private static final int TAGLINE_MAX = 200;
    private static final Pattern SLUG_RE = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,38}[a-z0-9])?$");

    private final DataSource ds;
    private final BundleDao bundleDao;
    private final EmbeddingClient embeddingClient;

    public BundleService(DataSource ds, BundleDao bundleDao) {
        this(ds, bundleDao, new DisabledEmbeddingClient());
    }

    public BundleService(DataSource ds, BundleDao bundleDao, EmbeddingClient embeddingClient) {
        this.ds = ds;
        this.bundleDao = bundleDao;
        this.embeddingClient = (embeddingClient == null) ? new DisabledEmbeddingClient() : embeddingClient;
    }

    public long createBundle(LoginUser admin, Map<String, String> form, List<Long> pluginIds) {
        ensureAdmin(admin);
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) throw new ValidationException("입력값을 확인해주세요", errors);

        String slug = form.get("slug").trim();
        if (bundleDao.findBySlug(slug).isPresent()) {
            errors.put("slug", "이미 사용 중인 slug 입니다");
            throw new ValidationException("입력값을 확인해주세요", errors);
        }

        Bundle draft = new Bundle(0L, admin.getId(), slug,
            form.get("name").trim(),
            trimOrNull(form.get("tagline")),
            trimOrNull(form.get("story")),
            parsePrice(form.get("price")),
            trimOrNull(form.get("thumbnail")),
            BundleStatus.PUBLIC, 0, null, null);

        long newId;
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                newId = bundleDao.insert(c, draft);
                bundleDao.replacePlugins(c, newId, pluginIds);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("createBundle failed: " + e.getMessage(), e);
        }

        // best-effort embedding
        tryEmbed(newId, draft.getName(), draft.getTagline(), draft.getStory());
        return newId;
    }

    public void updateBundle(LoginUser admin, long id, Map<String, String> form, List<Long> pluginIds) {
        ensureAdmin(admin);
        Optional<Bundle> existing = bundleDao.findById(id);
        if (existing.isEmpty()) throw new ValidationException("존재하지 않는 셋트", Map.of());

        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) throw new ValidationException("입력값을 확인해주세요", errors);

        String name    = form.get("name").trim();
        String tagline = trimOrNull(form.get("tagline"));
        String story   = trimOrNull(form.get("story"));
        int price      = parsePrice(form.get("price"));
        String thumb   = trimOrNull(form.get("thumbnail"));

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                bundleDao.update(c, id, name, tagline, story, price, thumb);
                bundleDao.replacePlugins(c, id, pluginIds);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("updateBundle failed: " + e.getMessage(), e);
        }

        tryEmbed(id, name, tagline, story);
    }

    public void deleteBundle(LoginUser admin, long id) {
        ensureAdmin(admin);
        bundleDao.softDelete(id);
    }

    public Optional<Bundle> findByIdWithPlugins(long id) {
        return bundleDao.findByIdWithPlugins(id);
    }

    public List<Bundle> listPublic(String sort, int offset, int limit) {
        return bundleDao.list(sort, offset, limit);
    }

    public int countPublic() {
        return bundleDao.countPublic();
    }

    private void tryEmbed(long id, String name, String tagline, String story) {
        if (embeddingClient == null || !embeddingClient.enabled()) return;
        StringBuilder sb = new StringBuilder();
        sb.append(name == null ? "" : name);
        if (tagline != null) sb.append('\n').append(tagline);
        if (story   != null) sb.append('\n').append(story.length() > 500 ? story.substring(0, 500) : story);
        try {
            float[] v = embeddingClient.embed(sb.toString());
            if (v != null && v.length > 0) bundleDao.updateEmbedding(id, v);
        } catch (LlmException e) {
            log.warn("bundle embedding failed for {}: {}", id, e.getMessage());
        }
    }

    private void ensureAdmin(LoginUser u) {
        if (u == null || u.getRole() == null) throw new ForbiddenException("관리자 권한이 필요합니다");
        if (u.getRole() != Role.ADMIN) throw new ForbiddenException("관리자 권한이 필요합니다");
    }

    private Map<String, String> validate(Map<String, String> form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String slug = trimOrNull(form.get("slug"));
        if (slug == null || slug.length() > SLUG_MAX || !SLUG_RE.matcher(slug).matches()) {
            errors.put("slug", "소문자/숫자/하이픈 1~40자 (시작·끝은 영숫자)");
        }
        String name = trimOrNull(form.get("name"));
        if (name == null || name.length() < NAME_MIN || name.length() > NAME_MAX) {
            errors.put("name", NAME_MIN + "~" + NAME_MAX + "자 이름");
        }
        String tagline = form.get("tagline");
        if (tagline != null && tagline.length() > TAGLINE_MAX) {
            errors.put("tagline", "최대 " + TAGLINE_MAX + "자");
        }
        try {
            int price = parsePrice(form.get("price"));
            if (price < 0) errors.put("price", "0 이상");
        } catch (NumberFormatException e) {
            errors.put("price", "숫자만 입력");
        }
        return errors;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static int parsePrice(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        return Integer.parseInt(s.trim());
    }
}
