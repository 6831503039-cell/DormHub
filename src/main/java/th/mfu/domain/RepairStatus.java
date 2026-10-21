package th.mfu.domain;

public enum RepairStatus {
    PENDING("รอดำเนินการ", "danger"),
    IN_PROGRESS("กำลังซ่อม", "warning"),
    DONE("เสร็จแล้ว", "success");

    private final String label;
    // Bootstrap color used for the status badge
    private final String badge;

    RepairStatus(String label, String badge) {
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
