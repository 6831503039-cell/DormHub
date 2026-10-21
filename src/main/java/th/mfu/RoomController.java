package th.mfu;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.domain.Room;

@Controller
public class RoomController {

    private final RoomRepository repository;

    public RoomController(RoomRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/rooms")
    public String listRooms(Model model) {
        model.addAttribute("rooms", repository.findAllByOrderByRoomNumberAsc());
        return "rooms/list";
    }

    @GetMapping("/rooms/new")
    public String addRoomForm(Model model) {
        model.addAttribute("room", new Room());
        return "rooms/form";
    }

    @GetMapping("/rooms/{id}")
    public String editRoomForm(Model model, @PathVariable Long id) {
        model.addAttribute("room", repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "rooms/form";
    }

    @PostMapping("/rooms")
    public String saveRoom(@ModelAttribute Room room, RedirectAttributes redirect) {
        try {
            repository.save(room);
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "เลขห้อง " + room.getRoomNumber() + " มีอยู่แล้ว");
            return room.getId() == null ? "redirect:/rooms/new" : "redirect:/rooms/" + room.getId();
        }
        redirect.addFlashAttribute("success", "บันทึกห้อง " + room.getRoomNumber() + " แล้ว");
        return "redirect:/rooms";
    }

    @GetMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            repository.deleteById(id);
            redirect.addFlashAttribute("success", "ลบห้องแล้ว");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "ลบไม่ได้ เพราะห้องนี้ยังมีผู้เช่า บิล หรือรายการแจ้งซ่อมอยู่");
        }
        return "redirect:/rooms";
    }
}
