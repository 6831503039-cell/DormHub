package th.mfu.service;

/** An in-app notification shown on the tenant home page. */
public class Notice {

    private final String level;   // Bootstrap color: info, warning, danger
    private final String icon;    // Lucide icon name
    private final String title;
    private final String message;

    public Notice(String level, String icon, String title, String message) {
        this.level = level;
        this.icon = icon;
        this.title = title;
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public String getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
