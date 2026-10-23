package th.mfu.app;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.BillRepository;
import th.mfu.PaymentRepository;
import th.mfu.config.DormProperties;
import th.mfu.domain.Bill;
import th.mfu.domain.Payment;
import th.mfu.domain.PaymentStatus;
import th.mfu.domain.Tenant;
import th.mfu.service.BillingService;
import th.mfu.service.CurrentUserService;
import th.mfu.service.PaymentService;

@Controller
public class TenantBillController {

    @Autowired
    private CurrentUserService currentUser;
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private BillingService billingService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private DormProperties props;

    @GetMapping("/app/bills")
    public String bills(Principal principal, Model model) {
        Optional<Tenant> tenant = currentUser.tenant(currentUser.user(principal));
        model.addAttribute("tenant", tenant.orElse(null));
        if (tenant.isEmpty() || tenant.get().getRoom() == null) {
            return "app/bills";
        }
        List<TenantBillView> unpaid = new ArrayList<>();
        List<TenantBillView> history = new ArrayList<>();
        for (Bill bill : billRepository.findByRoomOrderByBillMonthDesc(tenant.get().getRoom())) {
            TenantBillView view = view(bill);
            if (bill.isPaid()) {
                if (history.size() < 6) {
                    history.add(view);
                }
            } else {
                unpaid.add(view);
            }
        }
        model.addAttribute("unpaid", unpaid);
        model.addAttribute("history", history);
        model.addAttribute("promptpayId", props.getPromptpayId());
        model.addAttribute("promptpayName", props.getPromptpayName());
        return "app/bills";
    }

    @PostMapping("/app/bills/{id}/pay")
    public String pay(@PathVariable Long id, @RequestParam double amount, @RequestParam("slip") MultipartFile slip,
            Principal principal, RedirectAttributes redirect) throws IOException {
        Tenant tenant = currentUser.tenant(currentUser.user(principal))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        Bill bill = billRepository.findById(id)
                .filter(b -> tenant.getRoom() != null && b.getRoom().getId().equals(tenant.getRoom().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (slip.isEmpty() || amount <= 0) {
            redirect.addFlashAttribute("error", "กรุณาแนบรูปสลิปและระบุยอดเงิน");
            return "redirect:/app/bills";
        }
        Payment payment = paymentService.submit(tenant, bill, amount, slip.getBytes(), slip.getContentType());
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            redirect.addFlashAttribute("success", "ชำระเงินสำเร็จ ระบบตัดยอดให้อัตโนมัติแล้ว");
        } else if (payment.getStatus() == PaymentStatus.SUSPICIOUS) {
            redirect.addFlashAttribute("error", "สลิปไม่ผ่านการตรวจสอบเบื้องต้น แอดมินจะตรวจสอบอีกครั้ง: " + payment.getAiNote());
        } else {
            redirect.addFlashAttribute("success", "ส่งสลิปแล้ว รอแอดมินตรวจสอบ");
        }
        return "redirect:/app/bills";
    }

    private TenantBillView view(Bill bill) {
        return new TenantBillView(bill, billingService.occupants(bill.getRoom()), billingService.shareOf(bill),
                billingService.outstanding(bill), paymentRepository.findByBillOrderBySubmittedAtDesc(bill));
    }
}
