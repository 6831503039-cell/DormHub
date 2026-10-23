package th.mfu.service;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import th.mfu.AppUserRepository;
import th.mfu.config.DormProperties;
import th.mfu.domain.AppUser;

/** Makes the logged-in account and dorm name available to every template. */
@ControllerAdvice
public class CurrentUserAdvice {

    @Autowired
    private AppUserRepository users;
    @Autowired
    private DormProperties props;

    @ModelAttribute("currentUser")
    public AppUser currentUser(Principal principal) {
        return principal == null ? null : users.findByEmailIgnoreCase(principal.getName()).orElse(null);
    }

    @ModelAttribute("dormName")
    public String dormName() {
        return props.getDormName();
    }
}
