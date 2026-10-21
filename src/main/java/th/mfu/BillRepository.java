package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.Bill;
import th.mfu.domain.Room;

public interface BillRepository extends CrudRepository<Bill, Long> {

    List<Bill> findAllByOrderByBillMonthDescIdDesc();

    List<Bill> findAllByOrderByBillMonthAsc();

    List<Bill> findByPaidFalseOrderByBillMonthAsc();

    List<Bill> findByRoomOrderByBillMonthAsc(Room room);

    List<Bill> findByRoomOrderByBillMonthDesc(Room room);
}
