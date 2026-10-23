package th.mfu.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import th.mfu.LaundryMachineRepository;
import th.mfu.domain.LaundryMachine;

/** Mockup of the shared laundry room: no real machines, a run is simulated with a timer. */
@Service
public class LaundryService {

    @Autowired
    private LaundryMachineRepository repository;
    @Autowired
    private Clock clock;

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public List<LaundryMachine> machines() {
        return repository.findAllByOrderByNameAsc();
    }

    public long availableCount() {
        LocalDateTime now = now();
        return machines().stream().filter(m -> m.isAvailableAt(now)).count();
    }

    /** Returns false if the machine is already running. */
    public boolean start(Long id, String userName) {
        LaundryMachine machine = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!machine.isAvailableAt(now())) {
            return false;
        }
        machine.setBusyUntil(now().plusMinutes(machine.getType().getMinutes()));
        machine.setUsedBy(userName);
        repository.save(machine);
        return true;
    }
}
