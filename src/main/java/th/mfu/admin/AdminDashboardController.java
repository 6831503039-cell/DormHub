package th.mfu.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import th.mfu.BillRepository;
import th.mfu.FeedbackRepository;
import th.mfu.PaymentRepository;
import th.mfu.RepairRequestRepository;
import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.domain.Bill;
import th.mfu.domain.Feedback;
import th.mfu.domain.PaymentStatus;
import th.mfu.domain.RepairStatus;
import th.mfu.domain.RoomStatus;
import th.mfu.service.AnalyticsService;
import th.mfu.service.LaundryService;

@Controller
public class AdminDashboardController {

    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final BillRepository billRepository;
    private final RepairRequestRepository repairRepository;
    private final PaymentRepository paymentRepository;
    private final FeedbackRepository feedbackRepository;
    private final AnalyticsService analyticsService;
    private final LaundryService laundryService;

    public AdminDashboardController(RoomRepository roomRepository, TenantRepository tenantRepository,
            BillRepository billRepository, RepairRequestRepository repairRepository, PaymentRepository paymentRepository,
            FeedbackRepository feedbackRepository, AnalyticsService analyticsService, LaundryService laundryService) {
        this.roomRepository = roomRepository;
        this.tenantRepository = tenantRepository;
        this.billRepository = billRepository;
        this.repairRepository = repairRepository;
        this.paymentRepository = paymentRepository;
        this.feedbackRepository = feedbackRepository;
        this.analyticsService = analyticsService;
        this.laundryService = laundryService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("roomCount", roomRepository.count());
        model.addAttribute("availableCount", roomRepository.countByStatus(RoomStatus.AVAILABLE));
        model.addAttribute("tenantCount", tenantRepository.count());
        model.addAttribute("openRepairCount", repairRepository.countByStatusNot(RepairStatus.DONE));
        model.addAttribute("slipsToReview", paymentRepository.countByStatusIn(List.of(PaymentStatus.PENDING, PaymentStatus.SUSPICIOUS)));

        List<Bill> unpaidBills = billRepository.findByPaidFalseOrderByBillMonthAsc();
        model.addAttribute("unpaidBills", unpaidBills);
        double unpaidTotal = 0;
        for (Bill bill : unpaidBills) {
            unpaidTotal += bill.getTotal();
        }
        model.addAttribute("unpaidTotal", unpaidTotal);

        model.addAttribute("abnormal", analyticsService.abnormalRooms());

        List<Feedback> allFeedback = feedbackRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("latestFeedback", feedbackRepository.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("unreadFeedback", feedbackRepository.countByReadByOwnerFalse());
        double averageRating = 0;
        if (!allFeedback.isEmpty()) {
            int sum = 0;
            for (Feedback feedback : allFeedback) {
                sum += feedback.getRating();
            }
            averageRating = (double) sum / allFeedback.size();
        }
        model.addAttribute("averageRating", averageRating);

        model.addAttribute("machines", laundryService.machines());
        model.addAttribute("now", laundryService.now());
        return "admin/dashboard";
    }
}
