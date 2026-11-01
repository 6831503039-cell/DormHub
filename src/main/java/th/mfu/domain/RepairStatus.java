package th.mfu.domain;

public enum RepairStatus {
    PENDING("danger"),
    IN_PROGRESS("warning"),
    DONE("success");

    // Bootstrap color used for the status badge
    private final String badge;

    RepairStatus(String badge) {
        this.badge = badge;
    }

    /** Message key of the status name shown in the UI (messages*.properties). */
    public String getKey() {
        return "repairStatus." + name();
    }

    public String getBadge() {
        return badge;
    }
}
