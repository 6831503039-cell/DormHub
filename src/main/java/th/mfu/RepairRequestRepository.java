package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.RepairRequest;
import th.mfu.domain.RepairStatus;

public interface RepairRequestRepository extends CrudRepository<RepairRequest, Long> {

    List<RepairRequest> findAllByOrderByReportedDateDescIdDesc();

    long countByStatusNot(RepairStatus status);
}
