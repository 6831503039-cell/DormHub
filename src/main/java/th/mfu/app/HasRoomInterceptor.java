package th.mfu.app;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import th.mfu.service.CurrentUserService;

/**
 * Accounts without a room only get the home page and renting a room (/app, /app/rent).
 * Every other tenant page (bills, chat, laundry, requests, announcements, parcels ...) sends them back home;
 * once the admin confirms their booking the full menu appears.
 * Registered for /app/** in WebConfig.
 */
@Component
public class HasRoomInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUser;

    public HasRoomInterceptor(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Principal principal = request.getUserPrincipal();
        if (principal == null || currentUser.hasRoom(principal)) {
            return true;
        }
        if (String.valueOf(request.getHeader("Accept")).contains("application/json")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } else {
            response.sendRedirect(request.getContextPath() + "/app");
        }
        return false;
    }
}
