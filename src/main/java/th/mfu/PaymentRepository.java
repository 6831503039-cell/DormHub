package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.Bill;
import th.mfu.domain.Payment;
import th.mfu.domain.PaymentStatus;
import th.mfu.domain.Tenant;

public interface PaymentRepository extends CrudRepository<Payment, Long> {

    List<Payment> findAllByOrderBySubmittedAtDesc();

    List<Payment> findByBillOrderBySubmittedAtDesc(Bill bill);

    List<Payment> findByTenant(Tenant tenant);

    long countByStatusIn(List<PaymentStatus> statuses);
}
