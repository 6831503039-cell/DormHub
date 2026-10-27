package th.mfu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import th.mfu.domain.Bill;

public class BillTest {

    @Test
    public void totalIsRentPlusWaterPlusElectric() {
        Bill bill = new Bill();
        bill.setRent(4500.0);
        bill.setWaterUnits(5);      // 5 * 20 = 100
        bill.setElectricUnits(100); // 100 * 7 = 700

        assertEquals(100.0, bill.getWaterCost());
        assertEquals(700.0, bill.getElectricCost());
        assertEquals(5300.0, bill.getTotal());
    }

    @Test
    public void blankValuesCountAsZero() {
        Bill bill = new Bill();
        bill.setWaterUnits(null);

        assertEquals(bill.getElectricCost(), bill.getTotal());
    }
}
