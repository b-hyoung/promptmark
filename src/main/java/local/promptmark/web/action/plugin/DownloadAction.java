package local.promptmark.web.action.plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dto.LoginUser;
import local.promptmark.service.DownloadService;
import local.promptmark.service.DownloadService.DownloadPayload;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ViewResult;

/**
 * GET {@code /app/plugin/download?id=N} — resolve permissions, record the
 * download, and stream the bytes back. View result is binary so no JSP is
 * rendered.
 */
public class DownloadAction implements Action {

    private final DownloadService downloadService;

    public DownloadAction(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        long id = parseLong(req.getParameter("id"));

        DownloadPayload payload = downloadService.resolveDownload(id, me);
        // Permission already passed; record + bump count after success.
        downloadService.recordDownload(id, me.getId());

        return ViewResult.binary(payload.content, payload.filename, payload.contentType);
    }

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("자산을 찾을 수 없습니다");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("자산을 찾을 수 없습니다"); }
    }
}
