package th.mfu.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import th.mfu.BillRepository;
import th.mfu.RoomRepository;
import th.mfu.domain.Bill;
import th.mfu.domain.Room;
import th.mfu.domain.Season;

/** Electricity trends for the owner dashboard. */
@Service
public class AnalyticsService {

    // latest usage this much above the recent average counts as abnormal
    static final double ABNORMAL_RATIO = 1.3;

    private final BillRepository billRepository;
    private final RoomRepository roomRepository;

    public AnalyticsService(BillRepository billRepository, RoomRepository roomRepository) {
        this.billRepository = billRepository;
        this.roomRepository = roomRepository;
    }

    /** Totals per month over all rooms, oldest first: month -> [units, electricity cost, bill total]. */
    public Map<String, double[]> monthlyTotals(int months) {
        TreeMap<String, double[]> totals = new TreeMap<>();
        for (Bill b : billRepository.findAllByOrderByBillMonthAsc()) {
            if (!totals.containsKey(b.getBillMonth())) {
                totals.put(b.getBillMonth(), new double[3]);
            }
            double[] t = totals.get(b.getBillMonth());
            t[0] += value(b.getElectricUnits());
            t[1] += b.getElectricCost();
            t[2] += b.getTotal();
        }
        while (totals.size() > months) {
            totals.pollFirstEntry();
        }
        return totals;
    }

    /** Average electricity units per room-month in each season. */
    public Map<Season, Double> seasonAverages() {
        return seasonAverages(billRepository.findAllByOrderByBillMonthAsc());
    }

    static Map<Season, Double> seasonAverages(List<Bill> bills) {
        Map<Season, double[]> sums = new EnumMap<>(Season.class);
        for (Bill b : bills) {
            Season season = Season.of(YearMonth.parse(b.getBillMonth()).getMonthValue());
            if (!sums.containsKey(season)) {
                sums.put(season, new double[2]);
            }
            double[] s = sums.get(season);
            s[0] += value(b.getElectricUnits());
            s[1]++;
        }
        Map<Season, Double> averages = new EnumMap<>(Season.class);
        for (Season season : Season.values()) {
            double[] s = sums.get(season);
            averages.put(season, s == null ? 0 : Math.round(s[0] / s[1] * 10) / 10.0);
        }
        return averages;
    }

    public List<AbnormalUsage> abnormalRooms() {
        List<AbnormalUsage> result = new ArrayList<>();
        for (Room room : roomRepository.findAllByOrderByRoomNumberAsc()) {
            Optional<AbnormalUsage> usage = abnormalUsage(room);
            if (usage.isPresent()) {
                result.add(usage.get());
            }
        }
        return result;
    }

    public Optional<AbnormalUsage> abnormalUsage(Room room) {
        return abnormal(room, billRepository.findByRoomOrderByBillMonthAsc(room));
    }

    /** Compares the latest bill with the average of up to 3 bills before it. */
    static Optional<AbnormalUsage> abnormal(Room room, List<Bill> billsOldestFirst) {
        int n = billsOldestFirst.size();
        if (n < 2) {
            return Optional.empty();
        }
        Bill latest = billsOldestFirst.get(n - 1);
        List<Bill> previous = billsOldestFirst.subList(Math.max(0, n - 4), n - 1);
        double average = 0;
        if (!previous.isEmpty()) {
            double sum = 0;
            for (Bill b : previous) {
                sum += value(b.getElectricUnits());
            }
            average = sum / previous.size();
        }
        int units = (int) value(latest.getElectricUnits());
        if (average > 0 && units > average * ABNORMAL_RATIO) {
            return Optional.of(new AbnormalUsage(room, latest.getBillMonth(), units, Math.round(average * 10) / 10.0));
        }
        return Optional.empty();
    }

    private static double value(Number n) {
        return n == null ? 0 : n.doubleValue();
    }
}
