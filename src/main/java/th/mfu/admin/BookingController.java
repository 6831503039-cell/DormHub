package th.mfu.admin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.BookingRequestRepository;
import th.mfu.config.Messages;
import th.mfu.domain.BookingRequest;
import th.mfu.domain.BookingStatus;

/** Booking requests sent from the public 3D room map (/explore). */
@Controller
@RequestMapping("/admin")
public class BookingController {

    private final BookingRequestRepository repository;
    private final Messages messages;

    public BookingController(BookingRequestRepository repository, Messages messages) {
        this.repository = repository;
        this.messages = messages;
    }

    @GetMapping("/bookings")
    public String listBookings(Model model) {
        model.addAttribute("bookings", repository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("pendingCount", repository.countByStatus(BookingStatus.PENDING));
        return "admin/bookings/list";
    }

    @PostMapping("/bookings/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam BookingStatus status, RedirectAttributes redirect) {
        BookingRequest booking = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        booking.setStatus(status);
        repository.save(booking);
        redirect.addFlashAttribute("success", messages.get("flash.booking.status",
                booking.getRoom().getRoomNumber(), booking.getFullName(), messages.get(status.getKey())));
        return "redirect:/admin/bookings";
    }
}
