package th.mfu.admin;

import java.util.Optional;

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
import th.mfu.domain.Tenant;
import th.mfu.service.BookingService;

/** Booking requests sent from the 3D room map (public /explore page and the tenant app's /app/rent). */
@Controller
@RequestMapping("/admin")
public class BookingController {

    private final BookingRequestRepository repository;
    private final BookingService bookingService;
    private final Messages messages;

    public BookingController(BookingRequestRepository repository, BookingService bookingService, Messages messages) {
        this.repository = repository;
        this.bookingService = bookingService;
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
        Optional<BookingRequest> optBookingRequest = repository.findById(id);
        if (!optBookingRequest.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        BookingRequest booking = optBookingRequest.get();
        booking.setStatus(status);
        repository.save(booking);
        String message = messages.get("flash.booking.status",
                booking.getRoom().getRoomNumber(), booking.getFullName(), messages.get(status.getKey()));
        if (status == BookingStatus.CONFIRMED && booking.getApplicant() != null) {
            // sent from the app: the account becomes a tenant of the room (unless it already is one or the room is full)
            Optional<Tenant> tenant = bookingService.moveIn(booking);
            message += " " + (tenant.isPresent()
                    ? messages.get("flash.booking.linked", booking.getApplicant().getEmail(), booking.getRoom().getRoomNumber())
                    : messages.get("flash.booking.notLinked"));
        }
        redirect.addFlashAttribute("success", message);
        return "redirect:/admin/bookings";
    }
}
