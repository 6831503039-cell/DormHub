package th.mfu.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import th.mfu.AnnouncementRepository;
import th.mfu.BillRepository;
import th.mfu.ParcelRepository;
import th.mfu.RepairRequestRepository;
import th.mfu.RoomRepository;
import th.mfu.config.DormProperties;
import th.mfu.config.Messages;
import th.mfu.domain.Announcement;
import th.mfu.domain.AppUser;
import th.mfu.domain.Bill;
import th.mfu.domain.LaundryMachine;
import th.mfu.domain.MachineType;
import th.mfu.domain.Parcel;
import th.mfu.domain.RepairRequest;
import th.mfu.domain.Room;
import th.mfu.domain.RoomStatus;
import th.mfu.domain.Tenant;

/**
 * "Smart Virtual Admin": answers tenants' questions, dorm matters first, general questions too.
 * Uses Gemini with the dorm's live data as context; falls back to keyword answers without an API key
 * (or when Gemini does not answer in time).
 */
@Service
public class ChatbotService {

    private static final int MAX_HISTORY = 10;
    private static final int MAX_ANNOUNCEMENTS = 5;
    private static final int MAX_REPAIRS = 5;
    private static final int MAX_ANNOUNCEMENT_BODY = 300;
    // free rooms listed per floor in the short fallback answer (the AI prompt gets all of them)
    private static final int FALLBACK_ROOMS_PER_FLOOR = 6;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm");

    private final GeminiClient gemini;
    private final DormProperties props;
    private final BillRepository billRepository;
    private final BillingService billingService;
    private final LaundryService laundryService;
    private final RoomRepository roomRepository;
    private final AnnouncementRepository announcementRepository;
    private final ParcelRepository parcelRepository;
    private final RepairRequestRepository repairRepository;
    private final Messages messages;

    public ChatbotService(GeminiClient gemini, DormProperties props, BillRepository billRepository,
            BillingService billingService, LaundryService laundryService, RoomRepository roomRepository,
            AnnouncementRepository announcementRepository, ParcelRepository parcelRepository,
            RepairRequestRepository repairRepository, Messages messages) {
        this.gemini = gemini;
        this.props = props;
        this.billRepository = billRepository;
        this.billingService = billingService;
        this.laundryService = laundryService;
        this.roomRepository = roomRepository;
        this.announcementRepository = announcementRepository;
        this.parcelRepository = parcelRepository;
        this.repairRepository = repairRepository;
        this.messages = messages;
    }

    public ChatReply reply(AppUser user, Tenant tenant, String message, List<ChatTurn> history) {
        if (gemini.isConfigured()) {
            List<Map<String, Object>> contents = new ArrayList<>();
            List<ChatTurn> recent = history == null ? List.of()
                    : history.subList(Math.max(0, history.size() - MAX_HISTORY), history.size());
            for (ChatTurn turn : recent) {
                if (StringUtils.hasText(turn.getText())) {
                    String role = "user".equals(turn.getRole()) ? "user" : "model";
                    contents.add(GeminiClient.content(role, List.of(GeminiClient.textPart(turn.getText()))));
                }
            }
            contents.add(GeminiClient.content("user", List.of(GeminiClient.textPart(message))));
            Optional<String> answer = gemini.generate(systemPrompt(user, tenant), contents, false);
            if (answer.isPresent()) {
                return new ChatReply(answer.get(), "gemini");
            }
        }
        return new ChatReply(fallback(message, tenant), "fallback");
    }

    // ---------------------------------------------------------------- AI prompt

