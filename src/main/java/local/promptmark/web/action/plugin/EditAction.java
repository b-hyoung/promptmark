package local.promptmark.web.action.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oreilly.servlet.MultipartRequest;

import local.promptmark.dao.TagDao;
import local.promptmark.dto.Plugin;
import local.promptmark.dto.PluginType;
import local.promptmark.dto.LoginUser;
import local.promptmark.service.PluginService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.UploadUtil;
import local.promptmark.web.ValidationException;
import local.promptmark.web.ViewResult;

/** POST {@code /app/plugin/edit?id=N} — apply edits + redirect to detail. */
public class EditAction implements Action {

    private final PluginService pluginService;
    private final TagDao tagDao;

    public EditAction(PluginService pluginService, TagDao tagDao) {
        this.pluginService = pluginService;
        this.tagDao = tagDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) throws Exception {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        long id = parseLong(req.getParameter("id"));

        // Fetch the existing plugin first so we know its type (immutable) and can
        // re-use the existing file_key if no new file was uploaded.
        Plugin existing = pluginService.getEditable(id, me);

        MultipartRequest mr = UploadUtil.parse(req);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("type",      existing.getType().name()); // type is fixed
        form.put("title",     UploadUtil.getParam(mr, "title", ""));
        form.put("summary",   UploadUtil.getParam(mr, "summary", ""));
        form.put("price",     UploadUtil.getParam(mr, "price", "0"));
        form.put("demo_url",  UploadUtil.getParam(mr, "demo_url", ""));
        form.put("video_url", UploadUtil.getParam(mr, "video_url", ""));

        String body = UploadUtil.getParam(mr, "body", "");
        String fileKey = null;
        if (existing.getType() == PluginType.MD) {
            String maybe = UploadUtil.moveToPermanentStore(mr, "file");
            fileKey = (maybe == null || maybe.isEmpty()) ? existing.getFileKey() : maybe;
        }

        List<String> tags = parseTags(UploadUtil.getParam(mr, "tags", ""));

        try {
            pluginService.editPlugin(id, me, form, body, fileKey, tags);
            return ViewResult.redirect(req.getContextPath() + "/app/plugin/detail?id=" + id);
        } catch (ValidationException ve) {
            req.setAttribute("mode", "edit");
            req.setAttribute("pluginId", id);
            req.setAttribute("plugin", existing);
            req.setAttribute("form", form);
            req.setAttribute("body", body);
            req.setAttribute("tagsCsv", String.join(",", tags));
            req.setAttribute("errors", ve.fieldErrors());
            req.setAttribute("errorMessage", ve.userMessage());
            req.setAttribute("tags", tagDao.findAllOrLimit(100));
            return ViewResult.forward("plugin/form");
        }
    }

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

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("자산을 찾을 수 없습니다");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("자산을 찾을 수 없습니다"); }
    }
}
