package th.mfu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import th.mfu.BillRepository;
import th.mfu.domain.Bill;
import th.mfu.domain.Room;

/**
 * Builds the tenant's notifications on the fly:
 * payment due within 5 days / overdue, electricity summary within 7 days, abnormal electricity usage.
 */
@Service
public class NotificationService {

    static final int PAYMENT_REMINDER_DAYS = 5;
    static final int ELECTRICITY_REMINDER_DAYS = 7;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private BillRepository billRepository;
    @Autowired
    private BillingService billingService;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private Clock clock;

    public List<Notice> forRoom(Room room) {
        LocalDate today = LocalDate.now(clock);
        List<Bill> bills = billRepository.findByRoomOrderByBillMonthAsc(room);
        long occupants = billingService.occupants(room);

        List<Notice> notices = new ArrayList<>();
        for (Bill bill : bills) {
            if (!bill.isPaid()) {
                LocalDate due = bill.getDueDate() != null ? bill.getDueDate() : billingService.defaultDueDate(bill.getBillMonth());
                paymentNotice(bill.getBillMonth(), due, BillingService.split(bill.getTotal(), occupants), today)
                        .ifPresent(notices::add);
            }
        }
        electricityReminder(bills, today).ifPresent(notices::add);
        analyticsService.abnormalUsage(room).ifPresent(a -> notices.add(new Notice("warning", "zap",
                "ค่าไฟสูงผิดปกติ",
                "เดือน " + a.getBillMonth() + " ใช้ไฟ " + a.getUnits() + " หน่วย สูงกว่าค่าเฉลี่ย ("
                        + a.getAverage() + " หน่วย) " + a.getPercentAbove() + "% ลองตรวจเครื่องใช้ไฟฟ้าในห้อง")));
        return notices;
    }

    static Optional<Notice> paymentNotice(String billMonth, LocalDate due, double share, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, due);
        String amount = String.format("%,.2f", share);
        if (days < 0) {
            return Optional.of(new Notice("danger", "alert-circle", "เลยกำหนดชำระ",
                    "บิลเดือน " + billMonth + " เลยกำหนด " + due.format(DATE) + " มาแล้ว " + (-days) + " วัน (ส่วนของคุณ ฿" + amount + ")"));
        }
        if (days <= PAYMENT_REMINDER_DAYS) {
            String when = days == 0 ? "ครบกำหนดวันนี้" : "อีก " + days + " วันจะครบกำหนด (" + due.format(DATE) + ")";
            return Optional.of(new Notice("warning", "calendar-clock", "ใกล้ถึงกำหนดชำระ",
                    "บิลเดือน " + billMonth + " " + when + " ส่วนของคุณ ฿" + amount));
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
        double estimate = billsOldestFirst.subList(Math.max(0, n - 3), n).stream()
                .mapToDouble(Bill::getElectricCost).average().orElse(0);
        String when = days == 0 ? "วันนี้" : "อีก " + days + " วัน";
        return Optional.of(new Notice("info", "plug-zap", "แจ้งเตือนค่าไฟล่วงหน้า",
                when + "จะจดมิเตอร์สรุปค่าไฟเดือนนี้ ประมาณการจากค่าเฉลี่ย 3 เดือนล่าสุด ฿" + String.format("%,.2f", estimate)));
    }
}
