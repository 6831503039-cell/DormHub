package th.mfu.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import th.mfu.PaymentRepository;
import th.mfu.domain.Bill;
import th.mfu.domain.Payment;
import th.mfu.domain.PaymentStatus;
import th.mfu.domain.Tenant;

@Service
public class PaymentService {

    private final PaymentRepository repository;
    private final SlipVerificationService slipVerifier;
    private final BillingService billingService;
    private final Clock clock;

    public PaymentService(PaymentRepository repository, SlipVerificationService slipVerifier,
            BillingService billingService, Clock clock) {
        this.repository = repository;
        this.slipVerifier = slipVerifier;
        this.billingService = billingService;
        this.clock = clock;
    }

    /** Saves the slip, runs the AI check and closes the bill if it is now fully paid. */
    public Payment submit(Tenant tenant, Bill bill, double amount, byte[] slip, String contentType) {
        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setTenant(tenant);
        payment.setAmount(amount);
        payment.setSlip(slip);
        payment.setSlipContentType(contentType);
        payment.setSubmittedAt(LocalDateTime.now(clock));

        SlipCheck check = slipVerifier.verify(slip, contentType, amount);
        payment.setStatus(check.getStatus());
        payment.setAiNote(check.getNote());
        repository.save(payment);

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            billingService.refreshPaidStatus(bill);
        }
        return payment;
    }

    public void approve(Payment payment) {
        payment.setStatus(PaymentStatus.APPROVED);
        repository.save(payment);
        billingService.refreshPaidStatus(payment.getBill());
    }

    public void reject(Payment payment) {
        payment.setStatus(PaymentStatus.REJECTED);
        repository.save(payment);
    }
}
