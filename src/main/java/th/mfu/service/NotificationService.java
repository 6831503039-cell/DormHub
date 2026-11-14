package th.mfu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import th.mfu.BillRepository;
import th.mfu.domain.Bill;
import th.mfu.domain.Room;

/**
 * Builds the tenant's notifications on the fly:
 * payment due within 7 days / overdue, electricity summary within 7 days, abnormal electricity usage.
 */
@Service
public class NotificationService {

    static final int PAYMENT_REMINDER_DAYS = 7;
    static final int ELECTRICITY_REMINDER_DAYS = 7;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BillRepository billRepository;
    private final BillingService billingService;
    private final AnalyticsService analyticsService;
    private final Clock clock;

    public NotificationService(BillRepository billRepository, BillingService billingService,
            AnalyticsService analyticsService, Clock clock) {
        this.billRepository = billRepository;
        this.billingService = billingService;
        this.analyticsService = analyticsService;
        this.clock = clock;
    }

    public List<Notice> forRoom(Room room) {
        LocalDate today = LocalDate.now(clock);
        List<Bill> bills = billRepository.findByRoomOrderByBillMonthAsc(room);
        long occupants = billingService.occupants(room);

        List<Notice> notices = new ArrayList<>();
        for (Bill bill : bills) {
            if (!bill.isPaid()) {
                LocalDate due = bill.getDueDate() != null ? bill.getDueDate() : billingService.defaultDueDate(bill.getBillMonth());
                Optional<Notice> payment = paymentNotice(bill.getBillMonth(), due, BillingService.split(bill.getTotal(), occupants), today);
                if (payment.isPresent()) {
                    notices.add(payment.get());
                }
            }
        }
        Optional<Notice> electricity = electricityReminder(bills, today);
        if (electricity.isPresent()) {
            notices.add(electricity.get());
        }
        Optional<AbnormalUsage> abnormal = analyticsService.abnormalUsage(room);
        if (abnormal.isPresent()) {
            AbnormalUsage a = abnormal.get();
            notices.add(new Notice("warning", "zap",
                    "notice.abnormal.title", "notice.abnormal.message",
                    a.getBillMonth(), String.valueOf(a.getUnits()), String.valueOf(a.getAverage()), String.valueOf(a.getPercentAbove())));
        }
        return notices;
    }

    static Optional<Notice> paymentNotice(String billMonth, LocalDate due, double share, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, due);
        String amount = String.format("%,.2f", share);
        if (days < 0) {
            return Optional.of(new Notice("danger", "alert-circle", "notice.overdue.title", "notice.overdue.message",
                    billMonth, due.format(DATE), -days, amount));
        }
        if (days <= PAYMENT_REMINDER_DAYS) {
            return Optional.of(days == 0
                    ? new Notice("warning", "calendar-clock", "notice.due.title", "notice.dueToday.message", billMonth, amount)
                    : new Notice("warning", "calendar-clock", "notice.due.title", "notice.dueIn.message",
                            billMonth, days, due.format(DATE), amount));
        }
        return Optional.empty();
    }

    /** In the last 7 days of the month, warn about the coming electricity bill with an estimate. */
    static Optional<Notice> electricityReminder(List<Bill> billsOldestFirst, LocalDate today) {
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        long days = ChronoUnit.DAYS.between(today, endOfMonth);
        if (days > ELECTRICITY_REMINDER_DAYS || billsOldestFirst.isEmpty()) {
            return Optional.empty();
        }
        int n = billsOldestFirst.size();
        // average electricity cost of the last 3 bills
        List<Bill> lastThree = billsOldestFirst.subList(Math.max(0, n - 3), n);
        double sum = 0;
        for (Bill bill : lastThree) {
            sum += bill.getElectricCost();
        }
        double estimate = sum / lastThree.size();
        String amount = String.format("%,.2f", estimate);
        return Optional.of(days == 0
                ? new Notice("info", "plug-zap", "notice.electric.title", "notice.electricToday.message", amount)
                : new Notice("info", "plug-zap", "notice.electric.title", "notice.electricIn.message", days, amount));
    }
}
