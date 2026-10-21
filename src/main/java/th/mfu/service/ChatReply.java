package th.mfu.service;

public class ChatReply {

    private final String reply;
    // "gemini" or "fallback" (built-in answers when the API key is not set or the call failed)
    private final String source;

    public ChatReply(String reply, String source) {
        this.reply = reply;
        this.source = source;
    }

    public String getReply() {
        return reply;
    }

    public String getSource() {
        return source;
    }
}
