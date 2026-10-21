package th.mfu;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Tenant;

@Controller
public class TenantController {

    private final TenantRepository repository;
    private final RoomRepository roomRepository;

    public TenantController(TenantRepository repository, RoomRepository roomRepository) {
        this.repository = repository;
        this.roomRepository = roomRepository;
    }

    @GetMapping("/tenants")
    public String listTenants(Model model) {
        model.addAttribute("tenants", repository.findAllByOrderByFirstNameAsc());
        return "tenants/list";
    }

    @GetMapping("/tenants/new")
    public String addTenantForm(Model model) {
        return showForm(model, new Tenant());
    }

    @GetMapping("/tenants/{id}")
    public String editTenantForm(Model model, @PathVariable Long id) {
        return showForm(model, findTenant(id));
    }

    @PostMapping("/tenants")
    public String saveTenant(@ModelAttribute Tenant tenant, RedirectAttributes redirect) {
        // remember the old room so it can become available if the tenant moved out of it
        Room oldRoom = tenant.getId() == null ? null : findTenant(tenant.getId()).getRoom();

        repository.save(tenant);
        updateRoomStatus(tenant.getRoom());
        if (oldRoom != null && (tenant.getRoom() == null || !oldRoom.getId().equals(tenant.getRoom().getId()))) {
            updateRoomStatus(oldRoom);
        }

        redirect.addFlashAttribute("success", "บันทึกข้อมูล " + tenant.getFullName() + " แล้ว");
        return "redirect:/tenants";
    }

    @GetMapping("/tenants/{id}/delete")
    public String deleteTenant(@PathVariable Long id, RedirectAttributes redirect) {
        Tenant tenant = findTenant(id);
        repository.delete(tenant);
        updateRoomStatus(tenant.getRoom());
        redirect.addFlashAttribute("success", "ลบผู้เช่าแล้ว");
        return "redirect:/tenants";
    }

    private String showForm(Model model, Tenant tenant) {
        model.addAttribute("tenant", tenant);
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        return "tenants/form";
    }

    private Tenant findTenant(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // a room is occupied while it has at least one tenant; rooms under maintenance are left alone
    private void updateRoomStatus(Room room) {
        if (room == null || room.getStatus() == RoomStatus.MAINTENANCE) {
            return;
        }
        room.setStatus(repository.countByRoom(room) > 0 ? RoomStatus.OCCUPIED : RoomStatus.AVAILABLE);
        roomRepository.save(room);
    }
}
