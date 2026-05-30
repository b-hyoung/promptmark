package local.promptmark.web.action.asset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dao.TagDao;
import local.promptmark.dto.Asset;
import local.promptmark.dto.LoginUser;
import local.promptmark.dto.Tag;
import local.promptmark.service.AssetService;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/** GET {@code /app/asset/edit?id=N} — pre-filled form for the owner. */
public class EditFormAction implements Action {

    private final AssetService assetService;
    private final TagDao tagDao;

    public EditFormAction(AssetService assetService, TagDao tagDao) {
        this.assetService = assetService;
        this.tagDao = tagDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        long id = parseLong(req.getParameter("id"));

        Asset a = assetService.getEditable(id, me);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("type",      a.getType().name());
        form.put("title",     a.getTitle());
        form.put("summary",   a.getSummary());
        form.put("price",     String.valueOf(a.getPrice()));
        form.put("demo_url",  a.getDemoUrl() == null ? "" : a.getDemoUrl());
        form.put("video_url", a.getVideoUrl() == null ? "" : a.getVideoUrl());

        List<Tag> assetTags = tagDao.findByAssetId(a.getId());
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < assetTags.size(); i++) {
            if (i > 0) csv.append(',');
            csv.append(assetTags.get(i).getName());
        }

        req.setAttribute("mode", "edit");
        req.setAttribute("assetId", a.getId());
        req.setAttribute("asset", a);
        req.setAttribute("form", form);
        req.setAttribute("body", a.getBody() == null ? "" : a.getBody());
        req.setAttribute("tagsCsv", csv.toString());
        req.setAttribute("tags", tagDao.findAllOrLimit(100));
        return ViewResult.forward("asset/form");
    }

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("자산을 찾을 수 없습니다");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("자산을 찾을 수 없습니다"); }
    }
}
