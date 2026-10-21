package th.mfu.service;

/**
 * Builds a Thai PromptPay QR payload (EMVCo format) for a phone number or national/tax ID.
 * Any banking app can scan the resulting QR to transfer the given amount.
 */
public final class PromptPay {

    private PromptPay() {
    }

    public static String payload(String promptpayId, double amount) {
        String id = promptpayId.replaceAll("[^0-9]", "");
        String account;
        if (id.length() >= 13) {
            account = field("02", id); // national ID / tax ID
        } else {
            // phone 0812345678 -> 0066812345678
            String phone = "0066" + (id.startsWith("0") ? id.substring(1) : id);
            account = field("01", phone);
        }
        String merchant = field("00", "A000000677010111") + account;

        StringBuilder sb = new StringBuilder()
                .append(field("00", "01"))
                .append(field("01", "12")) // 12 = one-time QR with amount
                .append(field("29", merchant))
                .append(field("53", "764")) // THB
                .append(field("54", String.format("%.2f", amount)))
                .append(field("58", "TH"))
                .append("6304");
        return sb + crc16(sb.toString());
    }

    private static String field(String tag, String value) {
        return tag + String.format("%02d", value.length()) + value;
    }

    /** CRC-16/CCITT-FALSE, as required by the EMVCo QR spec. */
    static String crc16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes(java.nio.charset.StandardCharsets.US_ASCII)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }
}
