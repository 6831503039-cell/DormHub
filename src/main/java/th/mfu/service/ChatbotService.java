package th.mfu.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import th.mfu.BillRepository;
import th.mfu.config.DormProperties;
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

    @Autowired
    private GeminiClient gemini;
    @Autowired
    private DormProperties props;
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private BillingService billingService;
    @Autowired
    private LaundryService laundryService;

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
        return "คุณคือ \"แอดมินเสมือน\" ของหอพัก " + props.getDormName() + " ตอบผู้เช่าเป็นภาษาไทย สุภาพ กระชับ เป็นกันเอง "
                + "ใช้เฉพาะข้อมูลด้านล่าง ถ้าไม่รู้หรือเป็นเรื่องที่ต้องให้คนตัดสินใจ ให้แนะนำติดต่อสำนักงาน ห้ามแต่งข้อมูลขึ้นเอง\n"
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
        if (containsAny(m, "wifi", "wi-fi", "ไวไฟ", "วายฟาย", "เน็ต", "อินเทอร์เน็ต", "รหัส")) {
            return "Wi-Fi ของหอ ชื่อ \"" + props.getWifi().getSsid() + "\" รหัสผ่าน \"" + props.getWifi().getPassword()
                    + "\" ครับ ถ้าต่อไม่ได้ลองปิดเปิด Wi-Fi ใหม่ หรือแจ้งที่เมนูคำร้องได้เลย";
        }
        if (containsAny(m, "ค่าไฟ", "ค่าน้ำ", "บิล", "ค่าเช่า", "ค้าง", "จ่าย", "ชำระ", "โอน", "เท่าไหร่", "เท่าไร")) {
            return roomSummary(tenant) + " สแกน QR จ่ายและอัปโหลดสลิปได้ที่เมนู \"บิล\" ครับ";
        }
        if (containsAny(m, "ซักผ้า", "อบผ้า", "เครื่องซัก", "เครื่องอบ")) {
            return "ตอนนี้ " + laundrySummary() + " ดูสถานะและกดเริ่มใช้งานได้ที่เมนู \"ซักผ้า\" ครับ";
        }
        if (containsAny(m, "ซ่อม", "เสีย", "รั่ว", "พัง", "ไม่ติด", "ไม่เย็น")) {
            return "แจ้งซ่อมได้ที่เมนู \"คำร้อง\" ระบุอาการให้ละเอียด ทีมช่างจะติดต่อกลับครับ ถ้าเร่งด่วนโทร " + props.getContactPhone();
        }
        if (containsAny(m, "กฎ", "ระเบียบ", "เสียงดัง", "สัตว์", "แขก", "บุหรี่", "ขยะ")) {
            return "กฎของหอพัก: " + props.getRules();
        }
        if (containsAny(m, "ติดต่อ", "เบอร์", "โทร", "สำนักงาน", "นิติ", "เวลา", "เปิด", "ปิด")) {
            return "สำนักงานเปิด " + props.getOfficeHours() + " โทร " + props.getContactPhone() + " ครับ";
        }
        if (containsAny(m, "สวัสดี", "hello", "hi", "หวัดดี")) {
            return "สวัสดีครับ ผมแอดมินเสมือนของ " + props.getDormName()
                    + " ถามได้เลยเรื่อง Wi-Fi, ค่าเช่า/ค่าไฟ, เครื่องซักผ้า, แจ้งซ่อม หรือกฎหอพัก";
        }
        return "ขออภัยครับ ผมยังตอบเรื่องนี้ไม่ได้ ลองถามเรื่อง Wi-Fi, ค่าเช่า/ค่าไฟ, เครื่องซักผ้า, แจ้งซ่อม หรือกฎหอพัก "
                + "หรือติดต่อสำนักงาน " + props.getContactPhone();
    }

    private String roomSummary(Tenant tenant) {
        if (tenant == null || tenant.getRoom() == null) {
            return "บัญชีนี้ยังไม่ได้ผูกกับห้องพัก (ให้แอดมินเพิ่มอีเมลของคุณในข้อมูลผู้เช่า)";
        }
        StringBuilder sb = new StringBuilder("ห้อง " + tenant.getRoom().getRoomNumber()
                + " อยู่ " + billingService.occupants(tenant.getRoom()) + " คน");
        List<String> unpaid = new ArrayList<>();
        for (Bill bill : billRepository.findByRoomOrderByBillMonthAsc(tenant.getRoom())) {
            if (!bill.isPaid()) {
                unpaid.add(String.format("เดือน %s ยอด %,.2f บาท (ส่วนของคุณ %,.2f บาท)",
                        bill.getBillMonth(), billingService.outstanding(bill), billingService.shareOf(bill)));
            }
        }
        sb.append(unpaid.isEmpty() ? " ไม่มีบิลค้างชำระ" : " บิลค้างชำระ: " + String.join(", ", unpaid));
        return sb.toString();
    }

    private String laundrySummary() {
        LocalDateTime now = laundryService.now();
        List<String> parts = new ArrayList<>();
        for (LaundryMachine m : laundryService.machines()) {
            parts.add(m.getName() + (m.isAvailableAt(now) ? " ว่าง" : " ไม่ว่าง (อีก " + m.minutesLeftAt(now) + " นาที)"));
        }
        return parts.isEmpty() ? "ไม่มีข้อมูล" : String.join(", ", parts);
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
