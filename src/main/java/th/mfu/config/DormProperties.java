package th.mfu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under "dormhub.*" in application.properties.
 * Thai defaults live here because .properties files are read as ISO-8859-1.
 */
@ConfigurationProperties(prefix = "dormhub")
public class DormProperties {

    private String dormName = "DormHub";
    private String officeHours = "ทุกวัน 08:00 - 20:00 น.";
    private String contactPhone = "081-234-5678";
    private String rules = "งดส่งเสียงดังหลัง 22:00 น., ห้ามเลี้ยงสัตว์, แขกค้างคืนต้องแจ้งสำนักงานก่อน, "
            + "ทิ้งขยะที่จุดทิ้งขยะชั้น 1, ห้ามสูบบุหรี่ในอาคาร";
    private String promptpayId = "0628307781";
    // account holder name shown under the PromptPay QR
    private String promptpayName = "ญาณวุฒิ ทิศหนองแวง";
    // bills for month M are due on this day of month M+1
    private int dueDay = 5;

    private final Admin admin = new Admin();
    private final Wifi wifi = new Wifi();
    private final Gemini gemini = new Gemini();

    public static class Admin {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Wifi {
        private String ssid;
        private String password;

        public String getSsid() {
            return ssid;
        }
        public void setSsid(String ssid) {
            this.ssid = ssid;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Gemini {
        private String apiKey;
        private String model;

        public String getApiKey() {
            return apiKey;
        }
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        public String getModel() {
            return model;
        }
        public void setModel(String model) {
            this.model = model;
        }
    }

    public String getDormName() {
        return dormName;
    }
    public void setDormName(String dormName) {
        this.dormName = dormName;
    }

    public String getOfficeHours() {
        return officeHours;
    }
    public void setOfficeHours(String officeHours) {
        this.officeHours = officeHours;
    }

    public String getContactPhone() {
        return contactPhone;
    }
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getRules() {
        return rules;
    }
    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getPromptpayId() {
        return promptpayId;
    }
    public void setPromptpayId(String promptpayId) {
        this.promptpayId = promptpayId;
    }

    public String getPromptpayName() {
        return promptpayName;
    }
    public void setPromptpayName(String promptpayName) {
        this.promptpayName = promptpayName;
    }

    public int getDueDay() {
        return dueDay;
    }
    public void setDueDay(int dueDay) {
        this.dueDay = dueDay;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Wifi getWifi() {
        return wifi;
    }

    public Gemini getGemini() {
        return gemini;
    }
}
