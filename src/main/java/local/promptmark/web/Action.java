package local.promptmark.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Single-action controller invoked by {@link FrontController}.
 *
 * <p>Implementations build a {@link ViewResult} (forward / redirect / json) and
 * never write to the response directly. Override {@link #producesJson()} to flip
 * error responses for this action into JSON envelopes.
 */
public interface Action {

    ViewResult execute(HttpServletRequest req, HttpServletResponse res) throws Exception;

    /**
     * @return true when error responses for this action should be JSON envelopes
     *         instead of HTML error pages. Default false (HTML).
     */
    default boolean producesJson() {
        return false;
    }
}
