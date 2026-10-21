package th.mfu.service;

import th.mfu.domain.PaymentStatus;

/** Outcome of checking an uploaded transfer slip. */
public class SlipCheck {

    private final PaymentStatus status;
    private final String note;

    public SlipCheck(PaymentStatus status, String note) {
        this.status = status;
        this.note = note;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