    String systemPrompt(AppUser user, Tenant tenant) {
        LocalDateTime now = laundryService.now();
        StringBuilder p = new StringBuilder();
        p.append("คุณคือ \"แอดมินเสมือน\" ของหอพัก ").append(props.getResidenceName())
                .append(" คุยกับผู้เช่าผ่านแชทในแอป ").append(props.getDormName()).append("\n\n");

        p.append("วิธีตอบ\n");
        p.append("- ตอบเป็นภาษา").append(messages.isEnglish() ? "อังกฤษ" : "ไทย")
                .append(" แต่ถ้าผู้ใช้พิมพ์มาเป็นภาษาอื่นให้ตอบตามภาษานั้น สุภาพ เป็นกันเอง ใช้คำลงท้าย \"ครับ\"\n");
        p.append("- งานหลักคือช่วยเรื่องหอพัก แต่คำถามทั่วไปตอบได้เต็มที่ตามความรู้ของคุณ เช่น การเรียน การบ้าน ภาษา ")
                .append("เขียนโค้ด อาหาร การเดินทาง การใช้ชีวิต สุขภาพเบื้องต้น ถ้าเป็นข้อมูลเรียลไทม์ (ข่าว ราคา สภาพอากาศ) ")
                .append("ให้บอกว่าข้อมูลอาจไม่อัปเดต\n");
        p.append("- เรื่องหอพักและเรื่องของผู้ใช้ ให้ใช้เฉพาะ \"ข้อมูลหอพัก\" และ \"ข้อมูลของผู้ใช้\" ด้านล่าง ")
                .append("ห้ามเดาหรือแต่งตัวเลข ราคา เลขห้อง วันที่ หรือนโยบายขึ้นเอง ถ้าไม่มีข้อมูลให้บอกตรงๆ แล้วแนะนำติดต่อสำนักงาน\n");
        p.append("- เรื่องที่เจ้าหน้าที่ต้องตัดสินใจ เช่น ย้ายห้อง ขอผ่อนผันค่าเช่า ต่อหรือยกเลิกสัญญา เงินประกัน ร้องเรียนเพื่อนบ้าน ")
                .append("ให้อธิบายเท่าที่รู้ แล้วแนะนำติดต่อสำนักงาน\n");
        p.append("- ถ้าเรื่องนั้นทำในแอปได้ ให้บอกชื่อเมนูด้วย\n");
        p.append("- คำทักทายหรือคำถามสั้นๆ ตอบ 1-2 ประโยค เรื่องที่ต้องอธิบายให้ตอบครบแต่กระชับ ")
                .append("ถ้ามีหลายขั้นตอนหรือหลายรายการให้ขึ้นบรรทัดใหม่ทีละข้อ\n");
        p.append("- หน้าแชทแสดงข้อความธรรมดา ห้ามใช้ markdown เช่น ** หรือ #\n");
        p.append("- ห้ามเปิดเผยข้อมูลส่วนตัวของผู้เช่าคนอื่น เช่น ชื่อ เบอร์โทร หรือใครอยู่ห้องไหน\n");
        p.append("- เหตุฉุกเฉิน (ไฟไหม้ มีคนเจ็บ มีคนบุกรุก) ให้โทร 199 ดับเพลิง, 1669 เจ็บป่วยฉุกเฉิน, 191 ตำรวจ ก่อน ")
                .append("แล้วแจ้งสำนักงาน\n\n");

        p.append("ข้อมูลหอพัก (ณ ").append(now.format(DATE_TIME)).append(")\n");
        p.append("- สำนักงาน: เปิด ").append(props.getOfficeHours()).append(" โทร ").append(props.getContactPhone()).append("\n");
        p.append("- Wi-Fi: ชื่อ ").append(props.getWifi().getSsid())
                .append(" รหัสผ่าน ").append(props.getWifi().getPassword()).append("\n");
        p.append("- กฎหอพัก: ").append(props.getRules()).append("\n");

        List<Room> rooms = roomRepository.findAllByOrderByRoomNumberAsc();
        p.append("- ค่าเช่าต่อห้องต่อเดือน (หารเท่ากันตามจำนวนคนในห้อง): ").append(rentByFloor(rooms)).append("\n");
        Optional<Bill> latest = latestBill(tenant);
        Bill rates = latest.isPresent() ? latest.get() : new Bill();
        p.append("- ค่าไฟหน่วยละ ").append(rate(rates.getElectricRate())).append(" บาท ค่าน้ำหน่วยละ ")
                .append(rate(rates.getWaterRate())).append(" บาท จดมิเตอร์และออกบิลเดือนละครั้ง ")
                .append("ค่าเช่า ค่าน้ำ ค่าไฟ หารเท่ากันทุกคนในห้อง ดูกราฟการใช้ไฟได้ที่เมนู \"การใช้ไฟฟ้า\"\n");
        p.append("- ชำระเงิน: บิลของเดือนไหนครบกำหนดวันที่ ").append(props.getDueDay())
                .append(" ของเดือนถัดไป โอนพร้อมเพย์ ").append(props.getPromptpayId())
                .append(" ชื่อบัญชี ").append(props.getPromptpayName())
                .append(" แล้วอัปโหลดสลิปที่เมนู \"บิล\" AI ตรวจสลิปและตัดยอดให้อัตโนมัติถ้ายอดตรง ")
                .append("ถ้าไม่แน่ใจเจ้าหน้าที่จะตรวจเอง\n");
        p.append("- ห้องพัก: ").append(freeRoomsDetail(rooms)).append("\n");
        p.append("- จองห้อง: คนทั่วไปดูห้องว่างบนแผนผัง 3D และส่งคำขอจองได้ที่หน้า \"ห้องว่าง\" โดยไม่ต้องสมัครสมาชิก ")
                .append("ผู้ที่มีบัญชีแต่ยังไม่มีห้องใช้เมนู \"เช่าห้องพัก\" ยังไม่ต้องโอนเงิน เจ้าหน้าที่จะโทรกลับเพื่อยืนยัน ")
                .append("ห้องที่สัญญาจะหมดภายใน 60 วันเปิดให้จองล่วงหน้าได้\n");
        p.append("- ย้ายห้อง: ผู้เช่าเดิมที่อยากย้ายห้องให้ติดต่อสำนักงาน แจ้งเลขห้องว่างที่สนใจได้เลย\n");
        p.append("- ซักผ้า: เครื่องซักรอบละ ").append(MachineType.WASHER.getMinutes())
                .append(" นาที เครื่องอบรอบละ ").append(MachineType.DRYER.getMinutes())
                .append(" นาที กดเริ่มใช้งานที่เมนู \"ซักผ้า\" สถานะตอนนี้: ").append(laundrySummary()).append("\n");
        p.append("- แจ้งซ่อมและส่งความคิดเห็นถึงเจ้าของหอ: เมนู \"คำร้อง\" ผู้เช่าเห็นสถานะงานซ่อมในเมนูเดียวกัน\n");
        p.append("- พัสดุ: เจ้าหน้าที่บันทึกพัสดุที่มาส่ง ดูและยืนยันรับได้ที่เมนู \"พัสดุ\"\n");
        p.append("- เมนูในแอป: หน้าแรก, บิล, ถาม AI, ซักผ้า, คำร้อง, ประกาศ, พัสดุ, การใช้ไฟฟ้า, โปรไฟล์ ")
                .append("(ประกาศ พัสดุ และการใช้ไฟฟ้า อยู่ในทางลัดของหน้าแรก)\n");
        p.append("- ประกาศล่าสุด:").append(announcementsDetail()).append("\n\n");

        p.append("ข้อมูลของผู้ใช้\n");
        p.append("- ชื่อ: ").append(user.getFullName()).append("\n");
        p.append(userDetail(tenant, now.toLocalDate()));
        return p.toString();
    }

