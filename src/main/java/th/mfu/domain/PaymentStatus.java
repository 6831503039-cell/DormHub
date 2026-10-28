package th.mfu.domain;

public enum PaymentStatus {
    PENDING("warning"),
    APPROVED("success"),
    SUSPICIOUS("danger"),
    REJECTED("secondary");

    private final String badge;

    PaymentStatus(String badge) {
        this.badge = badge;
    }

    /** Message key of the status name shown in the UI (messages*.properties). */
    public String getKey() {
        return "paymentStatus." + name();
    }

    public String getBadge() {
        return badge;
    }
}
