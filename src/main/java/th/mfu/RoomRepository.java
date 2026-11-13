package th.mfu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;

public interface RoomRepository extends CrudRepository<Room, Long> {

    List<Room> findAllByOrderByRoomNumberAsc();

    Optional<Room> findByRoomNumber(String roomNumber);

    long countByStatus(RoomStatus status);

    // search on the database (Spring Data derived query): Spring builds
    // "WHERE LOWER(roomNumber) LIKE %q% OR LOWER(roomType) LIKE %q% ORDER BY roomNumber" from the method name
    List<Room> findByRoomNumberContainingIgnoreCaseOrRoomTypeContainingIgnoreCaseOrderByRoomNumberAsc(
            String roomNumber, String roomType);
}
