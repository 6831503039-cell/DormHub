package th.mfu.admin;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

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

import th.mfu.AnnouncementRepository;
import th.mfu.config.Messages;
import th.mfu.domain.Announcement;
import th.mfu.service.Images;

/** The admin writes announcements (with an optional picture) that every tenant sees in the app. */
@Controller
@RequestMapping("/admin/announcements")
public class AnnouncementController {

    private final AnnouncementRepository repository;
    private final Clock clock;
    private final Messages messages;

    public AnnouncementController(AnnouncementRepository repository, Clock clock, Messages messages) {
        this.repository = repository;
        this.clock = clock;
        this.messages = messages;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("announcements", repository.findAllByOrderByPinnedDescCreatedAtDesc());
        return "admin/announcements/list";
    }

    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("announcement", new Announcement());
        return "admin/announcements/form";
    }

    @GetMapping("/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("announcement", find(id));
        return "admin/announcements/form";
    }

    @PostMapping
    public String save(@RequestParam(required = false) Long id, @RequestParam String title,
            @RequestParam(required = false) String body, @RequestParam(defaultValue = "false") boolean pinned,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(defaultValue = "false") boolean removeImage, RedirectAttributes redirect) throws IOException {
        String back = id == null ? "redirect:/admin/announcements/new" : "redirect:/admin/announcements/" + id;
        if (!StringUtils.hasText(title)) {
            redirect.addFlashAttribute("error", messages.get("flash.announcement.noTitle"));
            return back;
        }
        if (image != null && !image.isEmpty() && !Images.isImage(image)) {
            redirect.addFlashAttribute("error", messages.get("flash.image.invalid"));
            return back;
        }
        Announcement a = id == null ? new Announcement() : find(id);
        if (a.getCreatedAt() == null) {
            a.setCreatedAt(LocalDateTime.now(clock));
        }
        a.setTitle(title.trim());
        a.setBody(StringUtils.hasText(body) ? body.trim() : null);
        a.setPinned(pinned);
        if (Images.isImage(image)) {
            a.setImage(image.getBytes());
            a.setImageContentType(image.getContentType());
        } else if (removeImage) {
            a.setImage(null);
            a.setImageContentType(null);
        }
        repository.save(a);
        redirect.addFlashAttribute("success", messages.get(id == null ? "flash.announcement.posted" : "flash.announcement.saved"));
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        repository.delete(find(id));
        redirect.addFlashAttribute("success", messages.get("flash.announcement.deleted"));
        return "redirect:/admin/announcements";
    }

    private Announcement find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
