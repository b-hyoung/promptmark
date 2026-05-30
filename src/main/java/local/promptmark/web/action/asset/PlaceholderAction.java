package local.promptmark.web.action.asset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import local.promptmark.web.Action;
import local.promptmark.web.ViewResult;

/**
 * Stand-in for {@code asset.list} and {@code asset.detail} until Phase 3 lands.
 * Renders a tiny holding page so the nav and welcome redirect have a real
 * destination during Phase 2 manual testing.
 */
public class PlaceholderAction implements Action {

    @Override
    public ViewResult execute(HttpServletRequest req, HttpServletResponse res) {
        return ViewResult.forward("asset/placeholder");
    }
}
