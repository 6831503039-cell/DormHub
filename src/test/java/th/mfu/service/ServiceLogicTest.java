package th.mfu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import th.mfu.config.DormProperties;
import th.mfu.domain.Bill;
import th.mfu.domain.PaymentStatus;
import th.mfu.domain.Room;
import th.mfu.domain.Season;

/** Plain unit tests for the date/number logic, no Spring context needed. */
public class ServiceLogicTest {

    @Test
    public void billIsSplitEquallyBetweenRoommates() {
        assertEquals(2000.0, BillingService.split(6000, 3));
        assertEquals(3333.33, BillingService.split(10000, 3));
        assertEquals(500.0, BillingService.split(500, 0)); // empty room: one payer
    }

    @Test
    public void thaiSeasons() {
        assertEquals(Season.HOT, Season.of(4));
        assertEquals(Season.RAINY, Season.of(8));
        assertEquals(Season.COOL, Season.of(12));
        assertEquals(Season.COOL, Season.of(1));
    }

    @Test
    public void seasonAveragesUseBillMonth() {
        List<Bill> bills = List.of(bill("2026-04", 200), bill("2026-05", 100), bill("2026-07", 120), bill("2026-12", 80));
        Map<Season, Double> avg = AnalyticsService.seasonAverages(bills);
        assertEquals(150.0, avg.get(Season.HOT));
        assertEquals(120.0, avg.get(Season.RAINY));
        assertEquals(80.0, avg.get(Season.COOL));
    }

    @Test
    public void paymentReminderStartsFiveDaysBeforeDueDate() {
        LocalDate due = LocalDate.of(2026, 11, 5);
        assertTrue(NotificationService.paymentNotice("2026-10", due, 100, LocalDate.of(2026, 10, 29)).isEmpty());
        assertEquals("warning", NotificationService.paymentNotice("2026-10", due, 100, LocalDate.of(2026, 10, 31)).get().getLevel());
        assertEquals("warning", NotificationService.paymentNotice("2026-10", due, 100, due).get().getLevel());
        assertEquals("danger", NotificationService.paymentNotice("2026-10", due, 100, LocalDate.of(2026, 11, 6)).get().getLevel());
    }

    @Test
    public void electricityReminderInLastSevenDaysOfMonth() {
        List<Bill> bills = List.of(bill("2026-08", 100), bill("2026-09", 100));
        assertTrue(NotificationService.electricityReminder(bills, LocalDate.of(2026, 10, 23)).isEmpty());
        assertFalse(NotificationService.electricityReminder(bills, LocalDate.of(2026, 10, 24)).isEmpty());
        assertFalse(NotificationService.electricityReminder(bills, LocalDate.of(2026, 10, 31)).isEmpty());
    }

    @Test
    public void abnormalElectricityIsDetected() {
        Room room = new Room();
        List<Bill> normal = new ArrayList<>(List.of(bill("2026-06", 100), bill("2026-07", 110), bill("2026-08", 90), bill("2026-09", 120)));
        assertTrue(AnalyticsService.abnormal(room, normal).isEmpty());

        normal.add(bill("2026-10", 200)); // average of previous 3 = 106.7
        AbnormalUsage a = AnalyticsService.abnormal(room, normal).get();
        assertEquals(200, a.getUnits());
        assertEquals(87, a.getPercentAbove());
    }

    @Test
    public void slipDecisionRules() {
        SlipVerificationService service = new SlipVerificationService(null, new ObjectMapper(), new DormProperties());
        assertEquals(PaymentStatus.APPROVED, service.decide(
                "{\"is_transfer_slip\":true,\"amount\":2250.0,\"looks_tampered\":false,\"reason\":\"ok\"}", 2250).getStatus());
        assertEquals(PaymentStatus.PENDING, service.decide(
                "{\"is_transfer_slip\":true,\"amount\":1000,\"looks_tampered\":false}", 2250).getStatus());
        assertEquals(PaymentStatus.SUSPICIOUS, service.decide(
                "{\"is_transfer_slip\":true,\"amount\":2250,\"looks_tampered\":true}", 2250).getStatus());
        assertEquals(PaymentStatus.SUSPICIOUS, service.decide("{\"is_transfer_slip\":false}", 2250).getStatus());
        assertEquals(PaymentStatus.PENDING, service.decide("not json", 2250).getStatus());
    }

    private static Bill bill(String month, int electricUnits) {
        Bill b = new Bill();
        b.setBillMonth(month);
        b.setRent(0.0);
        b.setElectricUnits(electricUnits);
        return b;
    }

    @Test
    public void roomMapStatusFromContractEnd() {
        LocalDate today = LocalDate.of(2026, 10, 26);
        assertEquals("AVAILABLE", RoomMapService.mapStatus(th.mfu.domain.RoomStatus.AVAILABLE, null, today));
        assertEquals("MAINTENANCE", RoomMapService.mapStatus(th.mfu.domain.RoomStatus.MAINTENANCE, today, today));
        // 60 days or less left -> "ใกล้หมดสัญญา"
        assertEquals("SOON", RoomMapService.mapStatus(th.mfu.domain.RoomStatus.OCCUPIED, today.plusDays(60), today));
        assertEquals("OCCUPIED", RoomMapService.mapStatus(th.mfu.domain.RoomStatus.OCCUPIED, today.plusDays(61), today));
        assertEquals("OCCUPIED", RoomMapService.mapStatus(th.mfu.domain.RoomStatus.OCCUPIED, null, today));
        // a "soon" room can be moved into the day after the contract ends
        assertEquals(today.plusDays(11), RoomMapService.availableFrom("SOON", today.plusDays(10), today));
        assertEquals(null, RoomMapService.availableFrom("OCCUPIED", today.plusDays(100), today));
    }

    @Test
    public void roomIsFreeWhenTheLastContractEnds() {
        th.mfu.domain.Tenant a = new th.mfu.domain.Tenant();
        a.setContractEndDate(LocalDate.of(2026, 12, 1));
        th.mfu.domain.Tenant b = new th.mfu.domain.Tenant();
        b.setContractEndDate(LocalDate.of(2027, 1, 15));
        assertEquals(LocalDate.of(2027, 1, 15), RoomMapService.contractEnd(List.of(a, b)));
        b.setContractEndDate(null); // one roommate without an end date: unknown
        assertEquals(null, RoomMapService.contractEnd(List.of(a, b)));
    }
}
