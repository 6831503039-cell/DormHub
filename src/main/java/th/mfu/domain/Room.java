package th.mfu.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomNumber;
    private Integer floor;
    private String roomType;
    private Double monthlyRent;
    // how many tenants can share the room (big rooms: up to 3)
    private Integer capacity = 1;

    @Enumerated(EnumType.STRING)
    private RoomStatus status = RoomStatus.AVAILABLE;

    // one room -> many tenants; "mappedBy" = Tenant.room owns the room_id column
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @OrderBy("firstName ASC")
    @JsonManagedReference("room-tenants")
    private List<Tenant> tenants;

    // one room -> many bills; Bill.room owns the room_id column
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @OrderBy("billMonth ASC")
    @JsonManagedReference("room-bills")
    private List<Bill> bills;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getFloor() {
        return floor;
    }
    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public String getRoomType() {
        return roomType;
    }
    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public Double getMonthlyRent() {
        return monthlyRent;
    }
    public void setMonthlyRent(Double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public Integer getCapacity() {
        return capacity;
    }
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public List<Tenant> getTenants() {
        return tenants;
    }
    public void setTenants(List<Tenant> tenants) {
        this.tenants = tenants;
    }

    public List<Bill> getBills() {
        return bills;
    }
    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public RoomStatus getStatus() {
        return status;
    }
    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}
