package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.Bill;

public interface BillRepository extends CrudRepository<Bill, Long> {

    List<Bill> findAllByOrderByBillMonthDescIdDesc();

    List<Bill> findByPaidFalseOrderByBillMonthAsc();
}
