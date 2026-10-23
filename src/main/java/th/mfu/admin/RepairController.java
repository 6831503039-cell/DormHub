package th.mfu.admin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.RepairRequestRepository;
import th.mfu.RoomRepository;
import th.mfu.domain.RepairRequest;

@Controller
@RequestMapping("/admin")
public class RepairController {

    private final RepairRequestRepository repository;
    private final RoomRepository roomRepository;

    public RepairController(RepairRequestRepository repository, RoomRepository roomRepository) {
        this.repository = repository;
        this.roomRepository = roomRepository;
    }

    @GetMapping("/repairs")
    public String listRepairs(Model model) {
        model.addAttribute("repairs", repository.findAllByOrderByReportedDateDescIdDesc());
        return "admin/repairs/list";
    }

    @GetMapping("/repairs/new")
    public String addRepairForm(Model model) {
        return showForm(model, new RepairRequest());
    }

    @GetMapping("/repairs/{id}")
    public String editRepairForm(Model model, @PathVariable Long id) {
        return showForm(model, repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @PostMapping("/repairs")
    public String saveRepair(@ModelAttribute RepairRequest repair, RedirectAttributes redirect) {
        repository.save(repair);
        redirect.addFlashAttribute("success", "บันทึกรายการแจ้งซ่อมแล้ว");
        return "redirect:/admin/repairs";
    }

    @GetMapping("/repairs/{id}/delete")
    public String deleteRepair(@PathVariable Long id, RedirectAttributes redirect) {
        repository.deleteById(id);
        redirect.addFlashAttribute("success", "ลบรายการแจ้งซ่อมแล้ว");
        return "redirect:/admin/repairs";
    }

    private String showForm(Model model, RepairRequest repair) {
        model.addAttribute("repair", repair);
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        return "admin/repairs/form";
    }
}
