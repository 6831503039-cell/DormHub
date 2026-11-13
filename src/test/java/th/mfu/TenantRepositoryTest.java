package th.mfu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import th.mfu.domain.Room;
import th.mfu.domain.Tenant;

// Tests the tenant search query (TenantRepository.search) on a real H2 database
@DataJpaTest
public class TenantRepositoryTest {

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private TenantRepository tenantRepo;

    private void addTenants() {
        Room room = new Room();
        room.setRoomNumber("B101");
        roomRepo.save(room);

        Tenant alice = new Tenant();
        alice.setFirstName("Alice");
        alice.setLastName("Smith");
        alice.setPhone("081-111-1111");
        alice.setEmail("alice@example.com");
        alice.setRoom(room);
        tenantRepo.save(alice);

        // a tenant without a room must still be found
        Tenant bob = new Tenant();
        bob.setFirstName("Bob");
        bob.setLastName("Brown");
        bob.setPhone("082-222-2222");
        bob.setEmail("bob@example.com");
        tenantRepo.save(bob);
    }

    @Test
    public void testSearchByName() {
        addTenants();
        List<Tenant> result = tenantRepo.search("alice");
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getFirstName());

        assertEquals(1, tenantRepo.search("Bob Brown").size());
    }

    @Test
    public void testSearchByPhoneEmailAndRoom() {
        addTenants();
        assertEquals("Bob", tenantRepo.search("082-222").get(0).getFirstName());
        assertEquals("Alice", tenantRepo.search("alice@example").get(0).getFirstName());
        assertEquals("Alice", tenantRepo.search("b101").get(0).getFirstName());
    }

    @Test
    public void testSearchNotFound() {
        addTenants();
        assertEquals(0, tenantRepo.search("nobody").size());
    }
}
