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

/** The tenant's own profile: name, phone, profile photo and password. */
@Controller
public class ProfileController {

    static final long MAX_PHOTO_BYTES = 2 * 1024 * 1024;

    private final CurrentUserService currentUser;
    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final Messages messages;

    public ProfileController(CurrentUserService currentUser, AppUserRepository userRepository,
            TenantRepository tenantRepository, PasswordEncoder passwordEncoder, Messages messages) {
        this.currentUser = currentUser;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.messages = messages;
    }

    @GetMapping("/app/profile")
    public String profile(Principal principal, Model model) {
        AppUser user = currentUser.user(principal);
        Optional<Tenant> tenant = currentUser.tenant(user);
        model.addAttribute("tenant", tenant.orElse(null));
        // phone: the one saved on the profile, else the one the admin recorded for the tenant
        String phone = user.getPhone();
        if (!StringUtils.hasText(phone) && tenant.isPresent()) {
            phone = tenant.get().getPhone();
        }
        model.addAttribute("phone", phone);
        return "app/profile";
    }

    @PostMapping("/app/profile")
    public String save(@RequestParam String fullName, @RequestParam(required = false) String phone,
            @RequestParam(required = false) MultipartFile photo, @RequestParam(defaultValue = "false") boolean removePhoto,
            Principal principal, RedirectAttributes redirect) throws IOException {
        AppUser user = currentUser.user(principal);
        if (!StringUtils.hasText(fullName)) {
            redirect.addFlashAttribute("error", messages.get("flash.profile.nameRequired"));
            return "redirect:/app/profile";
        }
        if (photo != null && !photo.isEmpty() && (!Images.isImage(photo) || photo.getSize() > MAX_PHOTO_BYTES)) {
            redirect.addFlashAttribute("error", messages.get("flash.profile.photoInvalid"));
            return "redirect:/app/profile";
        }

        user.setFullName(fullName.trim());
        user.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
        if (Images.isImage(photo)) {
            user.setPhoto(photo.getBytes());
            user.setPhotoType(photo.getContentType());
        } else if (removePhoto) {
            user.setPhoto(null);
            user.setPhotoType(null);
        }
        userRepository.save(user);

        // keep the phone the admin sees in the tenant list up to date
        Optional<Tenant> tenant = currentUser.tenant(user);
        if (tenant.isPresent() && StringUtils.hasText(phone)) {
            tenant.get().setPhone(phone.trim());
            tenantRepository.save(tenant.get());
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
            error = "flash.password.wrong";
        } else if (newPassword == null || newPassword.length() < 6) {
            error = "flash.password.short";
        } else if (!newPassword.equals(confirmPassword)) {
            error = "flash.password.mismatch";
        }
        if (error != null) {
            redirect.addFlashAttribute("error", messages.get(error));
            return "redirect:/app/profile";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redirect.addFlashAttribute("success", messages.get("flash.password.changed"));
        return "redirect:/app/profile";
    }

    /** The current user's profile photo, shown in the app header. */
    @GetMapping("/app/profile/photo")
    public ResponseEntity<byte[]> photo(Principal principal) {
        AppUser user = currentUser.user(principal);
        return Images.response(user.getPhoto(), user.getPhotoType());
    }
}
