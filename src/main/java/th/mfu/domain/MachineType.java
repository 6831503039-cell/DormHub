package th.mfu.domain;

public enum MachineType {
    WASHER(40),
    DRYER(50);

    // length of one simulated run
    private final int minutes;

    MachineType(int minutes) {
        this.minutes = minutes;
    }

    /** Message key of the machine type name (messages*.properties). */
    public String getKey() {
        return "machineType." + name();
    }

    public int getMinutes() {
        return minutes;
    }
}
