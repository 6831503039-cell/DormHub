package th.mfu.explore;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.config.Messages;
import th.mfu.domain.Bill;
import th.mfu.domain.BookingRequest;
import th.mfu.domain.Room;
import th.mfu.service.BookingService;
import th.mfu.service.RoomMapService;
import th.mfu.service.RoomView;

/** Public 3D room map: anyone can look at free rooms and send a booking request without logging in. */
@Controller
public class ExploreController {

    private final RoomMapService roomMapService;
    private final BookingService bookingService;
    private final Messages messages;

    public ExploreController(RoomMapService roomMapService, BookingService bookingService, Messages messages) {
        this.roomMapService = roomMapService;
        this.bookingService = bookingService;
        this.messages = messages;
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        addMapSettings(model);
        return "explore/index";
    }

    /** Settings for static/js/dorm3d.js, shared with the tenant app's rent page. */
    public static void addMapSettings(Model model) {
        model.addAttribute("soonDays", RoomMapService.SOON_DAYS);
        // the default rates of a new bill are the dorm's rates
        Bill rates = new Bill();
        model.addAttribute("electricRate", rates.getElectricRate());
        model.addAttribute("waterRate", rates.getWaterRate());
    }

    /** Room data for static/js/dorm3d.js (no tenant names). */
    @GetMapping("/explore/rooms")
    public ResponseEntity<List<RoomView>> rooms() {
        return new ResponseEntity<List<RoomView>>(roomMapService.rooms(false), HttpStatus.OK);
    }

    @PostMapping("/explore/book")
    public String book(@ModelAttribute BookingRequest booking, RedirectAttributes redirect) {
        Room room = booking.getRoom();
        if (room == null) {
            redirect.addFlashAttribute("error", messages.get("flash.book.noRoom"));
            return "redirect:/explore";
        }
        String back = "redirect:/explore?room=" + room.getRoomNumber();
        Optional<String> problem = bookingService.problem(booking);
        if (problem.isPresent()) {
            redirect.addFlashAttribute("error", problem.get());
            return back;
        }
        booking.setApplicant(null);
        bookingService.submit(booking);
        redirect.addFlashAttribute("success", messages.get("flash.book.sent", room.getRoomNumber(), booking.getPhone()));
        return back;
    }
}
