package th.mfu.app;

import java.util.List;

import th.mfu.domain.Bill;
import th.mfu.domain.Payment;

/** A bill as one tenant sees it: their share, what is still owed, the PromptPay QR and their slips. */
public class TenantBillView {

    private final Bill bill;
    private final long occupants;
    private final double share;
    private final double outstanding;
    private final String qrPayload;
    private final List<Payment> payments;

    public TenantBillView(Bill bill, long occupants, double share, double outstanding, String qrPayload, List<Payment> payments) {
        this.bill = bill;
        this.occupants = occupants;
        this.share = share;
        this.outstanding = outstanding;
        this.qrPayload = qrPayload;
        this.payments = payments;
    }

    public Bill getBill() {
        return bill;
    }

    public long getOccupants() {
        return occupants;
    }

    public double getShare() {
        return share;
    }

    public double getOutstanding() {
        return outstanding;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public List<Payment> getPayments() {
        return payments;
    }
}