    private String rentByFloor(List<Room> rooms) {
        Map<Integer, TreeSet<Double>> rents = new TreeMap<>();
        for (Room room : rooms) {
            if (room.getFloor() != null && room.getMonthlyRent() != null) {
                if (!rents.containsKey(room.getFloor())) {
                    rents.put(room.getFloor(), new TreeSet<>());
                }
                rents.get(room.getFloor()).add(room.getMonthlyRent());
            }
        }
        if (rents.isEmpty()) {
            return "ไม่มีข้อมูล";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Integer, TreeSet<Double>> e : rents.entrySet()) {
            List<String> amounts = new ArrayList<>();
            for (Double rent : e.getValue()) {
                amounts.add(money(rent));
            }
            parts.add("ชั้น " + e.getKey() + " " + String.join(" / ", amounts) + " บาท");
        }
        return String.join(", ", parts);
    }

    /** Every free room, grouped by building and floor, e.g. "ตึก A ชั้น 2: A202 (รับ 3 คน, 4,000 บาท), ...". */
    private String freeRoomsDetail(List<Room> rooms) {
        Map<String, Map<Integer, List<Room>>> free = freeRooms(rooms);
        StringBuilder s = new StringBuilder("ทั้งหมด ").append(rooms.size()).append(" ห้อง ว่าง ")
                .append(countFree(free)).append(" ห้อง");
        for (Map.Entry<String, Map<Integer, List<Room>>> building : free.entrySet()) {
            for (Map.Entry<Integer, List<Room>> floor : building.getValue().entrySet()) {
                List<String> items = new ArrayList<>();
                for (Room r : floor.getValue()) {
                    items.add(r.getRoomNumber() + " (รับ " + value(r.getCapacity()) + " คน, "
                            + money(r.getMonthlyRent()) + " บาท)");
                }
                s.append("\n  ตึก ").append(building.getKey()).append(" ชั้น ").append(floor.getKey()).append(": ")
                        .append(String.join(", ", items));
            }
        }
        return s.toString();
    }

