package local.promptmark.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import local.promptmark.config.DataSourceProvider;
import local.promptmark.dao.UserDao;
import local.promptmark.service.AuthService;
import local.promptmark.web.action.asset.PlaceholderAction;
import local.promptmark.web.action.auth.LoginAction;
import local.promptmark.web.action.auth.LoginFormAction;
import local.promptmark.web.action.auth.LogoutAction;
import local.promptmark.web.action.auth.SignupAction;
import local.promptmark.web.action.auth.SignupFormAction;

/**
 * Single dispatcher behind {@code /app/*}. Parses {@code /{module}/{action}}
 * from the path-info, looks the key up in the {@link #registry}, runs the
 * action, and maps any thrown {@link AppException} (or other Throwable) to
 * the right error page or JSON envelope.
 */
public class FrontController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(FrontController.class);

    private final Map<String, Action> registry = new HashMap<>();

    @Override
    public void init() throws ServletException {
        UserDao userDao = new UserDao(DataSourceProvider.get());
        AuthService authService = new AuthService(userDao);

        registry.put("auth.signup.GET",  new SignupFormAction());
        registry.put("auth.signup.POST", new SignupAction(authService));
        registry.put("auth.login.GET",   new LoginFormAction());
        registry.put("auth.login.POST",  new LoginAction(authService));
        registry.put("auth.logout.POST", new LogoutAction());

        // Phase-3 placeholders so AuthMap entries have a target.
        PlaceholderAction placeholder = new PlaceholderAction();
        registry.put("asset.list.GET",   placeholder);
        registry.put("asset.detail.GET", placeholder);

        log.info("FrontController initialised with {} actions", registry.size());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo(); // e.g. "/auth/login"
        String method = req.getMethod();
        String actionKey = parseActionKey(pathInfo);

        if (actionKey == null) {
            sendError(req, res, new NotFoundException("알 수 없는 경로입니다"), false);
            return;
        }

        Action action = registry.get(actionKey + "." + method);
        if (action == null) {
            // Fall back: maybe an action exists for a different method → 405.
            // For Phase 2 we treat both no-action and wrong-method as 404 to keep things simple.
            sendError(req, res, new NotFoundException("요청하신 경로를 찾을 수 없습니다"), false);
            return;
        }

        try {
            ViewResult vr = action.execute(req, res);
            if (vr == null) {
                throw new IllegalStateException("Action " + actionKey + " returned null ViewResult");
            }
            vr.apply(req, res);
        } catch (AppException ae) {
            sendError(req, res, ae, action.producesJson());
        } catch (Exception ex) {
            String traceId = String.valueOf(req.getAttribute("traceId"));
            log.error("Unhandled exception in action {} [trace={}]", actionKey, traceId, ex);
            sendError(req, res,
                new AppException(500, "서버 오류가 발생했습니다", ex),
                action.producesJson());
        }
    }

    /**
     * Extracts {@code "module.action"} from path-info like {@code "/auth/login"}.
     * Returns null on a malformed or empty path.
     */
    static String parseActionKey(String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty()) return null;
        // strip leading slash
        String s = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        // strip trailing slash
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (s.isEmpty()) return null;
        int slash = s.indexOf('/');
        if (slash <= 0 || slash == s.length() - 1) return null;
        String module = s.substring(0, slash);
        String action = s.substring(slash + 1);
        if (module.isEmpty() || action.isEmpty()) return null;
        // Disallow nested segments — keep the surface flat.
        if (action.indexOf('/') >= 0) return null;
        return module + "." + action;
    }

    private void sendError(HttpServletRequest req,
                           HttpServletResponse res,
                           AppException ae,
                           boolean json) throws ServletException, IOException {
        int code = ae.code();
        res.setStatus(code);
        String traceId = String.valueOf(req.getAttribute("traceId"));

        if (json) {
            Map<String, Object> envelope = new LinkedHashMap<>();
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("code", code);
            error.put("message", ae.userMessage());
            error.put("traceId", traceId);
            envelope.put("error", error);
            ViewResult.json(envelope).apply(req, res);
            return;
        }

        req.setAttribute("errorMessage", ae.userMessage());
        req.setAttribute("errorCode", code);
        String view = "/WEB-INF/view/error/" + code + ".jsp";
        // Fall back to 500.jsp if the specific page is missing — Tomcat will
        // raise FileNotFound otherwise. We don't probe ServletContext here;
        // just guard for the codes we have pages for.
        if (code != 400 && code != 401 && code != 403 && code != 404 && code != 409) {
            view = "/WEB-INF/view/error/500.jsp";
        }
        req.getRequestDispatcher(view).forward(req, res);
    }
}
