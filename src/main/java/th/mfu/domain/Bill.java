package th.mfu.domain;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // many bills -> one room; this side OWNS the relationship (room_id column)
    @ManyToOne(optional = false)
    @JsonBackReference("room-bills")
    private Room room;

    // one bill -> many payment slips; Payment.bill owns the bill_id column
    @OneToMany(mappedBy = "bill", fetch = FetchType.LAZY)
    @OrderBy("submittedAt DESC")
    @JsonManagedReference("bill-payments")
    private List<Payment> payments;

    // format yyyy-MM, matches <input type="month">
    private String billMonth;
    private Double rent;
    private Integer waterUnits = 0;
    private Double waterRate = 18.0;
    private Integer electricUnits = 0;
    private Double electricRate = 8.0;
    private boolean paid;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    public double getWaterCost() {
        return value(waterUnits) * value(waterRate);
    }

    public double getElectricCost() {
        return value(electricUnits) * value(electricRate);
    }

    public double getTotal() {
        return value(rent) + getWaterCost() + getElectricCost();
    }

    private static double value(Number n) {
        return n == null ? 0 : n.doubleValue();
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

    public String getBillMonth() {
        return billMonth;
    }
    public void setBillMonth(String billMonth) {
        this.billMonth = billMonth;
    }

    public Double getRent() {
        return rent;
    }
    public void setRent(Double rent) {
        this.rent = rent;
    }

    public Integer getWaterUnits() {
        return waterUnits;
    }
    public void setWaterUnits(Integer waterUnits) {
        this.waterUnits = waterUnits;
    }

    public Double getWaterRate() {
        return waterRate;
    }
    public void setWaterRate(Double waterRate) {
        this.waterRate = waterRate;
    }

    public Integer getElectricUnits() {
        return electricUnits;
    }
    public void setElectricUnits(Integer electricUnits) {
        this.electricUnits = electricUnits;
    }

    public Double getElectricRate() {
        return electricRate;
    }
    public void setElectricRate(Double electricRate) {
        this.electricRate = electricRate;
    }

    public List<Payment> getPayments() {
        return payments;
    }
    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public boolean isPaid() {
        return paid;
    }
    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
