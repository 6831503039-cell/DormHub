package th.mfu.domain;

public enum RoomStatus {
    AVAILABLE("ว่าง", "success"),
    OCCUPIED("มีผู้เช่า", "primary"),
    MAINTENANCE("ปิดซ่อม", "warning");

    private final String label;
    // Bootstrap color used for the status badge
    private final String badge;

    RoomStatus(String label, String badge) {
        this.label = label;
        this.badge = badge;
    }

    public String getLabel() {
        return label;
    }

    public String getBadge() {
        return badge;
    }
}
