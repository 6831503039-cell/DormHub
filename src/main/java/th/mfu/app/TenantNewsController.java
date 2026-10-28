package th.mfu.app;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import th.mfu.AnnouncementRepository;
import th.mfu.ParcelRepository;
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

    public TenantNewsController(CurrentUserService currentUser, AnnouncementRepository announcementRepository,
            ParcelRepository parcelRepository) {
        this.currentUser = currentUser;
        this.announcementRepository = announcementRepository;
        this.parcelRepository = parcelRepository;
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
        boolean ownRoom = currentUser.tenant(user)
                .map(t -> t.getRoom() != null && t.getRoom().getId().equals(parcel.getRoom().getId()))
                .orElse(false);
        if (!user.isAdmin() && !ownRoom) {
            // same answer as a missing parcel, so other rooms' parcels cannot be discovered
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return Images.response(parcel.getPhoto(), parcel.getPhotoContentType());
    }
}
