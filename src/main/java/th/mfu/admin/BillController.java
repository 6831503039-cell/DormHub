package th.mfu.admin;

import java.time.Clock;
import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Autowired;
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

import th.mfu.BillRepository;
import th.mfu.PaymentRepository;
import th.mfu.RoomRepository;
import th.mfu.domain.Bill;
import th.mfu.service.BillingService;

@Controller
@RequestMapping("/admin")
public class BillController {

    @Autowired
    private BillRepository repository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private BillingService billingService;
    @Autowired
    private Clock clock;

    @GetMapping("/bills")
    public String listBills(Model model) {
        model.addAttribute("bills", repository.findAllByOrderByBillMonthDescIdDesc());
        return "admin/bills/list";
    }

    @GetMapping("/bills/new")
    public String addBillForm(Model model) {
        Bill bill = new Bill();
        bill.setBillMonth(YearMonth.now(clock).toString());
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
        if (bill.getDueDate() == null) {
            bill.setDueDate(billingService.defaultDueDate(bill.getBillMonth()));
        }
        repository.save(bill);
        redirect.addFlashAttribute("success", "บันทึกบิลห้อง " + bill.getRoom().getRoomNumber() + " แล้ว");
        return "redirect:/admin/bills";
    }

    @GetMapping("/bills/{id}/pay")
    public String togglePaid(@PathVariable Long id) {
        Bill bill = findBill(id);
        bill.setPaid(!bill.isPaid());
        repository.save(bill);
        return "redirect:/admin/bills";
    }

    @GetMapping("/bills/{id}/delete")
    public String deleteBill(@PathVariable Long id, RedirectAttributes redirect) {
        Bill bill = findBill(id);
        paymentRepository.deleteAll(paymentRepository.findByBillOrderBySubmittedAtDesc(bill));
        repository.delete(bill);
        redirect.addFlashAttribute("success", "ลบบิลแล้ว");
        return "redirect:/admin/bills";
    }

    private String showForm(Model model, Bill bill) {
        model.addAttribute("bill", bill);
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        return "admin/bills/form";
    }

    private Bill findBill(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