    private String announcementsDetail() {
        List<Announcement> list = announcementRepository.findAllByOrderByPinnedDescCreatedAtDesc();
        if (list.isEmpty()) {
            return " ไม่มี";
        }
        StringBuilder s = new StringBuilder();
        for (Announcement a : list.subList(0, Math.min(MAX_ANNOUNCEMENTS, list.size()))) {
            s.append("\n  ").append(a.isPinned() ? "[ปักหมุด] " : "").append(a.getTitle());
            if (a.getCreatedAt() != null) {
                s.append(" (").append(a.getCreatedAt().format(DATE)).append(")");
            }
            if (StringUtils.hasText(a.getBody())) {
                s.append(": ").append(abbreviate(a.getBody().replaceAll("\\s+", " "), MAX_ANNOUNCEMENT_BODY));
            }
        }
        return s.toString();
    }

    private String userDetail(Tenant tenant, LocalDate today) {
        if (tenant == null || tenant.getRoom() == null) {
            return "- ห้อง: " + messages.get("chat.summary.notLinked")
                    + " ถ้าต้องการเช่าห้อง ใช้เมนู \"เช่าห้องพัก\" เพื่อเลือกห้องว่างบนแผนผังและส่งคำขอ\n";
        }
        Room room = tenant.getRoom();
        long occupants = billingService.occupants(room);
        StringBuilder s = new StringBuilder();
        s.append("- ห้อง ").append(room.getRoomNumber()).append(" ตึก ").append(room.getBuilding())
                .append(" ชั้น ").append(room.getFloor());
        if (StringUtils.hasText(room.getRoomType())) {
            s.append(" ประเภท ").append(room.getRoomType());
        }
        s.append(" ค่าเช่า ").append(money(room.getMonthlyRent())).append(" บาท/เดือน อยู่ ").append(occupants)
                .append(" จาก ").append(value(room.getCapacity())).append(" คน\n");
        if (tenant.getCheckInDate() != null || tenant.getContractEndDate() != null) {
            s.append("- สัญญา:");
            if (tenant.getCheckInDate() != null) {
                s.append(" เข้าพัก ").append(tenant.getCheckInDate().format(DATE));
            }
            if (tenant.getContractEndDate() != null) {
                long days = ChronoUnit.DAYS.between(today, tenant.getContractEndDate());
                s.append(" หมดสัญญา ").append(tenant.getContractEndDate().format(DATE))
                        .append(days >= 0 ? " (อีก " + days + " วัน)" : " (หมดแล้ว)");
            }
            s.append("\n");
        }
        s.append("- ").append(roomSummary(tenant)).append("\n");
        Optional<Bill> latest = latestBill(tenant);
        if (latest.isPresent()) {
            Bill bill = latest.get();
            s.append("- บิลล่าสุด เดือน ").append(bill.getBillMonth())
                .append(": ค่าเช่า ").append(money(bill.getRent()))
                .append(" ค่าไฟ ").append(value(bill.getElectricUnits())).append(" หน่วย ").append(money(bill.getElectricCost()))
                .append(" ค่าน้ำ ").append(value(bill.getWaterUnits())).append(" หน่วย ").append(money(bill.getWaterCost()))
                .append(" รวม ").append(money(bill.getTotal())).append(" บาท (ส่วนของผู้ใช้ ")
                .append(money(billingService.shareOf(bill))).append(" บาท)")
                .append(bill.isPaid() ? " ชำระแล้ว" : " ยังไม่ชำระ")
                .append(bill.getDueDate() != null ? " ครบกำหนด " + bill.getDueDate().format(DATE) : "").append("\n");
        }
        s.append("- พัสดุรอรับ: ").append(parcelSummary(room)).append("\n");
        s.append("- งานซ่อมของห้อง: ").append(repairsDetail(room)).append("\n");
        return s.toString();
    }

    private String repairsDetail(Room room) {
        List<RepairRequest> repairs = repairRepository.findByRoomOrderByReportedDateDescIdDesc(room);
        if (repairs.isEmpty()) {
            return "ไม่มี";
        }
        List<String> items = new ArrayList<>();
        for (RepairRequest r : repairs.subList(0, Math.min(MAX_REPAIRS, repairs.size()))) {
            String item = r.getTitle() + " (แจ้ง " + (r.getReportedDate() == null ? "-" : r.getReportedDate().format(DATE))
                    + (r.getStatus() == null ? "" : ", " + messages.get(r.getStatus().getKey())) + ")";
            if (StringUtils.hasText(r.getAdminNote())) {
                item += " หมายเหตุจากเจ้าหน้าที่: " + r.getAdminNote();
            }
            items.add(item);
        }
        return String.join(", ", items);
    }

