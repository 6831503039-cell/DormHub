package th.mfu.admin;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private FeedbackRepository repository;

    @GetMapping
    public String listFeedback(Model model) {
        model.addAttribute("feedbacks", repository.findAllByOrderByCreatedAtDesc());
        return "admin/feedback/list";
    }

    @GetMapping("/{id}/read")
    public String toggleRead(@PathVariable Long id) {
        Feedback feedback = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        feedback.setReadByOwner(!feedback.isReadByOwner());
        repository.save(feedback);
        return "redirect:/admin/feedback";
    }
}
