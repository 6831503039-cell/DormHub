package th.mfu.service;

import java.security.Principal;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import th.mfu.AppUserRepository;
import th.mfu.config.DormProperties;
import th.mfu.domain.AppUser;

/** Makes the logged-in account, dorm name and office hours available to every template. */
@ControllerAdvice
public class CurrentUserAdvice {

    private final AppUserRepository users;
    private final DormProperties props;
    private final CurrentUserService currentUser;

    public CurrentUserAdvice(AppUserRepository users, DormProperties props, CurrentUserService currentUser) {
        this.users = users;
        this.props = props;
        this.currentUser = currentUser;
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser(Principal principal) {
        return principal == null ? null : users.findByEmailIgnoreCase(principal.getName()).orElse(null);
    }

    @ModelAttribute("dormName")
    public String dormName() {
        return props.getDormName();
    }

    /** Tenant app: false for an account without a room, which only sees home and "rent a room". */
    @ModelAttribute("hasRoom")
    public boolean hasRoom(Principal principal) {
        return principal != null && currentUser.hasRoom(principal);
    }

    @ModelAttribute("residenceName")
    public String residenceName() {
        return props.getResidenceName();
    }

    @ModelAttribute("officeHours")
    public String officeHours() {
        return props.getOfficeHours();
    }
}
