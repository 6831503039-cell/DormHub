package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.Parcel;
import th.mfu.domain.Room;

public interface ParcelRepository extends CrudRepository<Parcel, Long> {

    List<Parcel> findAllByOrderByArrivedAtDesc();

    List<Parcel> findByRoomOrderByArrivedAtDesc(Room room);

    long countByRoomAndPickedUpAtIsNull(Room room);

    long countByPickedUpAtIsNull();
}
