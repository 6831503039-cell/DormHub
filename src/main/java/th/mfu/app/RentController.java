package th.mfu.app;

import java.security.Principal;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.BookingRequestRepository;
import th.mfu.config.Messages;
import th.mfu.domain.AppUser;
import th.mfu.domain.BookingRequest;
import th.mfu.domain.BookingStatus;
import th.mfu.domain.Room;
import th.mfu.domain.Tenant;
import th.mfu.explore.ExploreController;
import th.mfu.service.BookingService;
import th.mfu.service.CurrentUserService;

/**
 * Renting a room from inside the app: an account without a room picks one on the 3D map and sends a request.
 * When the admin confirms it, BookingService.moveIn links the account to the room.
 */
@Controller
public class RentController {

    private final CurrentUserService currentUser;
    private final BookingRequestRepository bookingRepository;
    private final BookingService bookingService;
    private final Messages messages;

    public RentController(CurrentUserService currentUser, BookingRequestRepository bookingRepository,
            BookingService bookingService, Messages messages) {
        this.currentUser = currentUser;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.messages = messages;
    }

    @GetMapping("/app/rent")
    public String rent(Principal principal, Model model) {
        AppUser user = currentUser.user(principal);
        Optional<Tenant> tenant = currentUser.tenant(user);
        model.addAttribute("tenant", tenant.orElse(null));
        model.addAttribute("myBookings", bookingRepository.findByApplicantOrderByCreatedAtDesc(user));
        ExploreController.addMapSettings(model);
        return "app/rent";
    }

    @PostMapping("/app/rent/book")
    public String book(@ModelAttribute BookingRequest booking, Principal principal, RedirectAttributes redirect) {
        AppUser user = currentUser.user(principal);
        Room room = booking.getRoom();
        if (room == null) {
            redirect.addFlashAttribute("error", messages.get("flash.book.noRoom"));
            return "redirect:/app/rent";
        }
        Optional<Tenant> tenant = currentUser.tenant(user);
        if (tenant.isPresent() && tenant.get().getRoom() != null) {
            redirect.addFlashAttribute("error", messages.get("flash.rent.hasRoom", tenant.get().getRoom().getRoomNumber()));
            return "redirect:/app/rent";
        }
        // one open request at a time; the admin confirms or cancels it first
        if (bookingRepository.findByApplicantOrderByCreatedAtDesc(user).stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.PENDING)) {
            redirect.addFlashAttribute("error", messages.get("flash.rent.pending"));
            return "redirect:/app/rent";
        }
        booking.setApplicant(user);
        // the account's email links the tenant record later, whatever was typed in the form
        booking.setEmail(user.getEmail());
        if (!StringUtils.hasText(booking.getFullName())) {
            booking.setFullName(user.getFullName());
        }
        Optional<String> problem = bookingService.problem(booking);
        if (problem.isPresent()) {
            redirect.addFlashAttribute("error", problem.get());
            return "redirect:/app/rent?room=" + room.getRoomNumber();
        }
        bookingService.submit(booking);
        // shown as a dialog in the middle of the screen (fragments/ui :: bookingSent)
        redirect.addFlashAttribute("bookingSent", room.getRoomNumber());
        return "redirect:/app/rent";
    }
}
