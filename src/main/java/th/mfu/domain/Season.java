package th.mfu.domain;

/** Thailand's three seasons, rounded to whole months. */
public enum Season {
    HOT, RAINY, COOL;

    public static Season of(int month) {
        if (month >= 3 && month <= 5) {
            return HOT;
        }
        if (month >= 6 && month <= 10) {
            return RAINY;
        }
        return COOL;
    }

    /** Message key of the season name; the months it covers are under getKey() + ".months". */
    public String getKey() {
        return "season." + name();
    }
}
