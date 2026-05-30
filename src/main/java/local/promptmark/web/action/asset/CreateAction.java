package local.promptmark.web.action.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oreilly.servlet.MultipartRequest;

import local.promptmark.dao.TagDao;
import local.promptmark.dto.AssetType;
import local.promptmark.dto.LoginUser;
import local.promptmark.service.AssetService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.UploadUtil;
import local.promptmark.web.ValidationException;
import local.promptmark.web.ViewResult;

/**
 * POST {@code /app/asset/new} — multipart asset creation.
 *
 * <p>Note: because CsrfFilter has to consume the request body before the
 * action runs, multipart parsing here happens against the same body. The
 * filter chain reads {@code csrf_token} as a form parameter, which works for
 * multipart too because the cos parser preserves text fields.
 */
public class CreateAction implements Action {

    private final AssetService assetService;
    private final TagDao tagDao;

    public CreateAction(AssetService assetService, TagDao tagDao) {
        this.assetService = assetService;
        this.tagDao = tagDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);

        MultipartRequest mr = UploadUtil.parse(req);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("type",      UploadUtil.getParam(mr, "type", ""));
        form.put("title",     UploadUtil.getParam(mr, "title", ""));
        form.put("summary",   UploadUtil.getParam(mr, "summary", ""));
        form.put("price",     UploadUtil.getParam(mr, "price", "0"));
        form.put("demo_url",  UploadUtil.getParam(mr, "demo_url", ""));
        form.put("video_url", UploadUtil.getParam(mr, "video_url", ""));

        String body = UploadUtil.getParam(mr, "body", "");
        AssetType type = parseType(form.get("type"));

        String fileKey = null;
        if (type == AssetType.MD) {
            fileKey = UploadUtil.moveToPermanentStore(mr, "file");
        }

        List<String> tags = parseTags(UploadUtil.getParam(mr, "tags", ""));

        try {
            long newId = assetService.createAsset(me, form, body, fileKey, tags);
            return ViewResult.redirect(req.getContextPath() + "/app/asset/detail?id=" + newId);
        } catch (ValidationException ve) {
            req.setAttribute("mode", "create");
            req.setAttribute("form", form);
            req.setAttribute("body", body);
            req.setAttribute("tagsCsv", String.join(",", tags));
            req.setAttribute("errors", ve.fieldErrors());
            req.setAttribute("errorMessage", ve.userMessage());
            req.setAttribute("tags", tagDao.findAllOrLimit(100));
            return ViewResult.forward("asset/form");
        }
    }

    private static AssetType parseType(String s) {
        if (s == null) return null;
        try { return AssetType.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** Accept a comma-separated CSV of tag names. */
    private static List<String> parseTags(String csv) {
        if (csv == null || csv.isEmpty()) return Collections.emptyList();
        String[] parts = csv.split(",");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
