package th.mfu.service;

/** An in-app notification shown on the tenant home page; title and message are message keys (messages*.properties). */
public class Notice {

    private final String level;   // Bootstrap color: info, warning, danger
    private final String icon;    // Lucide icon name
    private final String titleKey;
    private final String messageKey;
    private final Object[] args;  // values for {0}, {1}, ... in the message

    public Notice(String level, String icon, String titleKey, String messageKey, Object... args) {
        this.level = level;
        this.icon = icon;
        this.titleKey = titleKey;
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getLevel() {
        return level;
    }

    public String getIcon() {
        return icon;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
