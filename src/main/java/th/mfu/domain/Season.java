package th.mfu.domain;

/** Thailand's three seasons, rounded to whole months. */
public enum Season {
    HOT("ฤดูร้อน", "มี.ค. - พ.ค."),
    RAINY("ฤดูฝน", "มิ.ย. - ต.ค."),
    COOL("ฤดูหนาว", "พ.ย. - ก.พ.");

    private final String label;
    private final String months;

    Season(String label, String months) {
        this.label = label;
        this.months = months;
    }

    public static Season of(int month) {
        if (month >= 3 && month <= 5) {
            return HOT;
        }
        if (month >= 6 && month <= 10) {
            return RAINY;
        }
        return COOL;
    }

    public String getLabel() {
        return label;
    }

    public String getMonths() {
        return months;
    }
}
