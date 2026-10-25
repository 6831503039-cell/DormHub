package th.mfu.domain;

import java.time.LocalDateTime;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonBackReference;

/** A transfer slip a tenant uploaded for (part of) a bill. */
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    @JsonBackReference("bill-payments")
    private Bill bill;

    // null if the tenant record was deleted later
    @ManyToOne
    private Tenant tenant;

    private Double amount;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] slip;
    private String slipContentType;

    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    // result of the AI slip check, shown to the admin
    @Column(length = 1000)
    private String aiNote;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Bill getBill() {
        return bill;
    }
    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Tenant getTenant() {
        return tenant;
    }
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Double getAmount() {
        return amount;
    }
    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public byte[] getSlip() {
        return slip;
    }
    public void setSlip(byte[] slip) {
        this.slip = slip;
    }

    public String getSlipContentType() {
        return slipContentType;
    }
    public void setSlipContentType(String slipContentType) {
        this.slipContentType = slipContentType;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public PaymentStatus getStatus() {
        return status;
    }
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getAiNote() {
        return aiNote;
    }
    public void setAiNote(String aiNote) {
        this.aiNote = aiNote;
    }
}
