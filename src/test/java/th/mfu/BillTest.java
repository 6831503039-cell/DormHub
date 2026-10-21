package th.mfu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import th.mfu.domain.Bill;

public class BillTest {

    @Test
    public void totalIsRentPlusWaterPlusElectric() {
        Bill bill = new Bill();
        bill.setRent(4500.0);
        bill.setWaterUnits(5);      // 5 * 18 = 90
        bill.setElectricUnits(100); // 100 * 8 = 800

        assertEquals(90.0, bill.getWaterCost());
        assertEquals(800.0, bill.getElectricCost());
        assertEquals(5390.0, bill.getTotal());
    }

    @Test
    public void blankValuesCountAsZero() {
        Bill bill = new Bill();
        bill.setWaterUnits(null);

        assertEquals(bill.getElectricCost(), bill.getTotal());
    }
}
