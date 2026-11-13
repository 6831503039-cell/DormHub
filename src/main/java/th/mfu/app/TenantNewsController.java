package th.mfu.app;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            List<Parcel> waiting = new ArrayList<>();
            List<Parcel> pickedUp = new ArrayList<>();
            for (Parcel parcel : parcels) {
                if (parcel.isWaiting()) {
                    waiting.add(parcel);
                } else if (pickedUp.size() < 10) { // only the latest 10
                    pickedUp.add(parcel);
                }
            }
            model.addAttribute("waiting", waiting);
            model.addAttribute("pickedUp", pickedUp);
        }
        return "app/parcels";
    }

    /** A tenant confirms they took a parcel of their own room from the office. */
    @PostMapping("/app/parcels/{id}/picked-up")
    public String pickedUp(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        AppUser user = currentUser.user(principal);
        Optional<Parcel> optParcel = parcelRepository.findById(id);
        if (!optParcel.isPresent() || !isOwnRoom(user, optParcel.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Parcel parcel = optParcel.get();
        if (parcel.isWaiting()) {
            parcel.setPickedUpAt(LocalDateTime.now(clock));
            parcel.setPickedUpBy(user.getFullName());
            parcelRepository.save(parcel);
        }
        redirect.addFlashAttribute("success", messages.get("flash.parcel.confirmed"));
        return "redirect:/app/parcels";
    }

    private boolean isOwnRoom(AppUser user, Parcel parcel) {
        Optional<Tenant> tenant = currentUser.tenant(user);
        if (!tenant.isPresent()) {
            return false;
        }
        Tenant t = tenant.get();
        return t.getRoom() != null && t.getRoom().getId().equals(parcel.getRoom().getId());
    }

    // pictures: an announcement's for any logged-in user; a parcel photo only for the admin and that room's tenants

    @GetMapping("/announcements/{id}/image")
    public ResponseEntity<byte[]> announcementImage(@PathVariable Long id) {
        Optional<Announcement> optAnnouncement = announcementRepository.findById(id);
        if (!optAnnouncement.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Announcement a = optAnnouncement.get();
        return Images.response(a.getImage(), a.getImageContentType());
    }

    @GetMapping("/parcels/{id}/photo")
    public ResponseEntity<byte[]> parcelPhoto(@PathVariable Long id, Principal principal) {
        Optional<Parcel> photoParcel = parcelRepository.findById(id);
        if (!photoParcel.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Parcel parcel = photoParcel.get();
        AppUser user = currentUser.user(principal);
        if (!user.isAdmin() && !isOwnRoom(user, parcel)) {
            // same answer as a missing parcel, so other rooms' parcels cannot be discovered
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Images.response(parcel.getPhoto(), parcel.getPhotoContentType());
    }
}
