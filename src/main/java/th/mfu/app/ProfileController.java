package th.mfu.app;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.AppUserRepository;
import th.mfu.TenantRepository;
import th.mfu.config.Messages;
import th.mfu.domain.AppUser;
import th.mfu.domain.Tenant;
import th.mfu.service.CurrentUserService;
import th.mfu.service.Images;

/** "My profile" in the tenant app: name, phone, profile picture and password. */
@Controller
public class ProfileController {

    static final long MAX_AVATAR_BYTES = 2 * 1024 * 1024;
    static final int MIN_PASSWORD_LENGTH = 6;

    private final CurrentUserService currentUser;
    private final AppUserRepository users;
    private final TenantRepository tenants;
    private final PasswordEncoder passwordEncoder;
    private final Messages messages;

    public ProfileController(CurrentUserService currentUser, AppUserRepository users, TenantRepository tenants,
            PasswordEncoder passwordEncoder, Messages messages) {
        this.currentUser = currentUser;
        this.users = users;
        this.tenants = tenants;
        this.passwordEncoder = passwordEncoder;
        this.messages = messages;
    }

    @GetMapping("/app/profile")
    public String profile(Principal principal, Model model) {
        AppUser user = currentUser.user(principal);
        model.addAttribute("tenant", currentUser.tenant(user).orElse(null));
        return "app/profile";
    }

    @PostMapping("/app/profile")
    public String save(@RequestParam String fullName, @RequestParam(required = false) String phone,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam(defaultValue = "false") boolean removeAvatar,
            Principal principal, RedirectAttributes redirect) throws IOException {
        AppUser user = currentUser.user(principal);
        if (!StringUtils.hasText(fullName)) {
            redirect.addFlashAttribute("error", messages.get("flash.profile.nameMissing"));
            return "redirect:/app/profile";
        }
        if (avatar != null && !avatar.isEmpty()) {
            if (!Images.isImage(avatar)) {
                redirect.addFlashAttribute("error", messages.get("flash.profile.notImage"));
                return "redirect:/app/profile";
            }
            if (avatar.getSize() > MAX_AVATAR_BYTES) {
                redirect.addFlashAttribute("error", messages.get("flash.profile.tooBig"));
                return "redirect:/app/profile";
            }
            user.setAvatar(avatar.getBytes());
            user.setAvatarContentType(avatar.getContentType());
            user.setAvatarVersion(user.getAvatarVersion() + 1);
        } else if (removeAvatar) {
            user.setAvatar(null);
            user.setAvatarContentType(null);
            user.setAvatarVersion(user.getAvatarVersion() + 1);
        }
        user.setFullName(fullName.trim());
        users.save(user);

        // the phone number lives on the tenant record the admin also sees
        Optional<Tenant> tenant = currentUser.tenant(user);
        if (tenant.isPresent() && phone != null) {
            tenant.get().setPhone(phone.trim());
            tenants.save(tenant.get());
        }
        redirect.addFlashAttribute("success", messages.get("flash.profile.saved"));
        return "redirect:/app/profile";
    }

    @PostMapping("/app/profile/password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
            @RequestParam String confirmPassword, Principal principal, RedirectAttributes redirect) {
        AppUser user = currentUser.user(principal);
        String error = null;
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            error = messages.get("flash.profile.wrongPassword");
        } else if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            error = messages.get("flash.profile.shortPassword", MIN_PASSWORD_LENGTH);
        } else if (!newPassword.equals(confirmPassword)) {
            error = messages.get("flash.profile.passwordMismatch");
        }
        if (error != null) {
            redirect.addFlashAttribute("error", error);
            return "redirect:/app/profile";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        users.save(user);
        redirect.addFlashAttribute("success", messages.get("flash.profile.passwordChanged"));
        return "redirect:/app/profile";
    }

    @GetMapping("/app/profile/avatar")
    public ResponseEntity<byte[]> avatar(Principal principal) {
        AppUser user = currentUser.user(principal);
        return Images.response(user.getAvatar(), user.getAvatarContentType());
    }
}
