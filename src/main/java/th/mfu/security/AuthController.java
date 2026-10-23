package th.mfu.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import th.mfu.AppUserRepository;
import th.mfu.config.DormProperties;
import th.mfu.domain.AppUser;
import th.mfu.domain.Role;

@Controller
public class AuthController {

    @Autowired
    private AppUserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DormProperties props;

    // "/" is the landing page after login: admins go to the dashboard, everyone else to the tenant app
    @GetMapping("/")
    public String home(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
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
    public String register(@ModelAttribute("form") RegisterForm form, Model model) {
        String email = form.getEmail() == null ? "" : form.getEmail().trim().toLowerCase();
        String error = validate(form, email);
        if (error != null) {
            model.addAttribute("error", error);
            return "auth/register";
        }
        // self-registration always creates a tenant; the admin account is created by DataSeeder only
        users.save(new AppUser(email, passwordEncoder.encode(form.getPassword()), form.getFullName().trim(), Role.TENANT));
        return "redirect:/login?registered";
    }

    private String validate(RegisterForm form, String email) {
        if (!StringUtils.hasText(form.getFullName())) {
            return "กรุณากรอกชื่อ-นามสกุล";
        }
        if (!email.contains("@")) {
            return "รูปแบบอีเมลไม่ถูกต้อง";
        }
        if (email.equalsIgnoreCase(props.getAdmin().getEmail())) {
            return "อีเมลนี้สงวนไว้สำหรับผู้ดูแลหอพัก ไม่สามารถสมัครได้";
        }
        if (users.existsByEmailIgnoreCase(email)) {
            return "อีเมลนี้มีบัญชีอยู่แล้ว";
        }
        if (form.getPassword() == null || form.getPassword().length() < 6) {
            return "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            return "รหัสผ่านทั้งสองช่องไม่ตรงกัน";
        }
        return null;
    }
}
