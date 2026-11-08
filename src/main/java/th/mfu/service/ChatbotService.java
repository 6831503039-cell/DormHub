package th.mfu.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import th.mfu.BillRepository;
import th.mfu.config.DormProperties;
import th.mfu.config.Messages;
import th.mfu.domain.AppUser;
import th.mfu.domain.Bill;
import th.mfu.domain.LaundryMachine;
import th.mfu.domain.Tenant;

/**
 * "Smart Virtual Admin": answers tenants' everyday questions.
 * Uses Gemini with the dorm's facts as context; falls back to keyword answers without an API key.
 */
@Service
public class ChatbotService {

    private static final int MAX_HISTORY = 10;

    private final GeminiClient gemini;
    private final DormProperties props;
    private final BillRepository billRepository;
    private final BillingService billingService;
    private final LaundryService laundryService;
    private final Messages messages;

    public ChatbotService(GeminiClient gemini, DormProperties props, BillRepository billRepository,
            BillingService billingService, LaundryService laundryService, Messages messages) {
        this.gemini = gemini;
        this.props = props;
        this.billRepository = billRepository;
        this.billingService = billingService;
        this.laundryService = laundryService;
        this.messages = messages;
    }

    public ChatReply reply(AppUser user, Tenant tenant, String message, List<ChatTurn> history) {
        if (gemini.isConfigured()) {
            List<Map<String, Object>> contents = new ArrayList<>();
            List<ChatTurn> recent = history == null ? List.of()
                    : history.subList(Math.max(0, history.size() - MAX_HISTORY), history.size());
            for (ChatTurn turn : recent) {
                if (turn.getText() != null) {
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

    private String systemPrompt(AppUser user, Tenant tenant) {
        return "คุณคือ \"แอดมินเสมือน\" ของหอพัก " + props.getDormName() + " ตอบผู้เช่าเป็นภาษา"
                + (messages.isEnglish() ? "อังกฤษ" : "ไทย") + " สุภาพ กระชับ เป็นกันเอง "
                + "ตอบคำถามทั่วไปเรื่องอื่นที่ไม่เกี่ยวกับหอพักได้ตามความรู้ของคุณ ถ้าเป็นข้อมูลล่าสุดหรือเรียลไทม์ให้บอกว่าอาจไม่อัปเดต\n"
                + "ถ้าเป็นเรื่องของหอพักหรือของผู้ใช้ ให้ใช้เฉพาะข้อมูลด้านล่าง ถ้าไม่มีข้อมูลหรือเป็นเรื่องที่ต้องให้คนตัดสินใจ "
                + "ให้แนะนำติดต่อสำนักงาน ห้ามแต่งข้อมูลของหอพักขึ้นเอง\n"
                + "ข้อมูลหอพัก:\n"
                + "- Wi-Fi ชื่อ: " + props.getWifi().getSsid() + " รหัสผ่าน: " + props.getWifi().getPassword() + "\n"
                + "- เวลาทำการสำนักงาน: " + props.getOfficeHours() + " โทร " + props.getContactPhone() + "\n"
                + "- กฎหอพัก: " + props.getRules() + "\n"
                + "- ชำระค่าเช่า: บิลเดือนไหนครบกำหนดวันที่ " + props.getDueDay() + " ของเดือนถัดไป "
                + "โอนเข้าพร้อมเพย์ " + props.getPromptpayId() + " ชื่อบัญชี " + props.getPromptpayName()
                + " (สแกน QR และอัปโหลดสลิปได้ที่เมนู \"บิล\" ในแอป)\n"
                + "- แจ้งซ่อมและส่งความคิดเห็นได้ที่เมนู \"คำร้อง\" ในแอป\n"
                + "- เครื่องซักผ้า/อบผ้า: " + laundrySummary() + "\n"
                + "ข้อมูลผู้ใช้ที่กำลังคุย:\n"
                + "- ชื่อ: " + user.getFullName() + "\n"
                + "- " + roomSummary(tenant);
    }

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

    private String laundrySummary() {
        LocalDateTime now = laundryService.now();
        List<String> parts = new ArrayList<>();
        for (LaundryMachine m : laundryService.machines()) {
            parts.add(m.isAvailableAt(now) ? messages.get("chat.summary.machineFree", m.getName())
                    : messages.get("chat.summary.machineBusy", m.getName(), String.valueOf(m.minutesLeftAt(now))));
        }
        return parts.isEmpty() ? messages.get("chat.summary.noData") : String.join(", ", parts);
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
