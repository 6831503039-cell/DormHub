package th.mfu.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.AppUserRepository;
import th.mfu.config.DormProperties;
import th.mfu.config.Messages;
import th.mfu.domain.AppUser;
import th.mfu.domain.Role;

@Controller
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final DormProperties props;
    private final Messages messages;

    public AuthController(AppUserRepository users, PasswordEncoder passwordEncoder, DormProperties props, Messages messages) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.messages = messages;
    }

    // "/" is the landing page after login: admins go to the dashboard, everyone else to the tenant app
    @GetMapping("/")
    public String home(Authentication auth) {
        boolean admin = false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a.getAuthority().equals("ROLE_ADMIN")) {
                admin = true;
            }
        }
        return admin ? "redirect:/admin" : "redirect:/app";
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") RegisterForm form, Model model, RedirectAttributes redirect) {
        String email = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase();
        String error = validate(form, email);
        if (error != null) {
            model.addAttribute("error", error);
            return "auth/register";
        }
        // self-registration always creates a tenant; the admin account is created by DataSeeder only
        users.save(new AppUser(email, passwordEncoder.encode(form.getPassword()), form.getFullName().trim(), Role.TENANT));
        // the login page fills in this email; a flash attribute keeps it out of the URL
        redirect.addFlashAttribute("registeredEmail", email);
        return "redirect:/login?registered";
    }

    private String validate(RegisterForm form, String email) {
        if (!StringUtils.hasText(form.getFullName())) {
            return messages.get("register.error.name");
        }
        if (!email.contains("@")) {
            return messages.get("register.error.email");
        }
        if (email.equalsIgnoreCase(props.getAdmin().getEmail())) {
            return messages.get("register.error.reserved");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            return messages.get("register.error.exists");
        }
        if (form.getPassword() == null || form.getPassword().length() < 6) {
            return messages.get("register.error.short");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            return messages.get("register.error.mismatch");
        }
        return null;
    }
}
