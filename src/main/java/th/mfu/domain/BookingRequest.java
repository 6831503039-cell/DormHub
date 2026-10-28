package th.mfu.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.format.annotation.DateTimeFormat;

/** A booking request sent from the 3D room map (public page or the tenant app) by someone who wants to rent a room. */
@Entity
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // many requests -> one room
    @ManyToOne
    @JsonBackReference("room-bookings")
    private Room room;

    // the logged-in account that sent the request from the tenant app (/app/rent); null when sent from the public page
    @ManyToOne
    @JsonIgnore
    private AppUser applicant;

    private String fullName;
    private String phone;
    private String email;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate moveInDate;

    @Column(length = 500)
    private String note;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

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

    public AppUser getApplicant() {
        return applicant;
    }
    public void setApplicant(AppUser applicant) {
        this.applicant = applicant;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getMoveInDate() {
        return moveInDate;
    }
    public void setMoveInDate(LocalDate moveInDate) {
        this.moveInDate = moveInDate;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BookingStatus getStatus() {
        return status;
    }
    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
