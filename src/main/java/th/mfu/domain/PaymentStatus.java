package th.mfu.domain;

public enum PaymentStatus {
    PENDING("รอตรวจสอบ", "warning"),
    APPROVED("อนุมัติแล้ว", "success"),
    SUSPICIOUS("สงสัยสลิปปลอม", "danger"),
    REJECTED("ไม่อนุมัติ", "secondary");

    private final String label;
    private final String badge;

    PaymentStatus(String label, String badge) {
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
