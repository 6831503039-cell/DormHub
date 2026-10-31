package th.mfu.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.AppUserRepository;
import th.mfu.PaymentRepository;
import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.config.Messages;
import th.mfu.domain.Payment;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Tenant;

@Controller
@RequestMapping("/admin")
public class TenantController {

    private final TenantRepository repository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final AppUserRepository userRepository;
    private final Messages messages;

    public TenantController(TenantRepository repository, RoomRepository roomRepository,
            PaymentRepository paymentRepository, AppUserRepository userRepository, Messages messages) {
        this.repository = repository;
        this.roomRepository = roomRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.messages = messages;
    }

    @GetMapping("/tenants")
    public String listTenants(Model model) {
        List<Tenant> tenants = repository.findAllByOrderByFirstNameAsc();
        model.addAttribute("tenants", tenants);
        // building -> floor -> tenants ordered by room: a collapsible card per building with a part per floor;
        // "none" = no room yet (last, floor 0)
        Map<String, Map<Integer, List<Tenant>>> groups = new TreeMap<>(Comparator.comparing((String b) -> b.equals("none")).thenComparing(b -> b));
        tenants.stream()
                .sorted(Comparator.comparing((Tenant t) -> t.getRoom() == null ? "~" : t.getRoom().getRoomNumber())
                        .thenComparing(Tenant::getFullName))
                .forEach(t -> {
                    Room room = t.getRoom();
                    String building = room == null ? "none"
                            : (room.getBuilding() == null || room.getBuilding().isBlank() ? "-" : room.getBuilding());
                    int floor = room == null || room.getFloor() == null ? 0 : room.getFloor();
                    groups.computeIfAbsent(building, b -> new TreeMap<>())
                            .computeIfAbsent(floor, f -> new ArrayList<>()).add(t);
                });
        model.addAttribute("tenantGroups", groups);
        // emails that already have a login, so the list can show which tenants use the app
        Set<String> accountEmails = new HashSet<>();
        userRepository.findAll().forEach(u -> accountEmails.add(u.getEmail().toLowerCase()));
        model.addAttribute("accountEmails", accountEmails);
        return "admin/tenants/list";
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
        Room newRoom = tenant.getRoom();

        if (newRoom != null && isFull(newRoom, oldRoom)) {
            redirect.addFlashAttribute("error", messages.get("flash.tenant.roomFull",
                    newRoom.getRoomNumber(), newRoom.getCapacity()));
            return tenant.getId() == null ? "redirect:/admin/tenants/new" : "redirect:/admin/tenants/" + tenant.getId();
        }

        repository.save(tenant);
        updateRoomStatus(newRoom);
        if (oldRoom != null && (newRoom == null || !oldRoom.getId().equals(newRoom.getId()))) {
            updateRoomStatus(oldRoom);
        }

        redirect.addFlashAttribute("success", messages.get("flash.tenant.saved", tenant.getFullName()));
        return "redirect:/admin/tenants";
    }

    @GetMapping("/tenants/{id}/delete")
    public String deleteTenant(@PathVariable Long id, RedirectAttributes redirect) {
        Tenant tenant = findTenant(id);
        // keep payment history, just detach it from the tenant
        for (Payment p : paymentRepository.findByTenant(tenant)) {
            p.setTenant(null);
            paymentRepository.save(p);
        }
        repository.delete(tenant);
        updateRoomStatus(tenant.getRoom());
        redirect.addFlashAttribute("success", messages.get("flash.tenant.deleted"));
        return "redirect:/admin/tenants";
    }

    private boolean isFull(Room room, Room oldRoom) {
        long others = repository.countByRoom(room);
        if (oldRoom != null && oldRoom.getId().equals(room.getId())) {
            others--; // the tenant is already counted in this room
        }
        int capacity = room.getCapacity() == null ? 1 : room.getCapacity();
        return others >= capacity;
    }

    private String showForm(Model model, Tenant tenant) {
        model.addAttribute("tenant", tenant);
        model.addAttribute("rooms", roomRepository.findAllByOrderByRoomNumberAsc());
        return "admin/tenants/form";
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
