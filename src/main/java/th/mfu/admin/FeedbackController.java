package th.mfu.admin;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import th.mfu.FeedbackRepository;
import th.mfu.domain.Feedback;

@Controller
@RequestMapping("/admin/feedback")
public class FeedbackController {

    private final FeedbackRepository repository;

    public FeedbackController(FeedbackRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String listFeedback(Model model) {
        model.addAttribute("feedbacks", repository.findAllByOrderByCreatedAtDesc());
        return "admin/feedback/list";
    }

    @GetMapping("/{id}/read")
    public String toggleRead(@PathVariable Long id) {
        Optional<Feedback> optFeedback = repository.findById(id);
        if (!optFeedback.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Feedback feedback = optFeedback.get();
        feedback.setReadByOwner(!feedback.isReadByOwner());
        repository.save(feedback);
        return "redirect:/admin/feedback";
    }
}