    // ---------------------------------------------------------------- keyword fallback

    String fallback(String message, Tenant tenant) {
        String m = message == null ? "" : message.toLowerCase();
        if (containsAny(m, "wifi", "wi-fi", "ไวไฟ", "วายฟาย", "เน็ต", "อินเทอร์เน็ต", "รหัส", "internet", "password")) {
            return messages.get("chat.reply.wifi", props.getWifi().getSsid(), props.getWifi().getPassword());
        }
        if (containsAny(m, "ค่าไฟ", "ค่าน้ำ", "บิล", "ค่าเช่า", "ค้าง", "จ่าย", "ชำระ", "โอน", "เท่าไหร่", "เท่าไร",
                "bill", "rent", "electric", "water", "pay", "owe", "how much")) {
            return messages.get("chat.reply.bill", roomSummary(tenant));
        }
        if (containsAny(m, "ซักผ้า", "อบผ้า", "เครื่องซัก", "เครื่องอบ", "laundry", "wash", "dryer")) {
            return messages.get("chat.reply.laundry", laundrySummary());
        }
        if (containsAny(m, "ห้องว่าง", "ย้ายห้อง", "จองห้อง", "เช่าห้อง", "vacant", "available", "free room",
                "move room", "change room", "book a room") || (m.contains("ห้อง") && m.contains("ว่าง"))) {
            return messages.get("chat.reply.rooms", freeRoomsShort(roomRepository.findAllByOrderByRoomNumberAsc()),
                    props.getContactPhone());
        }
        if (containsAny(m, "พัสดุ", "ของมาส่ง", "ไปรษณีย์", "กล่อง", "parcel", "package", "delivery")) {
            return messages.get("chat.reply.parcels", tenant == null || tenant.getRoom() == null
                    ? messages.get("chat.summary.notLinked") : parcelSummary(tenant.getRoom()));
        }
        if (containsAny(m, "ประกาศ", "ข่าว", "announce", "news", "notice")) {
            return messages.get("chat.reply.announcements", announcementsShort());
        }
        if (containsAny(m, "ซ่อม", "เสีย", "รั่ว", "พัง", "ไม่ติด", "ไม่เย็น", "repair", "broken", "fix", "leak", "not working")) {
            return messages.get("chat.reply.repair", props.getContactPhone());
        }
        if (containsAny(m, "กฎ", "ระเบียบ", "เสียงดัง", "สัตว์", "แขก", "บุหรี่", "ขยะ", "rule", "noise", "pet", "guest", "smok", "trash")) {
            return messages.get("chat.reply.rules", props.getRules());
        }
        if (containsAny(m, "ติดต่อ", "เบอร์", "โทร", "สำนักงาน", "นิติ", "เวลา", "เปิด", "ปิด", "contact", "phone", "office", "call", "hours", "open")) {
            return messages.get("chat.reply.contact", props.getOfficeHours(), props.getContactPhone());
        }
        if (containsAny(m, "สวัสดี", "hello", "hi", "หวัดดี")) {
            return messages.get("chat.reply.hello", props.getDormName());
        }
        return messages.get("chat.reply.unknown", props.getContactPhone());
    }

