package th.mfu.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String listRooms(@RequestParam(required = false) String q, Model model) {
        // /admin/rooms?q=A1 searches on the database; without q every room is listed
        String keyword = q == null ? "" : q.trim();
        List<Room> rooms = keyword.isEmpty() ? repository.findAllByOrderByRoomNumberAsc()
                : repository.findByRoomNumberContainingIgnoreCaseOrRoomTypeContainingIgnoreCaseOrderByRoomNumberAsc(keyword, keyword);
        model.addAttribute("rooms", rooms);
        model.addAttribute("q", keyword);
        // building -> floor -> rooms: a collapsible card per building with a collapsible part per floor
        Map<String, Map<Integer, List<Room>>> groups = new TreeMap<>();
        for (Room room : rooms) {
            String building = room.getBuilding() == null || room.getBuilding().isBlank() ? "-" : room.getBuilding();
            int floor = room.getFloor() == null ? 0 : room.getFloor();
            if (!groups.containsKey(building)) {
                groups.put(building, new TreeMap<>());
            }
            Map<Integer, List<Room>> floors = groups.get(building);
            if (!floors.containsKey(floor)) {
                floors.put(floor, new ArrayList<>());
            }
            floors.get(floor).add(room);
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
            if (!buildings.containsKey(building)) {
                buildings.put(building, new TreeMap<>());
            }
            Map<Integer, List<Room>> floors = buildings.get(building);
            if (!floors.containsKey(floor)) {
                floors.put(floor, new ArrayList<>());
            }
            floors.get(floor).add(room);
        }
        // free / total rooms per building, shown on each collapsed building row
        Map<String, Integer> buildingFree = new TreeMap<>();
        Map<String, Integer> buildingTotal = new TreeMap<>();
        for (String building : buildings.keySet()) {
            int total = 0;
            int free = 0;
            for (List<Room> floorRooms : buildings.get(building).values()) {
                for (Room room : floorRooms) {
                    total++;
                    if (room.getStatus() == RoomStatus.AVAILABLE) {
                        free++;
                    }
                }
            }
            buildingTotal.put(building, total);
            buildingFree.put(building, free);
        }
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
    public ResponseEntity<List<RoomView>> layoutRooms() {
        return new ResponseEntity<List<RoomView>>(roomMapService.rooms(true), HttpStatus.OK);
    }

    @GetMapping("/rooms/new")
    public String addRoomForm(Model model) {
        model.addAttribute("room", new Room());
        return "admin/rooms/form";
    }

    @GetMapping("/rooms/{id}")
    public String editRoomForm(Model model, @PathVariable Long id) {
        Optional<Room> optRoom = repository.findById(id);
        if (!optRoom.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Room room = optRoom.get();
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
                Long roomId = t.getRoom().getId();
                counts.put(roomId, counts.getOrDefault(roomId, 0L) + 1);
            }
        }
        return counts;
    }
}
