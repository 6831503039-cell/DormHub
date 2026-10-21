package th.mfu.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import th.mfu.config.DormProperties;
import th.mfu.domain.PaymentStatus;

/**
 * Reads a transfer slip with Gemini and decides whether it can be approved automatically.
 * Only clean slips whose amount matches are auto-approved; anything doubtful goes to the admin.
 */
@Service
public class SlipVerificationService {

    private static final String PROMPT = "คุณคือระบบตรวจสอบสลิปโอนเงินของธนาคารไทยสำหรับหอพัก "
            + "ดูรูปที่แนบแล้วตอบเป็น JSON เท่านั้น ตามรูปแบบนี้: "
            + "{\"is_transfer_slip\": boolean, \"amount\": number หรือ null, \"transfer_datetime\": string หรือ null, "
            + "\"receiver\": string หรือ null, \"looks_tampered\": boolean, \"reason\": string (ภาษาไทย สั้นๆ)} "
            + "looks_tampered ให้เป็น true ถ้าเห็นร่องรอยการตัดต่อ ตัวอักษรไม่เสมอกัน ฟอนต์ผิดปกติ ตัวเลขซ้อนทับ "
            + "หรือรูปแบบไม่เหมือนสลิปจริงของธนาคาร";

    private final GeminiClient gemini;
    private final ObjectMapper mapper;
    private final DormProperties props;

    public SlipVerificationService(GeminiClient gemini, ObjectMapper mapper, DormProperties props) {
        this.gemini = gemini;
        this.mapper = mapper;
        this.props = props;
    }

    public SlipCheck verify(byte[] image, String contentType, double expectedAmount) {
        if (contentType == null || !contentType.startsWith("image/")) {
            return new SlipCheck(PaymentStatus.SUSPICIOUS, "ไฟล์ที่ส่งมาไม่ใช่รูปภาพ");
        }
        if (!gemini.isConfigured()) {
            return new SlipCheck(PaymentStatus.PENDING, "รอแอดมินตรวจสอบ (ยังไม่ได้เปิดใช้ AI ตรวจสลิป)");
        }
        String question = String.format("ยอดที่ผู้เช่าแจ้งว่าโอน: %.2f บาท ผู้รับควรเป็นพร้อมเพย์ %s (%s)",
                expectedAmount, props.getPromptpayId(), props.getDormName());
        Optional<String> answer = gemini.generate(PROMPT, List.of(GeminiClient.content("user",
                List.of(GeminiClient.textPart(question), GeminiClient.imagePart(image, contentType)))), true);
        if (answer.isEmpty()) {
            return new SlipCheck(PaymentStatus.PENDING, "AI ตรวจสลิปไม่สำเร็จ รอแอดมินตรวจสอบ");
        }
        return decide(answer.get(), expectedAmount);
    }

    SlipCheck decide(String json, double expectedAmount) {
        JsonNode r;
        try {
            r = mapper.readTree(json);
        } catch (Exception e) {
            return new SlipCheck(PaymentStatus.PENDING, "อ่านผลจาก AI ไม่ได้ รอแอดมินตรวจสอบ");
        }
        String reason = r.path("reason").asText("");
        if (!r.path("is_transfer_slip").asBoolean(false)) {
            return new SlipCheck(PaymentStatus.SUSPICIOUS, "AI: ไม่ใช่สลิปโอนเงิน " + reason);
        }
        if (r.path("looks_tampered").asBoolean(false)) {
            return new SlipCheck(PaymentStatus.SUSPICIOUS, "AI: สลิปอาจถูกตัดต่อ " + reason);
        }
        JsonNode amount = r.path("amount");
        if (!amount.isNumber()) {
            return new SlipCheck(PaymentStatus.PENDING, "AI อ่านยอดเงินไม่ได้ รอแอดมินตรวจสอบ " + reason);
        }
        if (Math.abs(amount.asDouble() - expectedAmount) > 1) {
            return new SlipCheck(PaymentStatus.PENDING, String.format(
                    "AI: ยอดในสลิป %.2f บาท ไม่ตรงกับที่แจ้ง %.2f บาท รอแอดมินตรวจสอบ", amount.asDouble(), expectedAmount));
        }
        return new SlipCheck(PaymentStatus.APPROVED, String.format("AI ตรวจผ่าน: ยอด %.2f บาท ตัดยอดอัตโนมัติ %s",
                amount.asDouble(), reason));
    }
}
