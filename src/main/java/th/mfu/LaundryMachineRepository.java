package th.mfu;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import th.mfu.domain.LaundryMachine;

public interface LaundryMachineRepository extends CrudRepository<LaundryMachine, Long> {

    List<LaundryMachine> findAllByOrderByNameAsc();
}
