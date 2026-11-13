package th.mfu.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Tenant;

// REST API for rooms, written like CustomerController / SaleOrderController in sample-boot-relation.
// Only the admin can call it (see SecurityConfig).
@RestController
public class RoomApiController {

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private TenantRepository tenantRepo;

    // GET all rooms, or search with /api/rooms?q=A1
    @GetMapping("/api/rooms")
    public ResponseEntity<Collection<Room>> getAllRooms(@RequestParam(required = false) String q) {
        List<Room> rooms;
        if (q == null || q.isBlank()) {
            rooms = roomRepo.findAllByOrderByRoomNumberAsc();
        } else {
            rooms = roomRepo.findByRoomNumberContainingIgnoreCaseOrRoomTypeContainingIgnoreCaseOrderByRoomNumberAsc(q, q);
        }
        return new ResponseEntity<Collection<Room>>(rooms, HttpStatus.OK);
    }

    // GET one room
    @GetMapping("/api/rooms/{id}")
    public ResponseEntity<Room> getRoom(@PathVariable Long id) {
        if (!roomRepo.existsById(id))
            return new ResponseEntity<Room>(HttpStatus.NOT_FOUND);
        Optional<Room> room = roomRepo.findById(id);
        return new ResponseEntity<Room>(room.get(), HttpStatus.OK);
    }

    // POST for creating a room
    @PostMapping("/api/rooms")
    public ResponseEntity<String> createRoom(@RequestBody Room room) {
        room.setId(null);
        if (roomRepo.findByRoomNumber(room.getRoomNumber()).isPresent())
            return new ResponseEntity<String>("Room number already exists", HttpStatus.CONFLICT);
        roomRepo.save(room);
        return new ResponseEntity<String>("Room created", HttpStatus.CREATED);
    }

    // PUT for updating a room
    @PutMapping("/api/rooms/{id}")
    public ResponseEntity<String> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        if (!roomRepo.existsById(id))
            return new ResponseEntity<String>("Room not found", HttpStatus.NOT_FOUND);
        room.setId(id);
        try {
            roomRepo.save(room);
        } catch (DataIntegrityViolationException e) {
            return new ResponseEntity<String>("Room number already exists", HttpStatus.CONFLICT);
        }
        return new ResponseEntity<String>("Room updated", HttpStatus.OK);
    }

    // DELETE a room (not allowed while tenants or bills still use it)
    @DeleteMapping("/api/rooms/{id}")
    public ResponseEntity<String> deleteRoom(@PathVariable Long id) {
        if (!roomRepo.existsById(id))
            return new ResponseEntity<String>("Room not found", HttpStatus.NOT_FOUND);
        try {
            roomRepo.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            return new ResponseEntity<String>("Room is still in use", HttpStatus.CONFLICT);
        }
        return new ResponseEntity<String>("Room deleted", HttpStatus.NO_CONTENT);
    }

    // GET the tenants UNDER a room (nested URL, like /customers/{customerId}/orders)
    @GetMapping("/api/rooms/{roomId}/tenants")
    public ResponseEntity<Collection<Tenant>> getTenantsByRoom(@PathVariable Long roomId) {
        Optional<Room> room = roomRepo.findById(roomId);
        if (!room.isPresent())
            return new ResponseEntity<Collection<Tenant>>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<Collection<Tenant>>(tenantRepo.findByRoomOrderByFirstNameAsc(room.get()), HttpStatus.OK);
    }

    // POST for creating a tenant UNDER a room
    @PostMapping("/api/rooms/{roomId}/tenants")
    public ResponseEntity<String> createTenant(@PathVariable Long roomId, @RequestBody Tenant tenant) {
        Optional<Room> optRoom = roomRepo.findById(roomId);
        if (!optRoom.isPresent())
            return new ResponseEntity<String>("Room not found", HttpStatus.NOT_FOUND);
        Room room = optRoom.get();
        int capacity = room.getCapacity() == null ? 1 : room.getCapacity();
        if (tenantRepo.countByRoom(room) >= capacity)
            return new ResponseEntity<String>("Room is full", HttpStatus.CONFLICT);

        tenant.setId(null);
        tenant.setRoom(room); // this is what links the tenant row to the room row
        tenantRepo.save(tenant);

        if (room.getStatus() != RoomStatus.MAINTENANCE) {
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepo.save(room);
        }
        return new ResponseEntity<String>("Tenant created", HttpStatus.CREATED);
    }
}
