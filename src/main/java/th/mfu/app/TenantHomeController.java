package th.mfu.app;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import th.mfu.AnnouncementRepository;
import th.mfu.BillRepository;
import th.mfu.BookingRequestRepository;
import th.mfu.ParcelRepository;
import th.mfu.TenantRepository;
import th.mfu.domain.AppUser;
import th.mfu.domain.Bill;
import th.mfu.domain.Room;
import th.mfu.domain.Tenant;
import th.mfu.service.BillingService;
import th.mfu.service.CurrentUserService;
import th.mfu.service.LaundryService;
import th.mfu.service.Notice;
import th.mfu.service.NotificationService;

@Controller
public class TenantHomeController {

    private final CurrentUserService currentUser;
    private final TenantRepository tenantRepository;
    private final BillRepository billRepository;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final LaundryService laundryService;
    private final AnnouncementRepository announcementRepository;
    private final ParcelRepository parcelRepository;
    private final BookingRequestRepository bookingRepository;

    public TenantHomeController(CurrentUserService currentUser, TenantRepository tenantRepository,
            BillRepository billRepository, BillingService billingService, NotificationService notificationService,
            LaundryService laundryService, AnnouncementRepository announcementRepository,
            ParcelRepository parcelRepository, BookingRequestRepository bookingRepository) {
        this.currentUser = currentUser;
        this.tenantRepository = tenantRepository;
        this.billRepository = billRepository;
        this.billingService = billingService;
        this.notificationService = notificationService;
        this.laundryService = laundryService;
        this.announcementRepository = announcementRepository;
        this.parcelRepository = parcelRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/app")
    public String home(Principal principal, Model model) {
        AppUser user = currentUser.user(principal);
        Optional<Tenant> tenant = currentUser.tenant(user);
        model.addAttribute("tenant", tenant.orElse(null));

        if (tenant.isPresent() && tenant.get().getRoom() != null) {
            Room room = tenant.get().getRoom();
            model.addAttribute("roommates", tenantRepository.findByRoomOrderByFirstNameAsc(room));

            List<Notice> notices = new ArrayList<>();
            long parcels = parcelRepository.countByRoomAndPickedUpAtIsNull(room);
            if (parcels > 0) {
                notices.add(new Notice("info", "package", "notice.parcel.title", "notice.parcel.message", parcels));
            }
            notices.addAll(notificationService.forRoom(room));
            model.addAttribute("notices", notices);
            model.addAttribute("parcelsWaiting", parcels);

            double myOutstanding = 0;
            for (Bill bill : billRepository.findByRoomOrderByBillMonthAsc(room)) {
                if (!bill.isPaid()) {
                    myOutstanding += Math.min(billingService.shareOf(bill), billingService.outstanding(bill));
                }
            }
            model.addAttribute("myOutstanding", myOutstanding);
            List<Bill> recent = billRepository.findByRoomOrderByBillMonthDesc(room);
            model.addAttribute("latestBill", recent.isEmpty() ? null : recent.get(0));
        } else {
            // no room yet: show the latest rent request sent from the app, if any
            List<?> bookings = bookingRepository.findByApplicantOrderByCreatedAtDesc(user);
            model.addAttribute("latestBooking", bookings.isEmpty() ? null : bookings.get(0));
        }
        model.addAttribute("announcements", announcementRepository.findTop3ByOrderByPinnedDescCreatedAtDesc());
        model.addAttribute("laundryAvailable", laundryService.availableCount());
        model.addAttribute("laundryTotal", laundryService.machines().size());
        return "app/home";
    }

    /** Electricity units of the last 12 bills of the tenant's room (the "latest electricity" card links here). */
    @GetMapping("/app/usage")
    public String usage(Principal principal, Model model) {
        Optional<Tenant> tenant = currentUser.tenant(currentUser.user(principal));
        model.addAttribute("tenant", tenant.orElse(null));
        if (tenant.isPresent() && tenant.get().getRoom() != null) {
            List<Bill> bills = billRepository.findByRoomOrderByBillMonthDesc(tenant.get().getRoom());
            List<Bill> last12 = new ArrayList<>(bills.subList(0, Math.min(12, bills.size())));
            model.addAttribute("bills", last12);
            int maxUnits = 0;
            long totalUnits = 0;
            for (int i = 0; i < last12.size(); i++) {
                int units = last12.get(i).getElectricUnits();
                if (i == 0 || units > maxUnits) {
                    maxUnits = units;
                }
                totalUnits += units;
            }
            long avgUnits = last12.isEmpty() ? 0 : Math.round((double) totalUnits / last12.size());
            model.addAttribute("maxUnits", maxUnits);
            model.addAttribute("avgUnits", avgUnits);
        } else {
            model.addAttribute("bills", Collections.emptyList());
        }
        return "app/usage";
    }
}
