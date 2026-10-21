package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.RepairRequest;
import th.mfu.domain.RepairStatus;
import th.mfu.domain.Room;

public interface RepairRequestRepository extends CrudRepository<RepairRequest, Long> {

    List<RepairRequest> findAllByOrderByReportedDateDescIdDesc();

    List<RepairRequest> findByRoomOrderByReportedDateDescIdDesc(Room room);

    long countByStatusNot(RepairStatus status);
}
