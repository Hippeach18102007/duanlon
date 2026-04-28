package com.retailmanagement.constants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.retailmanagement.dto.request.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

/**
 * Groq API Client — FREE tier: 14,400 req/ngày, 30 req/phút
 * Model: llama-3.3-70b-versatile (thông minh) hoặc llama-3.1-8b-instant (siêu nhanh)
 *
 * Lấy API key miễn phí tại: https://console.groq.com
 * Thêm vào application.properties:
 *   groq.api-key=gsk_...
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroqClient {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // llama-3.3-70b-versatile: thông minh, phù hợp tư vấn bán hàng
    // llama-3.1-8b-instant: nhanh hơn, dùng nếu cần response time thấp
    private static final String MODEL = "llama-3.3-70b-versatile";

    @Value("${groq.api-key}")
    private String apiKey;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Gửi conversation tới Groq và nhận reply.
     * Groq dùng OpenAI-compatible API nên format giống ChatGPT.
     *
     * @param systemInstruction  System prompt (context cửa hàng, thông tin khách...)
     * @param history            Lịch sử [{role:"user"/"model", content:"..."}]
     */
    public String chat(String systemInstruction, List<ChatMessage> history) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", MODEL);
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);

            // Groq dùng OpenAI format: messages array với role system/user/assistant
            ArrayNode messages = objectMapper.createArrayNode();

            // System message đầu tiên
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemInstruction);
            messages.add(systemMsg);

            // Conversation history
            // Groq dùng "assistant" thay vì "model" (khác Gemini)
            for (ChatMessage msg : history) {
                ObjectNode m = objectMapper.createObjectNode();
                String role = msg.getRole().equals("model") ? "assistant" : msg.getRole();
                m.put("role", role);
                m.put("content", msg.getContent());
                messages.add(m);
            }

            body.set("messages", messages);

            String responseJson = webClientBuilder.build()
                    .post()
                    .uri(GROQ_URL)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);

            if (root.has("error")) {
                String errMsg = root.path("error").path("message").asText();
                log.error("Groq API error: {}", errMsg);
                return "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau.";
            }

            return root
                    .path("choices").get(0)
                    .path("message")
                    .path("content").asText();

        } catch (Exception e) {
            log.error("GroqClient error: {}", e.getMessage());
            return "Xin lỗi, tôi không thể xử lý yêu cầu lúc này. Vui lòng liên hệ nhân viên hỗ trợ.";
        }
    }
}