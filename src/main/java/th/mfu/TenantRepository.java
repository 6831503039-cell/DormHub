package th.mfu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.Room;
import th.mfu.domain.Tenant;

public interface TenantRepository extends CrudRepository<Tenant, Long> {

    List<Tenant> findAllByOrderByFirstNameAsc();

    List<Tenant> findByRoomOrderByFirstNameAsc(Room room);

    Optional<Tenant> findFirstByEmailIgnoreCase(String email);

    long countByRoom(Room room);
}
