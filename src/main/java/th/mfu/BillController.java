package th.mfu;

import java.time.YearMonth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.domain.Bill;

@Controller
public class BillController {

    private final BillRepository repository;
    private final RoomRepository roomRepository;

    public BillController(BillRepository repository, RoomRepository roomRepository) {
        this.repository = repository;
        this.roomRepository = roomRepository;
    }

    @GetMapping("/bills")
    public String listBills(Model model) {
        model.addAttribute("bills", repository.findAllByOrderByBillMonthDescIdDesc());
        return "bills/list";
    }

    @GetMapping("/bills/new")
    public String addBillForm(Model model) {
        Bill bill = new Bill();
        bill.setBillMonth(YearMonth.now().toString());
        return showForm(model, bill);
    }

    @GetMapping("/bills/{id}")
    public String editBillForm(Model model, @PathVariable Long id) {
        return showForm(model, findBill(id));
    }

    @PostMapping("/bills")
    public String saveBill(@ModelAttribute Bill bill, RedirectAttributes redirect) {
        // blank rent = use the room's monthly rent
        if (bill.getRent() == null && bill.getRoom() != null) {
            bill.setRent(bill.getRoom().getMonthlyRent());
        }
        repository.save(bill);
        redirect.addFlashAttribute("success", "บันทึกบิลห้อง " + bill.getRoom().getRoomNumber() + " แล้ว");
        return "redirect:/bills";
    }

    @GetMapping("/bills/{id}/pay")
    public String togglePaid(@PathVariable Long id) {
        Bill bill = findBill(id);
        bill.setPaid(!bill.isPaid());
        repository.save(bill);
        return "redirect:/bills";
    }

    @GetMapping("/bills/{id}/delete")
    public String deleteBill(@PathVariable Long id, RedirectAttributes redirect) {
        repository.deleteById(id);
        redirect.addFlashAttribute("success", "ลบบิลแล้ว");
        return "redirect:/bills";
    }

    private String showForm(Model model, Bill bill) {
        model.addAttribute("bill", bill);
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        return "bills/form";
    }

    private Bill findBill(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
