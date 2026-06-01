package local.promptmark.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import local.promptmark.dao.PluginDao;
import local.promptmark.dao.TagDao;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginStatus;
import local.promptmark.dto.PluginType;
import local.promptmark.dto.LoginUser;
import local.promptmark.service.llm.DisabledEmbeddingClient;
import local.promptmark.service.llm.EmbeddingClient;
import local.promptmark.service.llm.LlmException;
import local.promptmark.web.ForbiddenException;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.Role;
import local.promptmark.web.ValidationException;

/**
 * Plugin CRUD + search. Validation rules live here (not the DAO) so all entry
 * points share the same field error map. Wrap-in-transaction is done for
 * create/edit so the plugin insert and the plugin_tags rebuild succeed or fail
 * together.
 */
public class PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginService.class);

    private static final int TITLE_MIN = 2;
    private static final int TITLE_MAX = 120;
    private static final int SUMMARY_MIN = 2;
    private static final int SUMMARY_MAX = 300;
    private static final int URL_MAX = 500;
    private static final int EMBEDDING_BODY_PREVIEW = 500;

    private static final Pattern URL_RE = Pattern.compile(
        "^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$");

    private final DataSource ds;
    private final PluginDao pluginDao;
    private final TagDao tagDao;
    private final EmbeddingClient embeddingClient;

    /** Backwards-compatible constructor; embedding generation is disabled. */
    public PluginService(DataSource ds, PluginDao pluginDao, TagDao tagDao) {
        this(ds, pluginDao, tagDao, new DisabledEmbeddingClient());
    }

    public PluginService(DataSource ds, PluginDao pluginDao, TagDao tagDao,
                        EmbeddingClient embeddingClient) {
        this.ds = ds;
        this.pluginDao = pluginDao;
        this.tagDao = tagDao;
        this.embeddingClient = (embeddingClient == null)
            ? new DisabledEmbeddingClient() : embeddingClient;
    }

    /**
     * Validate inputs, insert plugin, persist tags — all in one tx so a tag
     * failure rolls the plugin row back too. Returns the new plugin's id.
     */
    public long createPlugin(LoginUser seller,
                            Map<String, String> form,
                            String body,
                            String fileKey,
                            List<String> tags) {
        if (seller == null) throw new ForbiddenException("로그인이 필요합니다");

        PluginType type = parseType(form);
        Map<String, String> errors = validate(form, type, body, fileKey);
        if (!errors.isEmpty()) {
            throw new ValidationException("입력값을 확인해주세요", errors);
        }

        String title    = form.get("title").trim();
        String summary  = form.get("summary").trim();
        String demoUrl  = trimOrNull(form.get("demo_url"));
        String videoUrl = trimOrNull(form.get("video_url"));
        int price       = parsePrice(form.get("price"));
        String bodyVal  = type == PluginType.PROMPT ? body : null;
        String fileVal  = type == PluginType.MD ? fileKey : null;

        Plugin draft = new Plugin(0L, seller.getId(), type, title, summary,
            bodyVal, fileVal, demoUrl, videoUrl, price,
            PluginStatus.PUBLIC, 0, 0, null, null);

        long newId;
        try (Connection c = ds.getConnection()) {
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                newId = pluginDao.insert(draft);
                tagDao.replaceForPlugin(newId, tags, c);
                c.commit();
            } catch (RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("createPlugin failed: " + e.getMessage(), e);
        }

        // Best-effort embedding generation after the transactional commit.
        // Failures here MUST NOT roll the plugin back — log + continue.
        tryUpdateEmbedding(newId, title, summary, bodyVal);
        return newId;
    }

    /** Fetch + bump view count. NotFound when missing or DELETED. */
    public Plugin getDetailAndIncrementView(long id) {
        Optional<Plugin> opt = pluginDao.findById(id);
        if (!opt.isPresent()) {
            throw new NotFoundException("자산을 찾을 수 없습니다");
        }
        pluginDao.incrementViewCount(id);
        return opt.get();
    }

    /**
     * Fetch an plugin for the edit/delete flow. Non-owner non-ADMIN gets a
     * forbidden response (rather than 404) so they can't probe ids.
     */
    public Plugin getEditable(long id, LoginUser owner) {
        if (owner == null) throw new ForbiddenException("로그인이 필요합니다");
        if (owner.getRole() == Role.ADMIN) {
            return pluginDao.findById(id)
                .orElseThrow(() -> new NotFoundException("자산을 찾을 수 없습니다"));
        }
        return pluginDao.findByIdForOwner(id, owner.getId())
            .orElseThrow(() -> new ForbiddenException("해당 자산을 편집할 권한이 없습니다"));
    }

    public void editPlugin(long id,
                          LoginUser owner,
                          Map<String, String> form,
                          String body,
                          String fileKey,
                          List<String> tags) {
        Plugin existing = getEditable(id, owner);

        PluginType type = existing.getType(); // type is immutable post-create
        Map<String, String> errors = validate(form, type, body, fileKey);
        if (!errors.isEmpty()) {
            throw new ValidationException("입력값을 확인해주세요", errors);
        }

        String title    = form.get("title").trim();
        String summary  = form.get("summary").trim();
        String demoUrl  = trimOrNull(form.get("demo_url"));
        String videoUrl = trimOrNull(form.get("video_url"));
        int price       = parsePrice(form.get("price"));
        String bodyVal  = type == PluginType.PROMPT ? body : null;
        // Allow MD edits without a new upload — keep the existing key.
        String fileVal  = type == PluginType.MD
            ? (fileKey != null && !fileKey.isEmpty() ? fileKey : existing.getFileKey())
            : null;

        try (Connection c = ds.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                pluginDao.update(existing.getId(), title, summary, bodyVal,
                    fileVal, demoUrl, videoUrl, price);
                tagDao.replaceForPlugin(existing.getId(), tags, c);
                c.commit();
            } catch (RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new RuntimeException("editPlugin failed: " + e.getMessage(), e);
        }

        // Re-embed after a successful edit; same best-effort policy as create.
        tryUpdateEmbedding(existing.getId(), title, summary, bodyVal);
    }

    /**
     * Build the embedding text ({@code title + summary + body_preview(<=500)})
     * and persist the vector to the {@code plugins.embedding} column. Any
     * failure is logged and swallowed — embedding is best-effort metadata, not
     * a correctness invariant of the plugin row.
     */
    private void tryUpdateEmbedding(long pluginId, String title, String summary, String body) {
        if (embeddingClient == null || !embeddingClient.enabled()) return;
        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append(title);
        sb.append(' ');
        if (summary != null) sb.append(summary);
        sb.append(' ');
        if (body != null) {
            sb.append(body.length() <= EMBEDDING_BODY_PREVIEW
                ? body
                : body.substring(0, EMBEDDING_BODY_PREVIEW));
        }
        String text = sb.toString().trim();
        if (text.isEmpty()) return;
        try {
            float[] vec = embeddingClient.embed(text);
            if (vec != null && vec.length > 0) {
                pluginDao.updateEmbedding(pluginId, vec);
            }
        } catch (LlmException e) {
            log.warn("embedding update failed for plugin {}: {}", pluginId, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("embedding update threw unexpectedly for plugin {}: {}",
                pluginId, e.getMessage(), e);
        }
    }

    public void deletePlugin(long id, LoginUser owner) {
        // Reuses getEditable for the owner / ADMIN check.
        Plugin existing = getEditable(id, owner);
        pluginDao.softDelete(existing.getId());
    }

    public List<Plugin> search(String q, PluginType type, String tag,
                              String sort, int offset, int limit) {
        int safeLimit = clamp(limit, 1, 50);
        int safeOffset = Math.max(0, offset);
        return pluginDao.search(q, type, tag, sort, safeOffset, safeLimit);
    }

    public int countSearch(String q, PluginType type, String tag) {
        return pluginDao.countSearch(q, type, tag);
    }

    // ───── validation ─────────────────────────────────────────────────

    private static Map<String, String> validate(Map<String, String> form,
                                                PluginType type,
                                                String body,
                                                String fileKey) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form == null) {
            errors.put("_", "양식 데이터가 없습니다");
            return errors;
        }

        String title = trim(form.get("title"));
        if (title.length() < TITLE_MIN || title.length() > TITLE_MAX) {
            errors.put("title", "제목은 " + TITLE_MIN + "~" + TITLE_MAX + "자여야 합니다");
        }
        String summary = trim(form.get("summary"));
        if (summary.length() < SUMMARY_MIN || summary.length() > SUMMARY_MAX) {
            errors.put("summary", "요약은 " + SUMMARY_MIN + "~" + SUMMARY_MAX + "자여야 합니다");
        }

        String priceStr = trim(form.get("price"));
        int price;
        try {
            price = Integer.parseInt(priceStr);
        } catch (NumberFormatException nfe) {
            errors.put("price", "가격은 0 이상의 정수여야 합니다");
            price = -1;
        }
        if (price < 0) errors.put("price", "가격은 0 이상이어야 합니다");

        if (type == null) {
            errors.put("type", "자산 종류를 선택하세요");
        } else if (type == PluginType.PROMPT) {
            if (body == null || body.trim().isEmpty()) {
                errors.put("body", "프롬프트 본문을 입력하세요");
            }
        } else if (type == PluginType.MD) {
            if (fileKey == null || fileKey.isEmpty()) {
                errors.put("file", "마크다운 파일을 첨부하세요");
            }
        }

        validateOptionalUrl(form.get("demo_url"), "demo_url", errors);
        validateOptionalUrl(form.get("video_url"), "video_url", errors);

        return errors;
    }

    private static void validateOptionalUrl(String value, String field,
                                            Map<String, String> errors) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        if (v.length() > URL_MAX) {
            errors.put(field, "URL이 너무 깁니다");
            return;
        }
        if (!URL_RE.matcher(v).matches()) {
            errors.put(field, "올바른 URL 형식이 아닙니다 (http:// 또는 https://)");
        }
    }

    private static PluginType parseType(Map<String, String> form) {
        if (form == null) return null;
        String t = form.get("type");
        if (t == null) return null;
        try {
            return PluginType.valueOf(t.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int parsePrice(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Utility for callers that want an empty form map without nulls. */
    public static Map<String, String> emptyForm() {
        return Collections.emptyMap();
    }
}
