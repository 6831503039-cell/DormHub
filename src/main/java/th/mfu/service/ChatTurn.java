package th.mfu.service;

/** One earlier message in the chat, sent back by the browser as context. */
public class ChatTurn {

    private String role; // "user" or "bot"
    private String text;

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}
