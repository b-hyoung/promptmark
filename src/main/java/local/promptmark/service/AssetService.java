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

/**
 * Asset CRUD + search. Validation rules live here (not the DAO) so all entry
 * points share the same field error map. Wrap-in-transaction is done for
 * create/edit so the asset insert and the asset_tags rebuild succeed or fail
 * together.
 */
public class AssetService {

    private static final int TITLE_MIN = 2;
    private static final int TITLE_MAX = 120;
    private static final int SUMMARY_MIN = 2;
    private static final int SUMMARY_MAX = 300;
    private static final int URL_MAX = 500;

    private static final Pattern URL_RE = Pattern.compile(
        "^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$");

    private final DataSource ds;
    private final AssetDao assetDao;
    private final TagDao tagDao;

    public AssetService(DataSource ds, AssetDao assetDao, TagDao tagDao) {
        this.ds = ds;
        this.assetDao = assetDao;
        this.tagDao = tagDao;
    }

    /**
     * Validate inputs, insert asset, persist tags — all in one tx so a tag
     * failure rolls the asset row back too. Returns the new asset's id.
     */
    public long createAsset(LoginUser seller,
                            Map<String, String> form,
                            String body,
                            String fileKey,
                            List<String> tags) {
        if (seller == null) throw new ForbiddenException("로그인이 필요합니다");

        AssetType type = parseType(form);
        Map<String, String> errors = validate(form, type, body, fileKey);
        if (!errors.isEmpty()) {
            throw new ValidationException("입력값을 확인해주세요", errors);
        }

        String title    = form.get("title").trim();
        String summary  = form.get("summary").trim();
        String demoUrl  = trimOrNull(form.get("demo_url"));
        String videoUrl = trimOrNull(form.get("video_url"));
        int price       = parsePrice(form.get("price"));
        String bodyVal  = type == AssetType.PROMPT ? body : null;
        String fileVal  = type == AssetType.MD ? fileKey : null;

        Asset draft = new Asset(0L, seller.getId(), type, title, summary,
            bodyVal, fileVal, demoUrl, videoUrl, price,
            AssetStatus.PUBLIC, 0, 0, null, null);

        try (Connection c = ds.getConnection()) {
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                long newId = assetDao.insert(draft);
                tagDao.replaceForAsset(newId, tags, c);
                c.commit();
                return newId;
            } catch (RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("createAsset failed: " + e.getMessage(), e);
        }
    }

    /** Fetch + bump view count. NotFound when missing or DELETED. */
    public Asset getDetailAndIncrementView(long id) {
        Optional<Asset> opt = assetDao.findById(id);
        if (!opt.isPresent()) {
            throw new NotFoundException("자산을 찾을 수 없습니다");
        }
        assetDao.incrementViewCount(id);
        return opt.get();
    }

    /**
     * Fetch an asset for the edit/delete flow. Non-owner non-ADMIN gets a
     * forbidden response (rather than 404) so they can't probe ids.
     */
    public Asset getEditable(long id, LoginUser owner) {
        if (owner == null) throw new ForbiddenException("로그인이 필요합니다");
        if (owner.getRole() == Role.ADMIN) {
            return assetDao.findById(id)
                .orElseThrow(() -> new NotFoundException("자산을 찾을 수 없습니다"));
        }
        return assetDao.findByIdForOwner(id, owner.getId())
            .orElseThrow(() -> new ForbiddenException("해당 자산을 편집할 권한이 없습니다"));
    }

    public void editAsset(long id,
                          LoginUser owner,
                          Map<String, String> form,
                          String body,
                          String fileKey,
                          List<String> tags) {
        Asset existing = getEditable(id, owner);

        AssetType type = existing.getType(); // type is immutable post-create
        Map<String, String> errors = validate(form, type, body, fileKey);
        if (!errors.isEmpty()) {
            throw new ValidationException("입력값을 확인해주세요", errors);
        }

        String title    = form.get("title").trim();
        String summary  = form.get("summary").trim();
        String demoUrl  = trimOrNull(form.get("demo_url"));
        String videoUrl = trimOrNull(form.get("video_url"));
        int price       = parsePrice(form.get("price"));
        String bodyVal  = type == AssetType.PROMPT ? body : null;
        // Allow MD edits without a new upload — keep the existing key.
        String fileVal  = type == AssetType.MD
            ? (fileKey != null && !fileKey.isEmpty() ? fileKey : existing.getFileKey())
            : null;

        try (Connection c = ds.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                assetDao.update(existing.getId(), title, summary, bodyVal,
                    fileVal, demoUrl, videoUrl, price);
                tagDao.replaceForAsset(existing.getId(), tags, c);
                c.commit();
            } catch (RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new RuntimeException("editAsset failed: " + e.getMessage(), e);
        }
    }

    public void deleteAsset(long id, LoginUser owner) {
        // Reuses getEditable for the owner / ADMIN check.
        Asset existing = getEditable(id, owner);
        assetDao.softDelete(existing.getId());
    }

    public List<Asset> search(String q, AssetType type, String tag,
                              String sort, int offset, int limit) {
        int safeLimit = clamp(limit, 1, 50);
        int safeOffset = Math.max(0, offset);
        return assetDao.search(q, type, tag, sort, safeOffset, safeLimit);
    }

    public int countSearch(String q, AssetType type, String tag) {
        return assetDao.countSearch(q, type, tag);
    }

    // ───── validation ─────────────────────────────────────────────────

    private static Map<String, String> validate(Map<String, String> form,
                                                AssetType type,
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
        } else if (type == AssetType.PROMPT) {
            if (body == null || body.trim().isEmpty()) {
                errors.put("body", "프롬프트 본문을 입력하세요");
            }
        } else if (type == AssetType.MD) {
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

    private static AssetType parseType(Map<String, String> form) {
        if (form == null) return null;
        String t = form.get("type");
        if (t == null) return null;
        try {
            return AssetType.valueOf(t.trim().toUpperCase());
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
