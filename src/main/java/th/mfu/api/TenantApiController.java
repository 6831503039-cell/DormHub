package th.mfu.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import th.mfu.PaymentRepository;
import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.domain.Payment;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Tenant;

// REST API for tenants. A new tenant is created under a room: POST /api/rooms/{roomId}/tenants
@RestController
public class TenantApiController {

    @Autowired
    private TenantRepository tenantRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private PaymentRepository paymentRepo;

    // GET all tenants, or search with /api/tenants?q=somchai
    @GetMapping("/api/tenants")
    public ResponseEntity<Collection<Tenant>> getAllTenants(@RequestParam(required = false) String q) {
        List<Tenant> tenants;
        if (q == null || q.isBlank()) {
            tenants = tenantRepo.findAllByOrderByFirstNameAsc();
        } else {
            tenants = tenantRepo.search(q);
        }
        return new ResponseEntity<Collection<Tenant>>(tenants, HttpStatus.OK);
    }

    // GET one tenant
    @GetMapping("/api/tenants/{id}")
    public ResponseEntity<Tenant> getTenant(@PathVariable Long id) {
        if (!tenantRepo.existsById(id))
            return new ResponseEntity<Tenant>(HttpStatus.NOT_FOUND);
        Optional<Tenant> tenant = tenantRepo.findById(id);
        return new ResponseEntity<Tenant>(tenant.get(), HttpStatus.OK);
    }

    // PUT for updating a tenant's details (the room stays the same)
    @PutMapping("/api/tenants/{id}")
    public ResponseEntity<String> updateTenant(@PathVariable Long id, @RequestBody Tenant newData) {
        Optional<Tenant> optTenant = tenantRepo.findById(id);
        if (!optTenant.isPresent())
            return new ResponseEntity<String>("Tenant not found", HttpStatus.NOT_FOUND);
        Tenant tenant = optTenant.get();
        tenant.setFirstName(newData.getFirstName());
        tenant.setLastName(newData.getLastName());
        tenant.setPhone(newData.getPhone());
        tenant.setEmail(newData.getEmail());
        tenant.setCheckInDate(newData.getCheckInDate());
        tenant.setContractEndDate(newData.getContractEndDate());
        tenantRepo.save(tenant);
        return new ResponseEntity<String>("Tenant updated", HttpStatus.OK);
    }

    // DELETE a tenant; payments stay as history, only unlinked from the tenant
    @DeleteMapping("/api/tenants/{id}")
    public ResponseEntity<String> deleteTenant(@PathVariable Long id) {
        Optional<Tenant> optTenant = tenantRepo.findById(id);
        if (!optTenant.isPresent())
            return new ResponseEntity<String>("Tenant not found", HttpStatus.NOT_FOUND);
        Tenant tenant = optTenant.get();
        for (Payment p : paymentRepo.findByTenant(tenant)) {
            p.setTenant(null);
            paymentRepo.save(p);
        }
        tenantRepo.delete(tenant);

        // the room becomes available again when its last tenant leaves
        Room room = tenant.getRoom();
        if (room != null && room.getStatus() != RoomStatus.MAINTENANCE && tenantRepo.countByRoom(room) == 0) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepo.save(room);
        }
        return new ResponseEntity<String>("Tenant deleted", HttpStatus.NO_CONTENT);
    }
}
