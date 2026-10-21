package th.mfu.app;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import th.mfu.BillRepository;
import th.mfu.TenantRepository;
import th.mfu.domain.AppUser;
import th.mfu.domain.Bill;
import th.mfu.domain.Tenant;
import th.mfu.service.BillingService;
import th.mfu.service.CurrentUserService;
import th.mfu.service.LaundryService;
import th.mfu.service.NotificationService;

@Controller
public class TenantHomeController {

    private final CurrentUserService currentUser;
    private final TenantRepository tenantRepository;
    private final BillRepository billRepository;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final LaundryService laundryService;

    public TenantHomeController(CurrentUserService currentUser, TenantRepository tenantRepository,
            BillRepository billRepository, BillingService billingService, NotificationService notificationService,
            LaundryService laundryService) {
        this.currentUser = currentUser;
        this.tenantRepository = tenantRepository;
        this.billRepository = billRepository;
        this.billingService = billingService;
        this.notificationService = notificationService;
        this.laundryService = laundryService;
    }

    @GetMapping("/app")
    public String home(Principal principal, Model model) {
        AppUser user = currentUser.user(principal);
        Optional<Tenant> tenant = currentUser.tenant(user);
        model.addAttribute("tenant", tenant.orElse(null));

        if (tenant.isPresent() && tenant.get().getRoom() != null) {
            var room = tenant.get().getRoom();
            model.addAttribute("roommates", tenantRepository.findByRoomOrderByFirstNameAsc(room));
            model.addAttribute("notices", notificationService.forRoom(room));

            double myOutstanding = 0;
            for (Bill bill : billRepository.findByRoomOrderByBillMonthAsc(room)) {
                if (!bill.isPaid()) {
                    myOutstanding += Math.min(billingService.shareOf(bill), billingService.outstanding(bill));
                }
            }
            model.addAttribute("myOutstanding", myOutstanding);
            List<Bill> recent = billRepository.findByRoomOrderByBillMonthDesc(room);
            model.addAttribute("latestBill", recent.isEmpty() ? null : recent.get(0));
        }
        model.addAttribute("laundryAvailable", laundryService.availableCount());
        model.addAttribute("laundryTotal", laundryService.machines().size());
        return "app/home";
    }
}
