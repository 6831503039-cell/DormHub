package th.mfu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import th.mfu.AppUserRepository;
import th.mfu.BillRepository;
import th.mfu.FeedbackRepository;
import th.mfu.LaundryMachineRepository;
import th.mfu.RepairRequestRepository;
import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.config.DormProperties;
import th.mfu.domain.AppUser;
import th.mfu.domain.Bill;
import th.mfu.domain.Feedback;
import th.mfu.domain.LaundryMachine;
import th.mfu.domain.MachineType;
import th.mfu.domain.RepairRequest;
import th.mfu.domain.RepairStatus;
import th.mfu.domain.Role;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Season;
import th.mfu.domain.Tenant;

/**
 * Runs at startup: makes sure the dorm's admin account exists, then fills an empty database with demo data.
 * Bills cover the last 12 months relative to today so charts and reminders always look current.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    static final String DEMO_TENANT_PASSWORD = "tenant123";

    @Autowired
    private AppUserRepository users;
    @Autowired
    private RoomRepository rooms;
    @Autowired
    private TenantRepository tenants;
    @Autowired
    private BillRepository bills;
    @Autowired
    private RepairRequestRepository repairs;
    @Autowired
    private FeedbackRepository feedbacks;
    @Autowired
    private LaundryMachineRepository machines;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DormProperties props;
    @Autowired
    private Clock clock;

    @Override
    public void run(String... args) {
        ensureAdminAccount();
        if (rooms.count() == 0) {
            seedDemoData();
        }
    }

    /** The admin login is fixed by configuration (dormhub.admin.*) and always has the ADMIN role. */
    private void ensureAdminAccount() {
        String email = props.getAdmin().getEmail().trim().toLowerCase();
        AppUser admin = users.findByEmailIgnoreCase(email).orElseGet(AppUser::new);
        admin.setEmail(email);
        admin.setFullName("ผู้ดูแลหอพัก");
        admin.setRole(Role.ADMIN);
        if (admin.getPassword() == null || !passwordEncoder.matches(props.getAdmin().getPassword(), admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(props.getAdmin().getPassword()));
        }
        users.save(admin);
    }

    private void seedDemoData() {
        LocalDate today = LocalDate.now(clock);

        Room r101 = room("101", 1, "แอร์", 4500, 2, RoomStatus.OCCUPIED);
        Room r102 = room("102", 1, "แอร์", 4500, 2, RoomStatus.OCCUPIED);
        room("103", 1, "พัดลม", 3000, 1, RoomStatus.AVAILABLE);
        Room r104 = room("104", 1, "พัดลม", 3000, 1, RoomStatus.MAINTENANCE);
        Room r201 = room("201", 2, "แอร์ (ห้องใหญ่)", 6000, 3, RoomStatus.OCCUPIED);
        room("202", 2, "แอร์ (ห้องใหญ่)", 6000, 3, RoomStatus.AVAILABLE);
        room("203", 2, "พัดลม", 3200, 1, RoomStatus.AVAILABLE);
        Room r204 = room("204", 2, "แอร์", 5000, 2, RoomStatus.OCCUPIED);

        tenant("สมชาย", "ใจดี", "081-111-1111", "somchai@example.com", today.minusMonths(14), r101);
        tenant("สมศักดิ์", "มั่นคง", "081-222-2222", "somsak@example.com", today.minusMonths(14), r101);
        tenant("สุดา", "รักเรียน", "082-333-3333", "suda@example.com", today.minusMonths(13), r102);
        tenant("มานี", "มีสุข", "083-444-4444", "manee@example.com", today.minusMonths(13), r201);
        tenant("ชูใจ", "ใฝ่ดี", "083-555-5555", "chujai@example.com", today.minusMonths(13), r201);
        tenant("วีระ", "กล้าหาญ", "083-666-6666", "weera@example.com", today.minusMonths(13), r201);
        tenant("ปิติ", "ยินดี", "084-777-7777", "piti@example.com", today.minusMonths(13), r204);

        // demo tenant logins (linked to the tenant records above by email)
        AppUser somchai = demoUser("somchai@example.com", "สมชาย ใจดี");
        AppUser suda = demoUser("suda@example.com", "สุดา รักเรียน");
        AppUser manee = demoUser("manee@example.com", "มานี มีสุข");

        Room[] billed = { r101, r102, r201, r204 };
        double[] usageFactor = { 1.0, 0.8, 1.6, 1.1 };
        YearMonth current = YearMonth.from(today);
        for (int monthsAgo = 12; monthsAgo >= 1; monthsAgo--) {
            YearMonth ym = current.minusMonths(monthsAgo);
            for (int i = 0; i < billed.length; i++) {
                int units = (int) Math.round(seasonalUnits(ym) * usageFactor[i]) + ((monthsAgo * 7 + i * 13) % 21) - 10;
                if (monthsAgo == 1 && billed[i] == r204) {
                    units = (int) (units * 1.8); // abnormal spike for the demo alert
                }
                // latest month: 101, 201 and 204 not paid yet
                boolean paid = monthsAgo > 1 || billed[i] == r102;
                bill(billed[i], ym, units, 4 + (monthsAgo + i) % 5, paid);
            }
        }

        repair(r104, "พัดลมเพดานไม่หมุน", "เปิดแล้วมีเสียงแต่ใบพัดไม่หมุน", today.minusDays(11), RepairStatus.IN_PROGRESS, null);
        repair(r102, "ก๊อกน้ำห้องน้ำรั่ว", "น้ำหยดตลอดเวลา", today.minusDays(3), RepairStatus.PENDING, "สุดา รักเรียน");
        repair(r201, "หลอดไฟหน้าห้องขาด", null, today.minusDays(31), RepairStatus.DONE, "มานี มีสุข");

        feedback(somchai, r101, "อินเทอร์เน็ต", 3, "Wi-Fi ชั้น 1 ช่วงค่ำช้ามาก อยากให้เพิ่มตัวกระจายสัญญาณ", 2, false);
        feedback(suda, r102, "ความสะอาด", 5, "แม่บ้านทำความสะอาดทางเดินดีมากค่ะ", 6, true);
        feedback(manee, r201, "สิ่งอำนวยความสะดวก", 4, "อยากให้มีเครื่องอบผ้าเพิ่มอีก 1 เครื่อง ช่วงเย็นคิวยาว", 1, false);
        feedback(somchai, r101, "ความปลอดภัย", 4, "ไฟหน้าประตูทางเข้าดับ ช่วงกลางคืนมืด", 9, true);

        LocalDateTime now = LocalDateTime.now(clock);
        machines.save(new LaundryMachine("เครื่องซักผ้า 1", MachineType.WASHER));
        LaundryMachine washer2 = new LaundryMachine("เครื่องซักผ้า 2", MachineType.WASHER);
        washer2.setBusyUntil(now.plusMinutes(25));
        washer2.setUsedBy("ห้อง 201");
        machines.save(washer2);
        machines.save(new LaundryMachine("เครื่องซักผ้า 3", MachineType.WASHER));
        LaundryMachine dryer1 = new LaundryMachine("เครื่องอบผ้า 1", MachineType.DRYER);
        dryer1.setBusyUntil(now.plusMinutes(12));
        dryer1.setUsedBy("ห้อง 102");
        machines.save(dryer1);
        machines.save(new LaundryMachine("เครื่องอบผ้า 2", MachineType.DRYER));
    }

    /** Typical electricity units per month for a standard room in each season. */
    private static double seasonalUnits(YearMonth ym) {
        switch (Season.of(ym.getMonthValue())) {
            case HOT:
                return 175;
            case RAINY:
                return 125;
            default:
                return 90;
        }
    }

    private Room room(String number, int floor, String type, double rent, int capacity, RoomStatus status) {
        Room room = new Room();
        room.setRoomNumber(number);
        room.setFloor(floor);
        room.setRoomType(type);
        room.setMonthlyRent(rent);
        room.setCapacity(capacity);
        room.setStatus(status);
        return rooms.save(room);
    }

    private void tenant(String first, String last, String phone, String email, LocalDate checkIn, Room room) {
        Tenant t = new Tenant();
        t.setFirstName(first);
        t.setLastName(last);
        t.setPhone(phone);
        t.setEmail(email);
        t.setCheckInDate(checkIn);
        t.setRoom(room);
        tenants.save(t);
    }

    private AppUser demoUser(String email, String name) {
        return users.save(new AppUser(email, passwordEncoder.encode(DEMO_TENANT_PASSWORD), name, Role.TENANT));
    }

    private void bill(Room room, YearMonth ym, int electricUnits, int waterUnits, boolean paid) {
        Bill b = new Bill();
        b.setRoom(room);
        b.setBillMonth(ym.toString());
        b.setRent(room.getMonthlyRent());
        b.setElectricUnits(electricUnits);
        b.setWaterUnits(waterUnits);
        b.setPaid(paid);
        b.setDueDate(ym.plusMonths(1).atDay(props.getDueDay()));
        bills.save(b);
    }

    private void repair(Room room, String title, String description, LocalDate date, RepairStatus status, String by) {
        RepairRequest r = new RepairRequest();
        r.setRoom(room);
        r.setTitle(title);
        r.setDescription(description);
        r.setReportedDate(date);
        r.setStatus(status);
        r.setReportedBy(by);
        repairs.save(r);
    }

    private void feedback(AppUser author, Room room, String category, int rating, String message, int daysAgo, boolean read) {
        Feedback f = new Feedback();
        f.setAuthor(author);
        f.setRoom(room);
        f.setCategory(category);
        f.setRating(rating);
        f.setMessage(message);
        f.setCreatedAt(LocalDateTime.now(clock).minusDays(daysAgo));
        f.setReadByOwner(read);
        feedbacks.save(f);
    }
}
