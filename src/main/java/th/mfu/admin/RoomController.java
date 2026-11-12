package th.mfu.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.config.Messages;
import th.mfu.domain.Bill;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Tenant;
import th.mfu.service.RoomMapService;
import th.mfu.service.RoomView;

@Controller
@RequestMapping("/admin")
public class RoomController {

    private final RoomRepository repository;
    private final TenantRepository tenantRepository;
    private final RoomMapService roomMapService;
    private final Messages messages;

    public RoomController(RoomRepository repository, TenantRepository tenantRepository, RoomMapService roomMapService, Messages messages) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.roomMapService = roomMapService;
        this.messages = messages;
    }

    @GetMapping("/rooms")
    public String listRooms(Model model) {
        List<Room> rooms = repository.findAllByOrderByRoomNumberAsc();
        model.addAttribute("rooms", rooms);
        // building -> floor -> rooms: a collapsible card per building with a collapsible part per floor
        Map<String, Map<Integer, List<Room>>> groups = new TreeMap<>();
        for (Room room : rooms) {
            String building = room.getBuilding() == null || room.getBuilding().isBlank() ? "-" : room.getBuilding();
            int floor = room.getFloor() == null ? 0 : room.getFloor();
            groups.computeIfAbsent(building, b -> new TreeMap<>())
                    .computeIfAbsent(floor, f -> new ArrayList<>()).add(room);
        }
        model.addAttribute("roomGroups", groups);
        model.addAttribute("occupants", occupantsByRoom());
        return "admin/rooms/list";
    }

    /** Room layout: 3D map on top (static/js/dorm3d.js), and the rooms grouped by building and floor below. */
    @GetMapping("/layout")
    public String layout(Model model) {
        // building -> floor -> rooms
        Map<String, Map<Integer, List<Room>>> buildings = new TreeMap<>();
        for (Room room : repository.findAllByOrderByRoomNumberAsc()) {
            String building = room.getBuilding() == null || room.getBuilding().isBlank() ? "A" : room.getBuilding();
            int floor = room.getFloor() == null ? 0 : room.getFloor();
            buildings.computeIfAbsent(building, b -> new TreeMap<>())
                    .computeIfAbsent(floor, f -> new java.util.ArrayList<>()).add(room);
        }
        // free / total rooms per building, shown on each collapsed building row
        Map<String, Integer> buildingFree = new TreeMap<>();
        Map<String, Integer> buildingTotal = new TreeMap<>();
        buildings.forEach((building, floors) -> floors.values().forEach(rooms -> rooms.forEach(room -> {
            buildingTotal.merge(building, 1, Integer::sum);
            buildingFree.merge(building, room.getStatus() == RoomStatus.AVAILABLE ? 1 : 0, Integer::sum);
        })));
        model.addAttribute("buildings", buildings);
        model.addAttribute("buildingFree", buildingFree);
        model.addAttribute("buildingTotal", buildingTotal);
        model.addAttribute("occupants", occupantsByRoom());
        model.addAttribute("soonDays", RoomMapService.SOON_DAYS);
        Bill rates = new Bill();
        model.addAttribute("electricRate", rates.getElectricRate());
        model.addAttribute("waterRate", rates.getWaterRate());
        return "admin/rooms/layout";
    }

    /** Room data for the admin 3D map, including tenant names. */
    @GetMapping("/layout/rooms")
    @ResponseBody
    public List<RoomView> layoutRooms() {
        return roomMapService.rooms(true);
    }

    @GetMapping("/rooms/new")
    public String addRoomForm(Model model) {
        model.addAttribute("room", new Room());
        return "admin/rooms/form";
    }

    @GetMapping("/rooms/{id}")
    public String editRoomForm(Model model, @PathVariable Long id) {
        Room room = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("room", room);
        // tenants come straight from the @OneToMany relationship (Room.tenants)
        model.addAttribute("tenants", room.getTenants());
        return "admin/rooms/form";
    }

    @PostMapping("/rooms")
    public String saveRoom(@ModelAttribute Room room, RedirectAttributes redirect) {
        try {
            repository.save(room);
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", messages.get("flash.room.duplicate", room.getRoomNumber()));
            return room.getId() == null ? "redirect:/admin/rooms/new" : "redirect:/admin/rooms/" + room.getId();
        }
        redirect.addFlashAttribute("success", messages.get("flash.room.saved", room.getRoomNumber()));
        return "redirect:/admin/rooms";
    }

    @GetMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            repository.deleteById(id);
            redirect.addFlashAttribute("success", messages.get("flash.room.deleted"));
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", messages.get("flash.room.inUse"));
        }
        return "redirect:/admin/rooms";
    }

    private Map<Long, Long> occupantsByRoom() {
        Map<Long, Long> counts = new HashMap<>();
        for (Tenant t : tenantRepository.findAll()) {
            if (t.getRoom() != null) {
                counts.merge(t.getRoom().getId(), 1L, Long::sum);
            }
        }
        return counts;
    }
}
