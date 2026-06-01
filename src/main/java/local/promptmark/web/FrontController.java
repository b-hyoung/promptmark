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
import local.promptmark.config.Env;
import local.promptmark.dao.BundleDao;
import local.promptmark.dao.PluginDao;
import local.promptmark.dao.DownloadDao;
import local.promptmark.dao.OrderDao;
import local.promptmark.dao.ReportDao;
import local.promptmark.dao.TagDao;
import local.promptmark.dao.UserDao;
import local.promptmark.service.AdminService;
import local.promptmark.service.BundleService;
import local.promptmark.service.PluginService;
import local.promptmark.service.AuthService;
import local.promptmark.service.DownloadService;
import local.promptmark.service.OrderService;
import local.promptmark.service.RecommendService;
import local.promptmark.service.llm.EmbeddingClient;
import local.promptmark.service.llm.LlmAgent;
import local.promptmark.service.llm.LlmClient;
import local.promptmark.service.llm.LlmConfig;
import local.promptmark.service.llm.Tools;
import local.promptmark.web.action.chat.ChatPageAction;
import local.promptmark.web.action.chat.RecommendAction;
import local.promptmark.web.action.plugin.CreateAction;
import local.promptmark.web.action.plugin.CreateFormAction;
import local.promptmark.web.action.plugin.DeleteAction;
import local.promptmark.web.action.plugin.DetailAction;
import local.promptmark.web.action.plugin.DownloadAction;
import local.promptmark.web.action.plugin.EditAction;
import local.promptmark.web.action.plugin.EditFormAction;
import local.promptmark.web.action.plugin.ListAction;
import local.promptmark.web.action.plugin.ReportAction;
import local.promptmark.web.action.auth.LoginAction;
import local.promptmark.web.action.auth.LoginFormAction;
import local.promptmark.web.action.auth.LogoutAction;
import local.promptmark.web.action.auth.SignupAction;
import local.promptmark.web.action.auth.SignupFormAction;
import local.promptmark.web.action.cart.AddAction;
import local.promptmark.web.action.cart.RemoveAction;
import local.promptmark.web.action.cart.ViewAction;
import local.promptmark.web.action.admin.BanUserAction;
import local.promptmark.web.action.admin.ReportsAction;
import local.promptmark.web.action.admin.ResolveReportAction;
import local.promptmark.web.action.mypage.IndexAction;
import local.promptmark.web.action.order.CheckoutFormAction;
import local.promptmark.web.action.order.CompleteAction;
import local.promptmark.web.action.order.HistoryAction;
import local.promptmark.web.action.order.PlaceOrderAction;

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
        javax.sql.DataSource ds = DataSourceProvider.get();

        UserDao userDao = new UserDao(ds);
        PluginDao pluginDao = new PluginDao(ds);
        BundleDao bundleDao = new BundleDao(ds);
        TagDao tagDao = new TagDao(ds);
        OrderDao orderDao = new OrderDao(ds);
        DownloadDao downloadDao = new DownloadDao(ds);
        ReportDao reportDao = new ReportDao(ds);

        // ── LLM / agent wiring (Phase 4) ─────────────────────────────
        LlmConfig llmConfig = LlmConfig.fromEnv(Env.load());
        EmbeddingClient embeddingClient = llmConfig.newEmbeddingClient();
        LlmClient llmClient = llmConfig.newLlmClient();
        Tools tools = new Tools(pluginDao, bundleDao, tagDao, userDao, embeddingClient);
        LlmAgent llmAgent = new LlmAgent(llmClient, embeddingClient, tools, llmConfig);
        RecommendService recommendService = new RecommendService(
            pluginDao, tagDao, embeddingClient, llmAgent, llmConfig);

        AuthService authService = new AuthService(userDao);
        PluginService pluginService = new PluginService(ds, pluginDao, tagDao, embeddingClient);
        BundleService bundleService = new BundleService(ds, bundleDao, embeddingClient);
        OrderService orderService = new OrderService(ds, pluginDao, orderDao);
        DownloadService downloadService = new DownloadService(ds, pluginDao, orderDao, downloadDao);
        AdminService adminService = new AdminService(ds, reportDao, pluginDao, userDao);

        // ── Auth ──────────────────────────────────────────────────────
        registry.put("auth.signup.GET",  new SignupFormAction());
        registry.put("auth.signup.POST", new SignupAction(authService));
        registry.put("auth.login.GET",   new LoginFormAction());
        registry.put("auth.login.POST",  new LoginAction(authService));
        registry.put("auth.logout.POST", new LogoutAction());

        // ── Plugin ─────────────────────────────────────────────────────
        registry.put("plugin.list.GET",     new ListAction(pluginService));
        registry.put("plugin.detail.GET",   new DetailAction(pluginService, tagDao, userDao));
        registry.put("plugin.new.GET",      new CreateFormAction(tagDao));
        registry.put("plugin.new.POST",     new CreateAction(pluginService, tagDao));
        registry.put("plugin.edit.GET",     new EditFormAction(pluginService, tagDao));
        registry.put("plugin.edit.POST",    new EditAction(pluginService, tagDao));
        registry.put("plugin.delete.POST",  new DeleteAction(pluginService));
        registry.put("plugin.download.GET", new DownloadAction(downloadService));
        registry.put("plugin.report.POST",  new ReportAction(reportDao));

        // ── Bundle (curated sets, ADMIN only for CRUD) ────────────────
        registry.put("bundle.list.GET",     new local.promptmark.web.action.bundle.ListAction(bundleService));
        registry.put("bundle.detail.GET",   new local.promptmark.web.action.bundle.DetailAction(bundleService, bundleDao));
        registry.put("bundle.new.GET",      new local.promptmark.web.action.bundle.CreateFormAction(pluginDao));
        registry.put("bundle.new.POST",     new local.promptmark.web.action.bundle.CreateAction(bundleService));
        registry.put("bundle.delete.POST",  new local.promptmark.web.action.bundle.DeleteAction(bundleService));

        // ── Cart ──────────────────────────────────────────────────────
        registry.put("cart.add.POST",    new AddAction(pluginDao, orderDao));
        registry.put("cart.view.GET",    new ViewAction());
        registry.put("cart.remove.POST", new RemoveAction());

        // ── Order ─────────────────────────────────────────────────────
        registry.put("order.checkout.GET",  new CheckoutFormAction());
        registry.put("order.checkout.POST", new PlaceOrderAction(orderService));
        registry.put("order.complete.GET",  new CompleteAction(orderService));
        registry.put("order.history.GET",   new HistoryAction(orderService));

        // ── Chat / AI agent (Phase 4) ─────────────────────────────────
        registry.put("chat.page.GET",       new ChatPageAction());
        registry.put("chat.recommend.POST", new RecommendAction(recommendService));

        // ── Mypage (Phase 5) ──────────────────────────────────────────
        registry.put("mypage.index.GET",    new IndexAction(pluginDao, orderDao, downloadDao));

        // ── Admin (Phase 5) ───────────────────────────────────────────
        registry.put("admin.reports.GET",        new ReportsAction(adminService));
        registry.put("admin.report.resolve.POST", new ResolveReportAction(adminService));
        registry.put("admin.user.ban.POST",       new BanUserAction(adminService));

        log.info("FrontController initialised with {} actions (LLM enabled={})",
            registry.size(), llmConfig.enabled());
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
     *
     * <p>The action portion may contain additional slashes, which are folded
     * into dots — e.g. {@code "/admin/report/resolve"} → {@code "admin.report.resolve"}.
     * This keeps the routing table flat while letting the URL surface stay
     * grouped (admin actions live under {@code /app/admin/...}).
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
        // Empty segment guard — reject any "//" anywhere or leading/trailing /.
        if (action.startsWith("/") || action.endsWith("/")
                || action.contains("//")) {
            return null;
        }
        // Fold remaining segments into dotted action names.
        if (action.indexOf('/') >= 0) {
            action = action.replace('/', '.');
        }
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
