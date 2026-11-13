package th.mfu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import th.mfu.domain.Room;
import th.mfu.domain.Tenant;

// Like SaleOrderRepositoryTest in sample-boot-relation:
// tenants saved with a room can be found again THROUGH that room.
@DataJpaTest
public class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private TenantRepository tenantRepo;

    @Test
    public void testTenantsBelongToRoom() {
        Room room = new Room();
        room.setRoomNumber("B101");
        room.setCapacity(2);
        roomRepo.save(room);

        Tenant tenant1 = new Tenant();
        tenant1.setFirstName("Alice");
        tenant1.setLastName("Smith");
        tenant1.setRoom(room);
        tenantRepo.save(tenant1);

        Tenant tenant2 = new Tenant();
        tenant2.setFirstName("Bob");
        tenant2.setLastName("Brown");
        tenant2.setRoom(room);
        tenantRepo.save(tenant2);

        List<Tenant> tenants = tenantRepo.findByRoomOrderByFirstNameAsc(room);

        assertEquals(2, tenants.size());
        assertEquals("Alice", tenants.get(0).getFirstName());
        assertEquals("B101", tenants.get(0).getRoom().getRoomNumber());
        assertEquals(2, tenantRepo.countByRoom(room));
    }

    @Test
    public void testNoTenantsForOtherRoom() {
        Room room1 = new Room();
        room1.setRoomNumber("B101");
        roomRepo.save(room1);

        Room room2 = new Room();
        room2.setRoomNumber("B102");
        roomRepo.save(room2);

        Tenant tenant = new Tenant();
        tenant.setFirstName("Alice");
        tenant.setRoom(room1);
        tenantRepo.save(tenant);

        assertEquals(1, tenantRepo.findByRoomOrderByFirstNameAsc(room1).size());
        assertEquals(0, tenantRepo.findByRoomOrderByFirstNameAsc(room2).size());
    }

    @Test
    public void testSearchRoomsByNumberOrType() {
        Room room1 = new Room();
        room1.setRoomNumber("B101");
        room1.setRoomType("Standard");
        roomRepo.save(room1);

        Room room2 = new Room();
        room2.setRoomNumber("C201");
        room2.setRoomType("Deluxe");
        roomRepo.save(room2);

        List<Room> byNumber = roomRepo.findByRoomNumberContainingIgnoreCaseOrRoomTypeContainingIgnoreCaseOrderByRoomNumberAsc("b10", "b10");
        List<Room> byType = roomRepo.findByRoomNumberContainingIgnoreCaseOrRoomTypeContainingIgnoreCaseOrderByRoomNumberAsc("deluxe", "deluxe");

        assertEquals(1, byNumber.size());
        assertEquals("B101", byNumber.get(0).getRoomNumber());
        assertEquals(1, byType.size());
        assertEquals("C201", byType.get(0).getRoomNumber());
    }
}
