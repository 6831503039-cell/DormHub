package th.mfu.app;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

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

    public ChatController(CurrentUserService currentUser, ChatbotService chatbot, GeminiClient gemini) {
        this.currentUser = currentUser;
        this.chatbot = chatbot;
        this.gemini = gemini;
    }

    @GetMapping("/app/chat")
    public String chat(Model model) {
        model.addAttribute("aiEnabled", gemini.isConfigured());
        return "app/chat";
    }

    @PostMapping("/app/chat/send")
    @ResponseBody
    public ChatReply send(@RequestBody ChatRequest request, Principal principal) {
        if (!StringUtils.hasText(request.getMessage())) {
            return new ChatReply("พิมพ์คำถามได้เลยครับ", "fallback");
        }
        String message = request.getMessage().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }
        AppUser user = currentUser.user(principal);
        return chatbot.reply(user, currentUser.tenant(user).orElse(null), message, request.getHistory());
    }
}
