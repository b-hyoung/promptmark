package local.promptmark.web.action.bundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;

import local.promptmark.dao.BundleDao;
import local.promptmark.dto.Bundle;
import local.promptmark.service.BundleService;
import local.promptmark.web.Action;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/** GET /app/bundle/detail?id=N — also renders bundle.story markdown into bodyHtml. */
public class DetailAction implements Action {
    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder().build();

    private final BundleService bundleService;
    private final BundleDao bundleDao;

    public DetailAction(BundleService bundleService, BundleDao bundleDao) {
        this.bundleService = bundleService;
        this.bundleDao = bundleDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        long id = parseId(req.getParameter("id"));
        Bundle b = bundleService.findByIdWithPlugins(id)
            .orElseThrow(() -> new NotFoundException("Bundle not found: " + id));
        bundleDao.incrementViewCount(id);

        // Render story as markdown HTML (safe — only admins author bundles).
        String storyHtml = "";
        if (b.getStory() != null && !b.getStory().isEmpty()) {
            Node doc = MD_PARSER.parse(b.getStory());
            storyHtml = MD_RENDERER.render(doc);
        }

        req.setAttribute("bundle", b);
        req.setAttribute("storyHtml", storyHtml);
        return ViewResult.forward("bundle/detail");
    }

    private static long parseId(String s) {
        if (s == null) throw new NotFoundException("id required");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("invalid id: " + s); }
    }
}
