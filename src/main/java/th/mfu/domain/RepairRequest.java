package th.mfu.domain;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class RepairRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    private Room room;

    private String title;

    @Column(length = 1000)
    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate reportedDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    private RepairStatus status = RepairStatus.PENDING;

    // name of the tenant who reported it from the tenant app (blank when the admin adds it)
    private String reportedBy;

    public String getReportedBy() {
        return reportedBy;
    }
    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Room getRoom() {
        return room;
    }
    public void setRoom(Room room) {
        this.room = room;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getReportedDate() {
        return reportedDate;
    }
    public void setReportedDate(LocalDate reportedDate) {
        this.reportedDate = reportedDate;
    }

    public RepairStatus getStatus() {
        return status;
    }
    public void setStatus(RepairStatus status) {
        this.status = status;
    }
}
