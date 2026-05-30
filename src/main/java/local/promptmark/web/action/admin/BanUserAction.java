package local.promptmark.web.action.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.service.AdminService;
import local.promptmark.web.Action;
import local.promptmark.web.ValidationException;
import local.promptmark.web.ViewResult;

/**
 * POST {@code /app/admin/user/ban?userId=<id>}. Sets the target user's status to
 * BANNED. Redirects back to the reports queue with a flash {@code msg}.
 */
public class BanUserAction implements Action {

    private final AdminService adminService;

    public BanUserAction(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        long userId = parseLong(req.getParameter("userId"));
        if (userId <= 0) {
            throw new ValidationException("userId 파라미터가 잘못되었습니다",
                java.util.Collections.singletonMap("userId", "invalid"));
        }
        adminService.banUser(userId);
        String ctx = req.getContextPath() == null ? "" : req.getContextPath();
        return ViewResult.redirect(ctx + "/app/admin/reports?msg=banned_ok");
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
