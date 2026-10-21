package th.mfu;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import th.mfu.domain.Bill;
import th.mfu.domain.RepairStatus;
import th.mfu.domain.RoomStatus;

@Controller
public class HomeController {

    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final BillRepository billRepository;
    private final RepairRequestRepository repairRepository;

    public HomeController(RoomRepository roomRepository, TenantRepository tenantRepository,
            BillRepository billRepository, RepairRequestRepository repairRepository) {
        this.roomRepository = roomRepository;
        this.tenantRepository = tenantRepository;
        this.billRepository = billRepository;
        this.repairRepository = repairRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("roomCount", roomRepository.count());
        model.addAttribute("availableCount", roomRepository.countByStatus(RoomStatus.AVAILABLE));
        model.addAttribute("tenantCount", tenantRepository.count());
        model.addAttribute("openRepairCount", repairRepository.countByStatusNot(RepairStatus.DONE));

        List<Bill> unpaidBills = billRepository.findByPaidFalseOrderByBillMonthAsc();
        model.addAttribute("unpaidBills", unpaidBills);
        model.addAttribute("unpaidTotal", unpaidBills.stream().mapToDouble(Bill::getTotal).sum());
        return "index";
    }
}
