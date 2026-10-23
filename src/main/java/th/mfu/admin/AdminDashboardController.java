package th.mfu.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private RepairRequestRepository repairRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private LaundryService laundryService;

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("roomCount", roomRepository.count());
        model.addAttribute("availableCount", roomRepository.countByStatus(RoomStatus.AVAILABLE));
        model.addAttribute("tenantCount", tenantRepository.count());
        model.addAttribute("openRepairCount", repairRepository.countByStatusNot(RepairStatus.DONE));
        model.addAttribute("slipsToReview", paymentRepository.countByStatusIn(List.of(PaymentStatus.PENDING, PaymentStatus.SUSPICIOUS)));

        List<Bill> unpaidBills = billRepository.findByPaidFalseOrderByBillMonthAsc();
        model.addAttribute("unpaidBills", unpaidBills);
        model.addAttribute("unpaidTotal", unpaidBills.stream().mapToDouble(Bill::getTotal).sum());

        model.addAttribute("abnormal", analyticsService.abnormalRooms());

        List<Feedback> allFeedback = feedbackRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("latestFeedback", feedbackRepository.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("unreadFeedback", feedbackRepository.countByReadByOwnerFalse());
        model.addAttribute("averageRating", allFeedback.stream().mapToInt(Feedback::getRating).average().orElse(0));

        model.addAttribute("machines", laundryService.machines());
        model.addAttribute("now", laundryService.now());
        return "admin/dashboard";
    }
}
