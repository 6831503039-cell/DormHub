package th.mfu.domain;

import java.time.Duration;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/** Shared washer/dryer. The status is simulated (mockup): a run just sets busyUntil. */
@Entity
public class LaundryMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Enumerated(EnumType.STRING)
    private MachineType type;

    private LocalDateTime busyUntil;
    private String usedBy;

    public LaundryMachine() {
    }

    public LaundryMachine(String name, MachineType type) {
        this.name = name;
        this.type = type;
    }

    public boolean isAvailableAt(LocalDateTime now) {
        return busyUntil == null || !busyUntil.isAfter(now);
    }

    public long minutesLeftAt(LocalDateTime now) {
        return isAvailableAt(now) ? 0 : Duration.between(now, busyUntil).toMinutes() + 1;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public MachineType getType() {
        return type;
    }
    public void setType(MachineType type) {
        this.type = type;
    }

    public LocalDateTime getBusyUntil() {
        return busyUntil;
    }
    public void setBusyUntil(LocalDateTime busyUntil) {
        this.busyUntil = busyUntil;
    }

    public String getUsedBy() {
        return usedBy;
    }
    public void setUsedBy(String usedBy) {
        this.usedBy = usedBy;
    }
}
