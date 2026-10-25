package th.mfu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import th.mfu.AppUserRepository;
import th.mfu.BillRepository;
import th.mfu.BookingRequestRepository;
import th.mfu.FeedbackRepository;
import th.mfu.LaundryMachineRepository;
import th.mfu.RepairRequestRepository;
import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.config.DormProperties;
import th.mfu.domain.AppUser;
import th.mfu.domain.Bill;
import th.mfu.domain.BookingRequest;
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

    private final AppUserRepository users;
    private final RoomRepository rooms;
    private final TenantRepository tenants;
    private final BillRepository bills;
    private final RepairRequestRepository repairs;
    private final FeedbackRepository feedbacks;
    private final LaundryMachineRepository machines;
    private final BookingRequestRepository bookings;
    private final PasswordEncoder passwordEncoder;
    private final DormProperties props;
    private final Clock clock;

    public DataSeeder(AppUserRepository users, RoomRepository rooms, TenantRepository tenants, BillRepository bills,
            RepairRequestRepository repairs, FeedbackRepository feedbacks, LaundryMachineRepository machines,
            BookingRequestRepository bookings, PasswordEncoder passwordEncoder, DormProperties props, Clock clock) {
        this.users = users;
        this.rooms = rooms;
        this.tenants = tenants;
        this.bills = bills;
        this.repairs = repairs;
        this.feedbacks = feedbacks;
        this.machines = machines;
        this.bookings = bookings;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
    }

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

        // building A, floors 1-2: the original demo rooms (used by bills, repairs and feedback below)
        Room r101 = room("A101", "A", 1, "แอร์", 4500, 2, RoomStatus.OCCUPIED);
        Room r102 = room("A102", "A", 1, "แอร์", 4500, 2, RoomStatus.OCCUPIED);
        room("A103", "A", 1, "พัดลม", 3000, 1, RoomStatus.AVAILABLE);
        Room r104 = room("A104", "A", 1, "พัดลม", 3000, 1, RoomStatus.MAINTENANCE);
        Room r201 = room("A201", "A", 2, "แอร์ (ห้องใหญ่)", 6000, 3, RoomStatus.OCCUPIED);
        room("A202", "A", 2, "แอร์ (ห้องใหญ่)", 6000, 3, RoomStatus.AVAILABLE);
        room("A203", "A", 2, "พัดลม", 3200, 1, RoomStatus.AVAILABLE);
        Room r204 = room("A204", "A", 2, "แอร์", 5000, 2, RoomStatus.OCCUPIED);

        // contract ends: A101 and A204 end within 60 days, so they show as "ใกล้หมดสัญญา" on the 3D map
        tenant("สมชาย", "ใจดี", "081-111-1111", "somchai@example.com", today.minusMonths(14), today.plusDays(25), r101);
        tenant("สมศักดิ์", "มั่นคง", "081-222-2222", "somsak@example.com", today.minusMonths(14), today.plusDays(25), r101);
        tenant("สุดา", "รักเรียน", "082-333-3333", "suda@example.com", today.minusMonths(13), today.plusMonths(4), r102);
        tenant("มานี", "มีสุข", "083-444-4444", "manee@example.com", today.minusMonths(13), today.plusMonths(5), r201);
        tenant("ชูใจ", "ใฝ่ดี", "083-555-5555", "chujai@example.com", today.minusMonths(13), today.plusMonths(5), r201);
        tenant("วีระ", "กล้าหาญ", "083-666-6666", "weera@example.com", today.minusMonths(13), today.plusMonths(5), r201);
        tenant("ปิติ", "ยินดี", "084-777-7777", "piti@example.com", today.minusMonths(13), today.plusDays(40), r204);

        // the rest of the 3 buildings (A, B, C x 4 floors x 6 rooms) for the 3D room map
        seedBuildingRooms(today);

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
        washer2.setUsedBy("ห้อง A201");
        machines.save(washer2);
        machines.save(new LaundryMachine("เครื่องซักผ้า 3", MachineType.WASHER));
        LaundryMachine dryer1 = new LaundryMachine("เครื่องอบผ้า 1", MachineType.DRYER);
        dryer1.setBusyUntil(now.plusMinutes(12));
        dryer1.setUsedBy("ห้อง A102");
        machines.save(dryer1);
        machines.save(new LaundryMachine("เครื่องอบผ้า 2", MachineType.DRYER));
    }

    private static final String[] FIRST_NAMES = { "กานต์", "ณัฐ", "ธนา", "พิมพ์", "ภูมิ", "มินตรา", "รวิ", "ศุภ", "อร", "ปาล์ม", "เจน", "ต้นข้าว" };
    private static final String[] LAST_NAMES = { "ศรีสุข", "บุญมา", "แก้วใส", "ทองดี", "สายชล", "พรหมมา", "จันทร์เพ็ญ", "วงศ์ดี" };

    /**
     * Fills buildings A, B and C (4 floors x 6 rooms) with a fixed pattern of room types and statuses,
     * skipping the rooms already created above. Rented rooms get tenants with contract end dates.
     */
    private void seedBuildingRooms(LocalDate today) {
        String[] buildings = { "A", "B", "C" };
        Room firstFree = null;
        Room secondFree = null;
        int person = 0;
        for (int b = 0; b < buildings.length; b++) {
            for (int floor = 1; floor <= 4; floor++) {
                for (int i = 1; i <= 6; i++) {
                    String number = buildings[b] + floor + "0" + i;
                    if (rooms.findByRoomNumber(number).isPresent()) {
                        continue;
                    }
                    // corner rooms are big rooms, the middle rooms are fan rooms
                    String type;
                    double rent;
                    int capacity;
                    if (i == 1 || i == 6) {
                        type = "แอร์ (ห้องใหญ่)";
                        rent = 6000;
                        capacity = 3;
                    } else if (i == 2 || i == 5) {
                        type = "แอร์";
                        rent = 4500;
                        capacity = 2;
                    } else {
                        type = "พัดลม";
                        rent = 3000;
                        capacity = 1;
                    }
                    if (floor >= 3) {
                        rent += 300; // upper floors have a better view
                    }

                    int k = (b * 31 + floor * 7 + i * 3) % 10;
                    RoomStatus status = k < 3 ? RoomStatus.AVAILABLE
                            : (k == 3 && floor == 4) ? RoomStatus.MAINTENANCE : RoomStatus.OCCUPIED;
                    Room room = room(number, buildings[b], floor, type, rent, capacity, status);

                    if (status == RoomStatus.AVAILABLE) {
                        if (firstFree == null) {
                            firstFree = room;
                        } else if (secondFree == null && !buildings[b].equals(firstFree.getBuilding())) {
                            secondFree = room;
                        }
                    }
                    if (status == RoomStatus.OCCUPIED) {
                        // k = 4 or 5: contract ends within 60 days ("ใกล้หมดสัญญา")
                        LocalDate contractEnd = (k == 4 || k == 5) ? today.plusDays(10 + k * 7) : today.plusDays(90 + k * 25);
                        int people = Math.min(capacity, 1 + k % capacity);
                        for (int p = 0; p < people; p++) {
                            String first = FIRST_NAMES[person % FIRST_NAMES.length];
                            String last = LAST_NAMES[(person / FIRST_NAMES.length + person) % LAST_NAMES.length];
                            String phone = String.format("09%d-%03d-%04d", person % 10, 100 + person, 1000 + person * 7);
                            String email = number.toLowerCase() + "." + (p + 1) + "@example.com";
                            tenant(first, last, phone, email, contractEnd.minusYears(1), contractEnd, room);
                            person++;
                        }
                    }
                }
            }
        }

        // two demo booking requests from the public 3D map
        if (firstFree != null) {
            booking(firstFree, "ศิริพร นาคดี", "089-123-4567", "siriporn@example.com", today.plusDays(7), "อยากได้ห้องชั้นล่าง", 1);
        }
        if (secondFree != null) {
            booking(secondFree, "อนันต์ ใจกว้าง", "086-555-0101", null, today.plusDays(20), null, 0);
        }
    }

    private void booking(Room room, String name, String phone, String email, LocalDate moveIn, String note, int daysAgo) {
        BookingRequest b = new BookingRequest();
        b.setRoom(room);
        b.setFullName(name);
        b.setPhone(phone);
        b.setEmail(email);
        b.setMoveInDate(moveIn);
        b.setNote(note);
        b.setCreatedAt(LocalDateTime.now(clock).minusDays(daysAgo));
        bookings.save(b);
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

    private Room room(String number, String building, int floor, String type, double rent, int capacity, RoomStatus status) {
        Room room = new Room();
        room.setRoomNumber(number);
        room.setBuilding(building);
        room.setFloor(floor);
        room.setRoomType(type);
        room.setMonthlyRent(rent);
        room.setCapacity(capacity);
        room.setStatus(status);
        return rooms.save(room);
    }

    private void tenant(String first, String last, String phone, String email, LocalDate checkIn, LocalDate contractEnd,
            Room room) {
        Tenant t = new Tenant();
        t.setFirstName(first);
        t.setLastName(last);
        t.setPhone(phone);
        t.setEmail(email);
        t.setCheckInDate(checkIn);
        t.setContractEndDate(contractEnd);
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
