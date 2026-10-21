package th.mfu.service;

import th.mfu.domain.Room;

/** A room whose latest electricity usage is well above its recent average. */
public class AbnormalUsage {

    private final Room room;
    private final String billMonth;
    private final int units;
    private final double average;

    public AbnormalUsage(Room room, String billMonth, int units, double average) {
        this.room = room;
        this.billMonth = billMonth;
        this.units = units;
        this.average = average;
    }

    public int getPercentAbove() {
        return (int) Math.round((units / average - 1) * 100);
    }

    public Room getRoom() {
        return room;
    }

    public String getBillMonth() {
        return billMonth;
    }

    public int getUnits() {
        return units;
    }

    public double getAverage() {
        return average;
    }
}
