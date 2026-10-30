package th.mfu.app;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.AnnouncementRepository;
import th.mfu.ParcelRepository;
import th.mfu.config.Messages;
import th.mfu.domain.Announcement;
import th.mfu.domain.AppUser;
import th.mfu.domain.Parcel;
import th.mfu.domain.Tenant;
import th.mfu.service.CurrentUserService;
import th.mfu.service.Images;

/** Announcements for everyone and parcels for the tenant's own room, plus their pictures. */
@Controller
public class TenantNewsController {

    private final CurrentUserService currentUser;
    private final AnnouncementRepository announcementRepository;
    private final ParcelRepository parcelRepository;

    private final Clock clock;
    private final Messages messages;

    public TenantNewsController(CurrentUserService currentUser, AnnouncementRepository announcementRepository,
            ParcelRepository parcelRepository, Clock clock, Messages messages) {
        this.currentUser = currentUser;
        this.announcementRepository = announcementRepository;
        this.parcelRepository = parcelRepository;
        this.clock = clock;
        this.messages = messages;
    }

    @GetMapping("/app/announcements")
    public String announcements(Model model) {
        model.addAttribute("announcements", announcementRepository.findAllByOrderByPinnedDescCreatedAtDesc());
        return "app/announcements";
    }

    @GetMapping("/app/parcels")
    public String parcels(Principal principal, Model model) {
        Optional<Tenant> tenant = currentUser.tenant(currentUser.user(principal));
        model.addAttribute("tenant", tenant.orElse(null));
        if (tenant.isPresent() && tenant.get().getRoom() != null) {
            List<Parcel> parcels = parcelRepository.findByRoomOrderByArrivedAtDesc(tenant.get().getRoom());
            model.addAttribute("waiting", parcels.stream().filter(Parcel::isWaiting).collect(Collectors.toList()));
            model.addAttribute("pickedUp", parcels.stream().filter(p -> !p.isWaiting()).limit(10).collect(Collectors.toList()));
        }
        return "app/parcels";
    }

    /** A tenant confirms they took a parcel of their own room from the office. */
    @PostMapping("/app/parcels/{id}/picked-up")
    public String pickedUp(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        AppUser user = currentUser.user(principal);
        Parcel parcel = parcelRepository.findById(id)
                .filter(p -> isOwnRoom(user, p))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (parcel.isWaiting()) {
            parcel.setPickedUpAt(LocalDateTime.now(clock));
            parcel.setPickedUpBy(user.getFullName());
            parcelRepository.save(parcel);
        }
        redirect.addFlashAttribute("success", messages.get("flash.parcel.confirmed"));
        return "redirect:/app/parcels";
    }

    private boolean isOwnRoom(AppUser user, Parcel parcel) {
        return currentUser.tenant(user)
                .map(t -> t.getRoom() != null && t.getRoom().getId().equals(parcel.getRoom().getId()))
                .orElse(false);
    }

    // pictures: an announcement's for any logged-in user; a parcel photo only for the admin and that room's tenants

    @GetMapping("/announcements/{id}/image")
    public ResponseEntity<byte[]> announcementImage(@PathVariable Long id) {
        Announcement a = announcementRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Images.response(a.getImage(), a.getImageContentType());
    }

    @GetMapping("/parcels/{id}/photo")
    public ResponseEntity<byte[]> parcelPhoto(@PathVariable Long id, Principal principal) {
        Parcel parcel = parcelRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AppUser user = currentUser.user(principal);
        if (!user.isAdmin() && !isOwnRoom(user, parcel)) {
            // same answer as a missing parcel, so other rooms' parcels cannot be discovered
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Images.response(parcel.getPhoto(), parcel.getPhotoContentType());
    }
}
