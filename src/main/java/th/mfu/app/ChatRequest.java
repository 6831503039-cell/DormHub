package th.mfu.app;

import java.util.List;

import th.mfu.service.ChatTurn;

public class ChatRequest {

    private String message;
    private List<ChatTurn> history;

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatTurn> getHistory() {
        return history;
    }
    public void setHistory(List<ChatTurn> history) {
        this.history = history;
    }
}
