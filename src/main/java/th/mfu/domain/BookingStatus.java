package th.mfu.domain;

public enum BookingStatus {
    PENDING("warning"),
    CONFIRMED("success"),
    CANCELLED("secondary");

    // Bootstrap color used for the status badge
    private final String badge;

    BookingStatus(String badge) {
        this.badge = badge;
    }

    /** Message key of the status name shown in the UI (messages*.properties). */
    public String getKey() {
        return "bookingStatus." + name();
    }

    public String getBadge() {
        return badge;
    }
}
