package local.promptmark.web.action.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.service.AdminService;
import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/** GET {@code /app/admin/reports} — list of OPEN reports for the admin queue. */
public class ReportsAction implements Action {

    private static final int LIST_LIMIT = 100;

    private final AdminService adminService;

    public ReportsAction(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        req.setAttribute("reports", adminService.listOpenReports(LIST_LIMIT));
        return ViewResult.forward("admin/reports");
    }
}
