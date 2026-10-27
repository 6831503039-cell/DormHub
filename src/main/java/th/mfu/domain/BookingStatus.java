package th.mfu.domain;

public enum BookingStatus {
    PENDING("รอติดต่อกลับ", "warning"),
    CONFIRMED("ยืนยันแล้ว", "success"),
    CANCELLED("ยกเลิก", "secondary");

    private final String label;
    // Bootstrap color used for the status badge
    private final String badge;

    BookingStatus(String label, String badge) {
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
