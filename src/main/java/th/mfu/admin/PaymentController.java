package th.mfu.admin;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.PaymentRepository;
import th.mfu.config.Messages;
import th.mfu.domain.Payment;
import th.mfu.service.PaymentService;

/** Slip review: AI-flagged and pending slips are approved or rejected here. */
@Controller
@RequestMapping("/admin/payments")
public class PaymentController {

    private final PaymentRepository repository;
    private final PaymentService paymentService;
    private final Messages messages;

    public PaymentController(PaymentRepository repository, PaymentService paymentService, Messages messages) {
        this.repository = repository;
        this.paymentService = paymentService;
        this.messages = messages;
    }

    @GetMapping
    public String listPayments(Model model) {
        model.addAttribute("payments", repository.findAllByOrderBySubmittedAtDesc());
        return "admin/payments/list";
    }

    @GetMapping("/{id}/slip")
    public ResponseEntity<byte[]> slip(@PathVariable Long id) {
        Payment payment = find(id);
        if (payment.getSlip() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        MediaType type = payment.getSlipContentType() != null && payment.getSlipContentType().startsWith("image/")
                ? MediaType.parseMediaType(payment.getSlipContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(type).body(payment.getSlip());
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        paymentService.approve(find(id));
        redirect.addFlashAttribute("success", messages.get("flash.payment.approved"));
        return "redirect:/admin/payments";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirect) {
        paymentService.reject(find(id));
        redirect.addFlashAttribute("success", messages.get("flash.payment.rejected"));
        return "redirect:/admin/payments";
    }

    private Payment find(Long id) {
        Optional<Payment> optPayment = repository.findById(id);
        if (!optPayment.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return optPayment.get();
    }
}
