package local.promptmark.web.action.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.service.AdminService;
import local.promptmark.web.Action;
import local.promptmark.web.ValidationException;
import local.promptmark.web.ViewResult;

/**
 * POST {@code /app/admin/report/resolve?id=<reportId>&action=HIDE|DELETE|REJECT}.
 * On success, redirects back to the queue with a small flash message via the
 * {@code msg} query param (rendered by reports.jsp).
 */
public class ResolveReportAction implements Action {

    private final AdminService adminService;

    public ResolveReportAction(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        long reportId = parseLong(req.getParameter("id"));
        if (reportId <= 0) {
            throw new ValidationException("id 파라미터가 잘못되었습니다",
                java.util.Collections.singletonMap("id", "invalid"));
        }
        String action = req.getParameter("action");
        adminService.resolveReport(reportId, action);
        String ctx = req.getContextPath() == null ? "" : req.getContextPath();
        return ViewResult.redirect(ctx + "/app/admin/reports?msg=resolved_ok");
    }

    private static long parseLong(String s) {
        if (s == null || s.isEmpty()) return -1L;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException nfe) {
            return -1L;
        }
    }
}
