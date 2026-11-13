package th.mfu.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import th.mfu.config.DormProperties;

/**
 * Minimal client for the Gemini generateContent REST API.
 * The API key comes from the GEMINI_API_KEY environment variable; without it every call returns empty
 * and callers fall back to non-AI behaviour.
 */
@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final DormProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public GeminiClient(DormProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(props.getGemini().getApiKey());
    }

    /**
     * @param contents conversation turns built with {@link #content}
     * @param json     ask Gemini to answer with JSON only
     */
    public Optional<String> generate(String systemPrompt, List<Map<String, Object>> contents, boolean json) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("system_instruction", Map.of("parts", List.of(textPart(systemPrompt))));
            body.put("contents", contents);
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            // Gemini 3.x "thinks" before answering by default, which is what made replies slow here.
            // thinkingBudget: 0 turns that off so the chatbot answers right away.
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
            generationConfig.put("maxOutputTokens", 1024);
            if (json) {
                generationConfig.put("responseMimeType", "application/json");
            }
            body.put("generationConfig", generationConfig);
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + props.getGemini().getModel() + ":generateContent"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", props.getGemini().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Gemini API returned HTTP {}: {}", response.statusCode(), abbreviate(response.body()));
                return Optional.empty();
            }
            return extractText(mapper.readTree(response.body()));
        } catch (IOException e) {
            log.warn("Gemini API call failed: {}", e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private static Optional<String> extractText(JsonNode root) {
        StringBuilder text = new StringBuilder();
        for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
            if (!part.path("thought").asBoolean(false) && part.has("text")) {
                text.append(part.get("text").asText());
            }
        }
        return text.length() == 0 ? Optional.empty() : Optional.of(text.toString().trim());
    }

    public static Map<String, Object> content(String role, List<Map<String, Object>> parts) {
        return Map.of("role", role, "parts", parts);
    }

    public static Map<String, Object> textPart(String text) {
        return Map.of("text", text);
    }

    public static Map<String, Object> imagePart(byte[] data, String mimeType) {
        return Map.of("inline_data", Map.of("mime_type", mimeType, "data", Base64.getEncoder().encodeToString(data)));
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= 300 ? s : s.substring(0, 300) + "...";
    }
}
