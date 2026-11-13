package th.mfu.app;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.FeedbackRepository;
import th.mfu.RepairRequestRepository;
import th.mfu.config.Messages;
import th.mfu.domain.AppUser;
import th.mfu.domain.Feedback;
import th.mfu.domain.RepairRequest;
import th.mfu.domain.Tenant;
import th.mfu.service.CurrentUserService;

/** Tenant requests: repair reports and feedback for the owner. */
@Controller
public class TenantRequestController {

    private final CurrentUserService currentUser;
    private final RepairRequestRepository repairRepository;
    private final FeedbackRepository feedbackRepository;
    private final Clock clock;
    private final Messages messages;

    public TenantRequestController(CurrentUserService currentUser, RepairRequestRepository repairRepository,
            FeedbackRepository feedbackRepository, Clock clock, Messages messages) {
        this.currentUser = currentUser;
        this.repairRepository = repairRepository;
        this.feedbackRepository = feedbackRepository;
        this.clock = clock;
        this.messages = messages;
    }

    @GetMapping("/app/requests")
    public String requests(Principal principal, Model model) {
        AppUser user = currentUser.user(principal);
        Optional<Tenant> tenant = currentUser.tenant(user);
        model.addAttribute("tenant", tenant.orElse(null));
        if (tenant.isPresent() && tenant.get().getRoom() != null) {
            model.addAttribute("repairs", repairRepository.findByRoomOrderByReportedDateDescIdDesc(tenant.get().getRoom()));
        }
        model.addAttribute("feedbacks", feedbackRepository.findByAuthorOrderByCreatedAtDesc(user));
        model.addAttribute("categories", Feedback.CATEGORIES);
        return "app/requests";
    }

    @PostMapping("/app/repairs")
    public String reportRepair(@RequestParam String title, @RequestParam(required = false) String description,
            Principal principal, RedirectAttributes redirect) {
        AppUser user = currentUser.user(principal);
        Optional<Tenant> tenant = currentUser.tenant(user);
        if (tenant.isEmpty() || tenant.get().getRoom() == null || !StringUtils.hasText(title)) {
            redirect.addFlashAttribute("error", messages.get("flash.repair.failed"));
            return "redirect:/app/requests";
        }
        RepairRequest repair = new RepairRequest();
        repair.setRoom(tenant.get().getRoom());
        repair.setTitle(title.trim());
        repair.setDescription(description);
        repair.setReportedDate(LocalDate.now(clock));
        repair.setReportedBy(user.getFullName());
        repairRepository.save(repair);
        redirect.addFlashAttribute("success", messages.get("flash.repair.sent"));
        return "redirect:/app/requests";
    }

    @PostMapping("/app/feedback")
    public String sendFeedback(@RequestParam String category, @RequestParam int rating, @RequestParam String message,
            Principal principal, RedirectAttributes redirect) {
        if (!StringUtils.hasText(message)) {
            redirect.addFlashAttribute("error", messages.get("flash.feedback.empty"));
            return "redirect:/app/requests";
        }
        AppUser user = currentUser.user(principal);
        Feedback feedback = new Feedback();
        feedback.setAuthor(user);
        Optional<Tenant> tenant = currentUser.tenant(user);
        if (tenant.isPresent()) {
            feedback.setRoom(tenant.get().getRoom());
        }
        feedback.setCategory(category);
        feedback.setRating(Math.max(1, Math.min(5, rating)));
        feedback.setMessage(message.trim());
        feedback.setCreatedAt(LocalDateTime.now(clock));
        feedbackRepository.save(feedback);
        redirect.addFlashAttribute("success", messages.get("flash.feedback.sent"));
        return "redirect:/app/requests";
    }
}
