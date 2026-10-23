package th.mfu.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import th.mfu.BillRepository;
import th.mfu.PaymentRepository;
import th.mfu.TenantRepository;
import th.mfu.config.DormProperties;
import th.mfu.domain.Bill;
import th.mfu.domain.PaymentStatus;
import th.mfu.domain.Room;

/** Bill splitting between roommates and automatic "paid" status from approved slips. */
@Service
public class BillingService {

    private final TenantRepository tenantRepository;
    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final DormProperties props;

    public BillingService(TenantRepository tenantRepository, PaymentRepository paymentRepository,
            BillRepository billRepository, DormProperties props) {
        this.tenantRepository = tenantRepository;
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
        this.props = props;
    }

    /** People sharing the bill: the tenants in the room, at least 1. */
    public long occupants(Room room) {
        return Math.max(1, tenantRepository.countByRoom(room));
    }

    public double shareOf(Bill bill) {
        return split(bill.getTotal(), occupants(bill.getRoom()));
    }

    public double approvedAmount(Bill bill) {
        return paymentRepository.findByBillOrderBySubmittedAtDesc(bill).stream()
                .filter(p -> p.getStatus() == PaymentStatus.APPROVED)
                .mapToDouble(p -> p.getAmount() == null ? 0 : p.getAmount())
                .sum();
    }

    public double outstanding(Bill bill) {
        return bill.isPaid() ? 0 : Math.max(0, round2(bill.getTotal() - approvedAmount(bill)));
    }

    /** Marks the bill paid once approved slips cover the total (auto reconciliation). */
    public void refreshPaidStatus(Bill bill) {
        if (!bill.isPaid() && approvedAmount(bill) >= bill.getTotal() - 0.5) {
            bill.setPaid(true);
            billRepository.save(bill);
        }
    }

    public LocalDate defaultDueDate(String billMonth) {
        return YearMonth.parse(billMonth).plusMonths(1).atDay(props.getDueDay());
    }

    static double split(double total, long people) {
        return round2(total / Math.max(1, people));
    }

    static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
