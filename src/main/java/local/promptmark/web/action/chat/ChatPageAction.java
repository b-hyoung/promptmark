package local.promptmark.web.action.chat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/**
 * Renders the AI chat page. The page itself is pure JSP/HTML/JS — all chat
 * interactions hit {@link RecommendAction} via {@code fetch}.
 */
public final class ChatPageAction implements Action {

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        return ViewResult.forward("chat/page");
    }
}
