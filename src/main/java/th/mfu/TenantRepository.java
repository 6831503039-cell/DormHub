package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.Room;
import th.mfu.domain.Tenant;

public interface TenantRepository extends CrudRepository<Tenant, Long> {

    List<Tenant> findAllByOrderByFirstNameAsc();

    long countByRoom(Room room);
}