    /** Free rooms per building and floor, a few numbers each, e.g. "ตึก A ชั้น 2: A202, A203". */
    private String freeRoomsShort(List<Room> rooms) {
        Map<String, Map<Integer, List<Room>>> free = freeRooms(rooms);
        int count = countFree(free);
        if (count == 0) {
            return messages.get("chat.summary.noFreeRooms");
        }
        List<String> floors = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, List<Room>>> building : free.entrySet()) {
            for (Map.Entry<Integer, List<Room>> floor : building.getValue().entrySet()) {
                List<Room> list = floor.getValue();
                List<String> numbers = new ArrayList<>();
                for (Room r : list.subList(0, Math.min(FALLBACK_ROOMS_PER_FLOOR, list.size()))) {
                    numbers.add(r.getRoomNumber());
                }
                String more = list.size() > numbers.size()
                        ? " " + messages.get("chat.summary.andMore", list.size() - numbers.size()) : "";
                floors.add(messages.get("chat.summary.freeFloor", building.getKey(), floor.getKey(),
                        String.join(", ", numbers) + more));
            }
        }
        return messages.get("chat.summary.freeRooms", count, rooms.size(), String.join("\n", floors));
    }

    private String announcementsShort() {
        List<Announcement> list = announcementRepository.findTop3ByOrderByPinnedDescCreatedAtDesc();
        if (list.isEmpty()) {
            return messages.get("chat.summary.noAnnouncements");
        }
        List<String> titles = new ArrayList<>();
        for (Announcement a : list) {
            titles.add("- " + a.getTitle());
        }
        return String.join("\n", titles);
    }

    // ---------------------------------------------------------------- shared summaries

    private String roomSummary(Tenant tenant) {
        if (tenant == null || tenant.getRoom() == null) {
            return messages.get("chat.summary.notLinked");
        }
        List<String> unpaid = new ArrayList<>();
        for (Bill bill : billRepository.findByRoomOrderByBillMonthAsc(tenant.getRoom())) {
            if (!bill.isPaid()) {
                unpaid.add(messages.get("chat.summary.unpaidBill", bill.getBillMonth(),
                        String.format("%,.2f", billingService.outstanding(bill)), String.format("%,.2f", billingService.shareOf(bill))));
            }
        }
        return messages.get("chat.summary.room", tenant.getRoom().getRoomNumber(),
                String.valueOf(billingService.occupants(tenant.getRoom())))
                + " " + (unpaid.isEmpty() ? messages.get("chat.summary.noUnpaid")
                        : messages.get("chat.summary.unpaid", String.join(", ", unpaid)));
    }

    private String parcelSummary(Room room) {
        List<String> waiting = new ArrayList<>();
        for (Parcel parcel : parcelRepository.findByRoomOrderByArrivedAtDesc(room)) {
            if (parcel.isWaiting()) {
                String arrived = parcel.getArrivedAt() == null ? "-" : parcel.getArrivedAt().format(DATE_TIME);
                waiting.add(messages.get("chat.summary.parcel", parcel.getRecipientName(),
                        StringUtils.hasText(parcel.getCarrier()) ? parcel.getCarrier() : "-", arrived));
            }
        }
        return waiting.isEmpty() ? messages.get("chat.summary.noParcels")
                : messages.get("chat.summary.parcels", waiting.size(), String.join(", ", waiting));
    }

    private String laundrySummary() {
        LocalDateTime now = laundryService.now();
        List<String> parts = new ArrayList<>();
        for (LaundryMachine m : laundryService.machines()) {
            parts.add(m.isAvailableAt(now) ? messages.get("chat.summary.machineFree", m.getName())
                    : messages.get("chat.summary.machineBusy", m.getName(), String.valueOf(m.minutesLeftAt(now))));
        }
        return parts.isEmpty() ? messages.get("chat.summary.noData") : String.join(", ", parts);
    }

    private Optional<Bill> latestBill(Tenant tenant) {
        if (tenant == null || tenant.getRoom() == null) {
            return Optional.empty();
        }
        List<Bill> bills = billRepository.findByRoomOrderByBillMonthDesc(tenant.getRoom());
        if (bills.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(bills.get(0));
    }

    /** Available rooms by building (A, B, C...) then floor, both sorted. */
    private static Map<String, Map<Integer, List<Room>>> freeRooms(List<Room> rooms) {
        Map<String, Map<Integer, List<Room>>> free = new TreeMap<>();
        for (Room room : rooms) {
            if (room.getStatus() == RoomStatus.AVAILABLE) {
                String building = StringUtils.hasText(room.getBuilding()) ? room.getBuilding() : "-";
                int floor = room.getFloor() == null ? 0 : room.getFloor();
                if (!free.containsKey(building)) {
                    free.put(building, new TreeMap<>());
                }
                Map<Integer, List<Room>> floors = free.get(building);
                if (!floors.containsKey(floor)) {
                    floors.put(floor, new ArrayList<>());
                }
                floors.get(floor).add(room);
            }
        }
        return free;
    }

    private static int countFree(Map<String, Map<Integer, List<Room>>> free) {
        int count = 0;
        for (Map<Integer, List<Room>> floors : free.values()) {
            for (List<Room> list : floors.values()) {
                count += list.size();
            }
        }
        return count;
    }

    private static String money(Double amount) {
        return amount == null ? "-" : String.format("%,.0f", amount);
    }

    /** Unit price without trailing zeros: 7.0 -> "7", 7.5 -> "7.5". */
    private static String rate(Double amount) {
        return amount == null ? "-" : BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
    }

    private static int value(Integer n) {
        return n == null ? 0 : n;
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static boolean containsAny(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }
}
