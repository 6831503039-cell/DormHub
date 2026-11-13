package th.mfu.admin;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.ParcelRepository;
import th.mfu.RoomRepository;
import th.mfu.config.Messages;
import th.mfu.domain.Parcel;
import th.mfu.domain.Room;
import th.mfu.service.Images;

/**
 * Parcels at the office: the admin takes a photo, picks the room written on the label and the tenants of that
 * room see it in the app. Parcels without a room number cannot be recorded; their owners check at the office.
 */
@Controller
@RequestMapping("/admin/parcels")
public class ParcelController {

    private final ParcelRepository repository;
    private final RoomRepository roomRepository;
    private final Clock clock;
    private final Messages messages;

    public ParcelController(ParcelRepository repository, RoomRepository roomRepository, Clock clock, Messages messages) {
        this.repository = repository;
        this.roomRepository = roomRepository;
        this.clock = clock;
        this.messages = messages;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("parcels", repository.findAllByOrderByArrivedAtDesc());
        model.addAttribute("waitingCount", repository.countByPickedUpAtIsNull());
        return "admin/parcels/list";
    }

    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        return "admin/parcels/form";
    }

    @PostMapping
    public String add(@RequestParam(required = false) Room room, @RequestParam(required = false) String recipientName,
            @RequestParam(required = false) String carrier, @RequestParam(required = false) String note,
            @RequestParam(value = "photo", required = false) MultipartFile photo, RedirectAttributes redirect) throws IOException {
        if (room == null) {
            redirect.addFlashAttribute("error", messages.get("flash.parcel.noRoom"));
            return "redirect:/admin/parcels/new";
        }
        if (photo != null && !photo.isEmpty() && !Images.isImage(photo)) {
            redirect.addFlashAttribute("error", messages.get("flash.image.invalid"));
            return "redirect:/admin/parcels/new";
        }
        Parcel parcel = new Parcel();
        parcel.setRoom(room);
        parcel.setRecipientName(trimToNull(recipientName));
        parcel.setCarrier(trimToNull(carrier));
        parcel.setNote(trimToNull(note));
        parcel.setArrivedAt(LocalDateTime.now(clock));
        if (Images.isImage(photo)) {
            parcel.setPhoto(photo.getBytes());
            parcel.setPhotoContentType(photo.getContentType());
        }
        repository.save(parcel);
        redirect.addFlashAttribute("success", messages.get("flash.parcel.added", room.getRoomNumber()));
        return "redirect:/admin/parcels";
    }

    @PostMapping("/{id}/picked-up")
    public String pickedUp(@PathVariable Long id, RedirectAttributes redirect) {
        Parcel parcel = find(id);
        parcel.setPickedUpAt(LocalDateTime.now(clock));
        repository.save(parcel);
        redirect.addFlashAttribute("success", messages.get("flash.parcel.pickedUp", parcel.getRoom().getRoomNumber()));
        return "redirect:/admin/parcels";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        repository.delete(find(id));
        redirect.addFlashAttribute("success", messages.get("flash.parcel.deleted"));
        return "redirect:/admin/parcels";
    }

    private Parcel find(Long id) {
        Optional<Parcel> optParcel = repository.findById(id);
        if (!optParcel.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return optParcel.get();
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
