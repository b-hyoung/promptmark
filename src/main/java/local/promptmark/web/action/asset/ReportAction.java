package local.promptmark.web.action.asset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.dao.ReportDao;
import local.promptmark.dto.LoginUser;
import local.promptmark.web.Action;
import local.promptmark.web.AuthFilter;
import local.promptmark.web.NotFoundException;
import local.promptmark.web.ValidationException;
import local.promptmark.web.ViewResult;

/**
 * POST {@code /app/asset/report?id=N} — record a user report against an asset.
 * Reason is required (3..300 chars). On success bounces back to detail with a
 * confirmation flag.
 */
public class ReportAction implements Action {

    private static final int REASON_MIN = 3;
    private static final int REASON_MAX = 300;

    private final ReportDao reportDao;

    public ReportAction(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        LoginUser me = (LoginUser) req.getSession().getAttribute(AuthFilter.LOGIN_USER_ATTR);
        long id = parseLong(req.getParameter("id"));

        String reason = req.getParameter("reason");
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.length() < REASON_MIN || trimmed.length() > REASON_MAX) {
            throw new ValidationException("신고 사유는 " + REASON_MIN + "~" + REASON_MAX + "자여야 합니다",
                java.util.Collections.singletonMap("reason",
                    "신고 사유는 " + REASON_MIN + "~" + REASON_MAX + "자여야 합니다"));
        }

        reportDao.insert(me.getId(), id, trimmed);
        return ViewResult.redirect(req.getContextPath() +
            "/app/asset/detail?id=" + id + "&msg=reported_ok");
    }

    private static long parseLong(String s) {
        if (s == null) throw new NotFoundException("자산을 찾을 수 없습니다");
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException nfe) { throw new NotFoundException("자산을 찾을 수 없습니다"); }
    }
}
