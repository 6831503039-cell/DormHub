package th.mfu.app;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import th.mfu.config.Messages;
import th.mfu.domain.AppUser;
import th.mfu.service.ChatReply;
import th.mfu.service.ChatbotService;
import th.mfu.service.CurrentUserService;
import th.mfu.service.GeminiClient;

@Controller
public class ChatController {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final CurrentUserService currentUser;
    private final ChatbotService chatbot;
    private final GeminiClient gemini;
    private final Messages messages;

    public ChatController(CurrentUserService currentUser, ChatbotService chatbot, GeminiClient gemini, Messages messages) {
        this.currentUser = currentUser;
        this.chatbot = chatbot;
        this.gemini = gemini;
        this.messages = messages;
    }

    @GetMapping("/app/chat")
    public String chat(Model model) {
        model.addAttribute("aiEnabled", gemini.isConfigured());
        return "app/chat";
    }

    // web service called by $.ajax in app/chat.html: JSON in (@RequestBody), JSON out
    @PostMapping("/app/chat/send")
    public ResponseEntity<ChatReply> send(@RequestBody ChatRequest request, Principal principal) {
        if (!StringUtils.hasText(request.getMessage())) {
            return new ResponseEntity<ChatReply>(new ChatReply(messages.get("chat.reply.empty"), "fallback"), HttpStatus.OK);
        }
        String message = request.getMessage().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }
        AppUser user = currentUser.user(principal);
        ChatReply reply = chatbot.reply(user, currentUser.tenant(user).orElse(null), message, request.getHistory());
        return new ResponseEntity<ChatReply>(reply, HttpStatus.OK);
    }
}
