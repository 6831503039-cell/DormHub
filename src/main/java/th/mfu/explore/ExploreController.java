package th.mfu.explore;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.BookingRequestRepository;
import th.mfu.domain.BookingRequest;
import th.mfu.domain.BookingStatus;
import th.mfu.domain.Room;
import th.mfu.service.RoomMapService;
import th.mfu.service.RoomView;

/** Public 3D room map: anyone can look at free rooms and send a booking request without logging in. */
@Controller
public class ExploreController {

    private final RoomMapService roomMapService;
    private final BookingRequestRepository bookingRepository;
    private final Clock clock;

    public ExploreController(RoomMapService roomMapService, BookingRequestRepository bookingRepository, Clock clock) {
        this.roomMapService = roomMapService;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        model.addAttribute("soonDays", RoomMapService.SOON_DAYS);
        return "explore/index";
    }

    /** Room data for static/js/dorm3d.js (no tenant names). */
    @GetMapping("/explore/rooms")
    @ResponseBody
    public List<RoomView> rooms() {
        return roomMapService.rooms(false);
    }

    @PostMapping("/explore/book")
    public String book(@ModelAttribute BookingRequest booking, RedirectAttributes redirect) {
        Room room = booking.getRoom();
        if (room == null) {
            redirect.addFlashAttribute("error", "ไม่พบห้องที่เลือก กรุณาเลือกห้องใหม่อีกครั้ง");
            return "redirect:/explore";
        }
        String back = "redirect:/explore?room=" + room.getRoomNumber();

        if (!StringUtils.hasText(booking.getFullName()) || !StringUtils.hasText(booking.getPhone())) {
            redirect.addFlashAttribute("error", "กรุณากรอกชื่อและเบอร์โทรศัพท์");
            return back;
        }
        if (!roomMapService.canBook(room)) {
            redirect.addFlashAttribute("error", "ห้อง " + room.getRoomNumber() + " ไม่ว่างแล้ว ลองเลือกห้องอื่น");
            return back;
        }
        LocalDate from = roomMapService.availableFrom(room);
        if (booking.getMoveInDate() == null || booking.getMoveInDate().isBefore(from)) {
            redirect.addFlashAttribute("error", "ห้อง " + room.getRoomNumber() + " เข้าอยู่ได้ตั้งแต่วันที่ " + from + " เป็นต้นไป");
            return back;
        }

        // values the visitor must not choose themselves
        booking.setId(null);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now(clock));
        booking.setFullName(booking.getFullName().trim());
        booking.setPhone(booking.getPhone().trim());
        bookingRepository.save(booking);

        redirect.addFlashAttribute("success", "ส่งคำขอจองห้อง " + room.getRoomNumber()
                + " แล้ว เจ้าหน้าที่หอจะติดต่อกลับที่เบอร์ " + booking.getPhone());
        return back;
    }
}
