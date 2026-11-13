package th.mfu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import th.mfu.domain.Room;
import th.mfu.domain.Tenant;

public interface TenantRepository extends CrudRepository<Tenant, Long> {

    List<Tenant> findAllByOrderByFirstNameAsc();

    List<Tenant> findByRoomOrderByFirstNameAsc(Room room);

    Optional<Tenant> findFirstByEmailIgnoreCase(String email);

    long countByRoom(Room room);

    // search on the database (JPQL): name, full name, phone, email or room number contains the keyword.
    // LEFT JOIN keeps tenants that have no room yet.
    @Query("SELECT t FROM Tenant t LEFT JOIN t.room r "
            + "WHERE LOWER(t.firstName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(t.lastName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(CONCAT(t.firstName, ' ', t.lastName)) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR t.phone LIKE CONCAT('%', :q, '%') "
            + "OR LOWER(t.email) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "ORDER BY t.firstName ASC")
    List<Tenant> search(@Param("q") String q);
}
