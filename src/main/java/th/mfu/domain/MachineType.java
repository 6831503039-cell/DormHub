package th.mfu.domain;

public enum MachineType {
    WASHER("เครื่องซักผ้า", 40),
    DRYER("เครื่องอบผ้า", 50);

    private final String label;
    // length of one simulated run
    private final int minutes;

    MachineType(String label, int minutes) {
        this.label = label;
        this.minutes = minutes;
    }

    public String getLabel() {
        return label;
    }

    public int getMinutes() {
        return minutes;
    }
}
