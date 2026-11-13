package th.mfu.app;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import th.mfu.config.Messages;
import th.mfu.domain.Bill;
import th.mfu.domain.Payment;
import th.mfu.domain.PaymentStatus;
import th.mfu.domain.Tenant;
import th.mfu.service.BillingService;
import th.mfu.service.CurrentUserService;
import th.mfu.service.PaymentService;

@Controller
public class TenantBillController {

    private final CurrentUserService currentUser;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final BillingService billingService;
    private final PaymentService paymentService;
    private final DormProperties props;
    private final Messages messages;

    public TenantBillController(CurrentUserService currentUser, BillRepository billRepository,
            PaymentRepository paymentRepository, BillingService billingService, PaymentService paymentService,
            DormProperties props, Messages messages) {
        this.currentUser = currentUser;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.billingService = billingService;
        this.paymentService = paymentService;
        this.props = props;
        this.messages = messages;
    }

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
    public String pay(@PathVariable Long id, @RequestParam("slip") MultipartFile slip,
            Principal principal, RedirectAttributes redirect) throws IOException {
        Optional<Tenant> optTenant = currentUser.tenant(currentUser.user(principal));
        if (!optTenant.isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Tenant tenant = optTenant.get();
        Optional<Bill> optBill = billRepository.findById(id);
        // only a bill of the tenant's own room
        if (!optBill.isPresent() || tenant.getRoom() == null
                || !optBill.get().getRoom().getId().equals(tenant.getRoom().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Bill bill = optBill.get();

        // the amount is fixed by the system: the tenant's share, or what is left if that is less
        double amount = amountDue(bill);
        if (amount <= 0) {
            redirect.addFlashAttribute("error", messages.get("flash.slip.nothingDue"));
            return "redirect:/app/bills";
        }
        if (slip.isEmpty()) {
            redirect.addFlashAttribute("error", messages.get("flash.slip.missing"));
            return "redirect:/app/bills";
        }
        Payment payment = paymentService.submit(tenant, bill, amount, slip.getBytes(), slip.getContentType());
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            redirect.addFlashAttribute("success", messages.get("flash.slip.approved"));
        } else if (payment.getStatus() == PaymentStatus.SUSPICIOUS) {
            redirect.addFlashAttribute("error", messages.get("flash.slip.suspicious", payment.getAiNote()));
        } else {
            redirect.addFlashAttribute("success", messages.get("flash.slip.pending"));
        }
        return "redirect:/app/bills";
    }

    /** What one tenant transfers for a bill: their share of the room, but never more than is still unpaid. */
    private double amountDue(Bill bill) {
        return Math.min(billingService.shareOf(bill), billingService.outstanding(bill));
    }

    private TenantBillView view(Bill bill) {
        return new TenantBillView(bill, billingService.occupants(bill.getRoom()), billingService.shareOf(bill),
                billingService.outstanding(bill), paymentRepository.findByBillOrderBySubmittedAtDesc(bill));
    }
}
