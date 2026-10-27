package th.mfu.service;

import java.util.List;

/**
 * One room as the 3D room map needs it (sent as JSON to static/js/dorm3d.js).
 * Tenant names are only filled in for the admin map, never on the public page.
 */
public class RoomView {

    private Long id;
    private String number;
    private String building;
    private int floor;
    private String type;
    private double rent;
    private int capacity;
    private int occupants;
    // AVAILABLE, SOON (contract ends within 60 days), OCCUPIED or MAINTENANCE
    private String status;
    private String statusLabel;
    // dates as yyyy-MM-dd, null when not known
    private String contractEnd;
    private String availableFrom;
    private long pendingBookings;
    private List<String> tenants;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }
    public void setNumber(String number) {
        this.number = number;
    }

    public String getBuilding() {
        return building;
    }
    public void setBuilding(String building) {
        this.building = building;
    }

    public int getFloor() {
        return floor;
    }
    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public double getRent() {
        return rent;
    }
    public void setRent(double rent) {
        this.rent = rent;
    }

    public int getCapacity() {
        return capacity;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupants() {
        return occupants;
    }
    public void setOccupants(int occupants) {
        this.occupants = occupants;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }
    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getContractEnd() {
        return contractEnd;
    }
    public void setContractEnd(String contractEnd) {
        this.contractEnd = contractEnd;
    }

    public String getAvailableFrom() {
        return availableFrom;
    }
    public void setAvailableFrom(String availableFrom) {
        this.availableFrom = availableFrom;
    }

    public long getPendingBookings() {
        return pendingBookings;
    }
    public void setPendingBookings(long pendingBookings) {
        this.pendingBookings = pendingBookings;
    }

    public List<String> getTenants() {
        return tenants;
    }
    public void setTenants(List<String> tenants) {
        this.tenants = tenants;
    }
}
