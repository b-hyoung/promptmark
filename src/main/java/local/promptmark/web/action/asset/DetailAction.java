package local.promptmark.web.action.asset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;

import local.promptmark.dao.TagDao;
import local.promptmark.dao.UserDao;
import local.promptmark.dto.Asset;
import local.promptmark.dto.AssetType;
import local.promptmark.dto.User;
import local.promptmark.service.AssetService;
import local.promptmark.web.Action;
import local.promptmark.web.EmbedHelper;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/**
 * GET {@code /app/asset/detail?id=N} — single asset page.
 *
 * <p>Bumps view_count via {@link AssetService}. For PROMPT assets the body is
 * left as plain text (the JSP escapes it inside a {@code <pre>}); for MD it is
 * rendered to HTML with flexmark and surfaced as {@code bodyHtml}.
 */
public class DetailAction implements Action {

    private static final Parser MD_PARSER = Parser.builder().build();
    private static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder().build();

    private final AssetService assetService;
    private final TagDao tagDao;
    private final UserDao userDao;

    public DetailAction(AssetService assetService, TagDao tagDao, UserDao userDao) {
        this.assetService = assetService;
        this.tagDao = tagDao;
        this.userDao = userDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        long id = parseLong(req.getParameter("id"));
        Asset a = assetService.getDetailAndIncrementView(id);

        String sellerNick = userDao.findById(a.getSellerId())
            .map(User::getNickname)
            .orElse(null);

        req.setAttribute("asset", a);
        req.setAttribute("tags", tagDao.findByAssetId(a.getId()));
        req.setAttribute("sellerNickname", sellerNick);
        req.setAttribute("videoEmbedSrc", EmbedHelper.toIframeSrc(a.getVideoUrl()));

        if (a.getType() == AssetType.MD) {
            String body = a.getBody() == null ? "" : a.getBody();
            Node doc = MD_PARSER.parse(body);
            req.setAttribute("bodyHtml", MD_RENDERER.render(doc));
        }
        return ViewResult.forward("asset/detail");
    }

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("자산을 찾을 수 없습니다");
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException nfe) {
            throw new NotFoundException("자산을 찾을 수 없습니다");
        }
    }
}
