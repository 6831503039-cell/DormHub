package th.mfu.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import th.mfu.AnnouncementRepository;
import th.mfu.AppUserRepository;
import th.mfu.BillRepository;
import th.mfu.BookingRequestRepository;
import th.mfu.FeedbackRepository;
import th.mfu.LaundryMachineRepository;
import th.mfu.ParcelRepository;
import th.mfu.RepairRequestRepository;
import th.mfu.RoomRepository;
import th.mfu.TenantRepository;
import th.mfu.config.DormProperties;
import th.mfu.domain.Announcement;
import th.mfu.domain.AppUser;
import th.mfu.domain.Bill;
import th.mfu.domain.BookingRequest;
import th.mfu.domain.Feedback;
import th.mfu.domain.LaundryMachine;
import th.mfu.domain.MachineType;
import th.mfu.domain.Parcel;
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
    private final AnnouncementRepository announcements;
    private final ParcelRepository parcels;
    private final PasswordEncoder passwordEncoder;
    private final DormProperties props;
    private final Clock clock;

    public DataSeeder(AppUserRepository users, RoomRepository rooms, TenantRepository tenants, BillRepository bills,
            RepairRequestRepository repairs, FeedbackRepository feedbacks, LaundryMachineRepository machines,
            BookingRequestRepository bookings, AnnouncementRepository announcements, ParcelRepository parcels,
            PasswordEncoder passwordEncoder, DormProperties props, Clock clock) {
        this.users = users;
        this.rooms = rooms;
        this.tenants = tenants;
        this.bills = bills;
        this.repairs = repairs;
        this.feedbacks = feedbacks;
        this.machines = machines;
        this.bookings = bookings;
        this.announcements = announcements;
        this.parcels = parcels;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
    }

    /**
     * One transaction for the whole seed: if the app is stopped halfway (seeding a remote Cloud SQL database takes
     * minutes), nothing is committed and the next start seeds again from scratch, instead of leaving half the demo data.
     */
    @Override
    @Transactional
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
        // SJD Mansion: every room has air conditioning; floor 1 costs 4,500 and floors 2-4 cost 4,000 per room
        Room r101 = room("A101", "A", 1, ROOM_TYPE, rentFor(1), 2, RoomStatus.OCCUPIED);
        Room r102 = room("A102", "A", 1, ROOM_TYPE, rentFor(1), 2, RoomStatus.OCCUPIED);
        room("A103", "A", 1, ROOM_TYPE, rentFor(1), 1, RoomStatus.AVAILABLE);
        Room r104 = room("A104", "A", 1, ROOM_TYPE, rentFor(1), 1, RoomStatus.MAINTENANCE);
        Room r201 = room("A201", "A", 2, ROOM_TYPE, rentFor(2), 3, RoomStatus.OCCUPIED);
        room("A202", "A", 2, ROOM_TYPE, rentFor(2), 3, RoomStatus.AVAILABLE);
        room("A203", "A", 2, ROOM_TYPE, rentFor(2), 1, RoomStatus.AVAILABLE);
        Room r204 = room("A204", "A", 2, ROOM_TYPE, rentFor(2), 2, RoomStatus.OCCUPIED);

        // contract ends: A101 and A204 end within 60 days, so they show as "ใกล้หมดสัญญา" on the 3D map
        tenant("สมชาย", "ใจดี", "081-111-1111", "somchai@example.com", today.minusMonths(14), today.plusDays(25), r101);
        tenant("สมศักดิ์", "มั่นคง", "081-222-2222", "somsak@example.com", today.minusMonths(14), today.plusDays(25), r101);
        tenant("สุดา", "รักเรียน", "082-333-3333", "suda@example.com", today.minusMonths(13), today.plusMonths(4), r102);
        tenant("มานี", "มีสุข", "083-444-4444", "manee@example.com", today.minusMonths(13), today.plusMonths(5), r201);
        tenant("ชูใจ", "ใฝ่ดี", "083-555-5555", "chujai@example.com", today.minusMonths(13), today.plusMonths(5), r201);
        tenant("วีระ", "กล้าหาญ", "083-666-6666", "weera@example.com", today.minusMonths(13), today.plusMonths(5), r201);
        tenant("ปิติ", "ยินดี", "084-777-7777", "piti@example.com", today.minusMonths(13), today.plusDays(40), r204);

        // the rest of the 3 buildings for the 3D room map (laid out like SJD Mansion)
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

        announcement("งดจ่ายน้ำชั่วคราว วันเสาร์นี้ 10:00 - 14:00 น.",
                "การประปาจะซ่อมท่อเมนหน้าหอพัก ขอให้ผู้เช่าสำรองน้ำไว้ล่วงหน้า ขออภัยในความไม่สะดวก", true, 1);
        announcement("ทำความสะอาดใหญ่ทางเดินและบันได ทุกวันจันทร์แรกของเดือน",
                "แม่บ้านจะทำความสะอาดช่วง 09:00 - 12:00 น. กรุณาเก็บรองเท้าและของใช้หน้าห้องเข้าห้องให้เรียบร้อย", false, 5);

        parcel(r101, "สมชาย ใจดี", "Kerry Express", null, now.minusHours(3), null);
        parcel(r101, "สมศักดิ์ มั่นคง", "Flash Express", "กล่องใหญ่ ฝากไว้หลังเคาน์เตอร์", now.minusDays(1), null);
        parcel(r102, "สุดา รักเรียน", "Thailand Post", null, now.minusDays(4), now.minusDays(3));
    }

    private void announcement(String title, String body, boolean pinned, int daysAgo) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setBody(body);
        a.setPinned(pinned);
        a.setCreatedAt(LocalDateTime.now(clock).minusDays(daysAgo));
        announcements.save(a);
    }

    private void parcel(Room room, String recipient, String carrier, String note, LocalDateTime arrived, LocalDateTime pickedUp) {
        Parcel p = new Parcel();
        p.setRoom(room);
        p.setRecipientName(recipient);
        p.setCarrier(carrier);
        p.setNote(note);
        p.setArrivedAt(arrived);
        p.setPickedUpAt(pickedUp);
        parcels.save(p);
    }

    private static final String[] FIRST_NAMES = { "กานต์", "ณัฐ", "ธนา", "พิมพ์", "ภูมิ", "มินตรา", "รวิ", "ศุภ", "อร", "ปาล์ม", "เจน", "ต้นข้าว" };
    private static final String[] LAST_NAMES = { "ศรีสุข", "บุญมา", "แก้วใส", "ทองดี", "สายชล", "พรหมมา", "จันทร์เพ็ญ", "วงศ์ดี" };

    static final String ROOM_TYPE = "แอร์";

    /** Room price at SJD Mansion: 4,500 baht on floor 1, 4,000 baht from floor 2 up (per room, split between roommates). */
    static double rentFor(int floor) {
        return floor == 1 ? 4500 : 4000;
    }

    /**
     * Fills buildings A, B and C like SJD Mansion, skipping the rooms created above:
     * every building has 4 floors with 20 rooms on floors 2-4 (01-20); on floor 1, A has 20 rooms
     * while B and C have 10 rooms (the other half is the entrance, parking, office and laundry).
     * Rooms take 1, 2 or 3 people. Rented rooms get tenants with contract end dates.
     */
    private void seedBuildingRooms(LocalDate today) {
        String[] buildings = { "A", "B", "C" };
        Room firstFree = null;
        Room secondFree = null;
        int person = 0;
        for (int b = 0; b < buildings.length; b++) {
            for (int floor = 1; floor <= 4; floor++) {
                int n = (floor == 1 && b > 0) ? 10 : 20;
                for (int i = 1; i <= n; i++) {
                    String number = buildings[b] + floor + String.format("%02d", i);
                    if (rooms.findByRoomNumber(number).isPresent()) {
                        continue;
                    }
                    // C103 is a single room (one double bed); the rest follow the 1-3 person pattern
                    int capacity = number.equals("C103") ? 1 : 1 + (i * 7 + floor * 3 + b) % 3;
                    int k = (b * 31 + floor * 7 + i * 3) % 10;
                    RoomStatus status = k < 3 ? RoomStatus.AVAILABLE
                            : (k == 3 && floor == 4) ? RoomStatus.MAINTENANCE : RoomStatus.OCCUPIED;
                    Room room = room(number, buildings[b], floor, ROOM_TYPE, rentFor(floor), capacity, status);

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
                            String phone = String.format("09%d-%03d-%04d", person % 10, 100 + person % 900, 1000 + person * 7);
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
