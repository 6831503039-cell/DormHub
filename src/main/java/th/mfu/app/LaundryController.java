package th.mfu.app;

import java.security.Principal;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.config.Messages;
import th.mfu.domain.AppUser;
import th.mfu.domain.Tenant;
import th.mfu.service.CurrentUserService;
import th.mfu.service.LaundryService;

/** Shared laundry room status (mockup, simulated machines). */
@Controller
public class LaundryController {

    private final LaundryService laundryService;
    private final CurrentUserService currentUser;
    private final Messages messages;

    public LaundryController(LaundryService laundryService, CurrentUserService currentUser, Messages messages) {
        this.laundryService = laundryService;
        this.currentUser = currentUser;
        this.messages = messages;
    }

    @GetMapping("/app/laundry")
    public String laundry(Model model) {
        model.addAttribute("machines", laundryService.machines());
        model.addAttribute("now", laundryService.now());
        return "app/laundry";
    }

    @PostMapping("/app/laundry/{id}/start")
    public String start(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        AppUser user = currentUser.user(principal);
        Optional<Tenant> tenant = currentUser.tenant(user);
        String who = tenant.isPresent() && tenant.get().getRoom() != null
                ? messages.get("laundry.usedByRoom", tenant.get().getRoom().getRoomNumber())
                : user.getFullName();
        if (laundryService.start(id, who)) {
            redirect.addFlashAttribute("success", messages.get("flash.laundry.started"));
        } else {
            redirect.addFlashAttribute("error", messages.get("flash.laundry.busy"));
        }
        return "redirect:/app/laundry";
    }
}
