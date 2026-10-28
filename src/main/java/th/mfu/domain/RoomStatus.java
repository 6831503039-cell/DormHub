package th.mfu.domain;

public enum RoomStatus {
    AVAILABLE("success"),
    OCCUPIED("primary"),
    MAINTENANCE("warning");

    // Bootstrap color used for the status badge
    private final String badge;

    RoomStatus(String badge) {
        this.badge = badge;
    }

    /** Message key of the status name shown in the UI (messages*.properties). */
    public String getKey() {
        return "roomStatus." + name();
    }

    public String getBadge() {
        return badge;
    }
}
