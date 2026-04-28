package com.retailmanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.retailmanagement.constants.GroqClient;
import com.retailmanagement.dto.request.ChatMessage;
import com.retailmanagement.dto.request.ChatRequest;
import com.retailmanagement.dto.response.ChatResponse;
import com.retailmanagement.dto.response.CustomerResponse;
import com.retailmanagement.dto.response.ProductSummaryDto;
import com.retailmanagement.entity.ChatSession;
import com.retailmanagement.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_HISTORY_TURNS = 20;

    private final GroqClient groqClient;
    private final ChatSessionRepository sessionRepository;
    private final CustomerService customerService;
    private final ObjectMapper objectMapper;
    private final ProductAiService productAiService;


    public ChatResponse customerChat(ChatRequest request, String customerEmail) {
        CustomerResponse customer = null;
        if (customerEmail != null) {
            try {
                customer = customerService.findByEmail(customerEmail);
            } catch (Exception e) {
                log.warn("Could not load customer for email {}: {}", customerEmail, e.getMessage());
            }
        }

        ChatSession session = resolveSession(request.getSessionId(), "CUSTOMER",
                customer != null ? customer.getId() : null);

        List<ChatMessage> history = loadHistory(session);
        history.add(new ChatMessage("user", request.getMessage()));

        String systemPrompt = buildCustomerSystemPrompt(customer, request.getMessage());
        String reply = groqClient.chat(systemPrompt, history);

        history.add(new ChatMessage("model", reply));
        saveHistory(session, history);

        String intent = detectIntent(request.getMessage(), reply);
        boolean escalate = shouldEscalate(request.getMessage(), reply);

        session.setLastIntent(intent);
        session.setEscalated(escalate);
        saveSessionFinal(session);

        return ChatResponse.builder()
                .sessionId(session.getSessionId())
                .reply(reply)
                .intent(intent)
                .escalate(escalate)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSessionFinal(ChatSession session) {
        sessionRepository.save(session);
    }
    public ChatResponse staffChat(ChatRequest request, String staffUsername, String targetCustomerEmail) {
        CustomerResponse customer = null;
        if (targetCustomerEmail != null) {
            try {
                customer = customerService.findByEmail(targetCustomerEmail);
            } catch (Exception e) {
                log.warn("Could not load customer {}: {}", targetCustomerEmail, e.getMessage());
            }
        }

        ChatSession session = resolveSession(request.getSessionId(), "STAFF", null);

        List<ChatMessage> history = loadHistory(session);
        history.add(new ChatMessage("user", request.getMessage()));

        String systemPrompt = buildStaffSystemPrompt(staffUsername, customer);
        String reply = groqClient.chat(systemPrompt, history);

        history.add(new ChatMessage("model", reply));
        saveHistory(session, history);

        String intent = detectIntent(request.getMessage(), reply);
        session.setLastIntent(intent);
        saveSessionFinal(session);

        return ChatResponse.builder()
                .sessionId(session.getSessionId())
                .reply(reply)
                .intent(intent)
                .escalate(false)
                .build();
    }

    private String buildCustomerSystemPrompt(CustomerResponse customer, String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            Bạn là trợ lý AI của cửa hàng RetailPro — thân thiện, chuyên nghiệp.
            Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu.
            Khi tư vấn sản phẩm, chỉ giới thiệu sản phẩm có trong danh sách được cung cấp.
            Không bịa thêm sản phẩm không có trong danh sách.

            NHIỆM VỤ:
            1. Tư vấn sản phẩm và gợi ý mua hàng
            2. Tra cứu và giải thích điểm thưởng, hạng VIP
            3. Hỗ trợ tra cứu đơn hàng, chính sách đổi/hoàn/hủy
            4. Upsell và cross-sell tự nhiên khi phù hợp
            5. Vấn đề phức tạp (khiếu nại, hoàn tiền) → đề xuất gặp nhân viên

            CHÍNH SÁCH ĐIỂM THƯỞNG:
            - 10.000 VNĐ = 1 điểm | Cuối tuần x1.5 điểm
            - Member → Bronze (100đ) → Silver (500đ) → Gold (1000đ) → Platinum (5000đ)
            - VIP (Gold+) được giảm giá và ưu tiên hỗ trợ

            QUY TẮC: Không bịa thông tin. Không hứa hẹn không chắc chắn. Luôn lịch sự.
            """);

        // ── PRODUCT CONTEXT ──────────────────────────────────────────
        try {
            List<ProductSummaryDto> products = isProductQuery(userMessage)
                    ? productAiService.searchProducts(userMessage)
                    : productAiService.getTopProducts(15);
            String productContext = productAiService.formatProductContext(products);
            if (!productContext.isBlank()) {
                sb.append("\n").append(productContext).append("\n");
            }
        } catch (Exception e) {
            log.warn("Could not load product context: {}", e.getMessage());
        }
        // ─────────────────────────────────────────────────────────────

        if (customer != null) {
            sb.append("\n--- THÔNG TIN KHÁCH HÀNG ---\n");
            sb.append("Tên: ").append(customer.getName()).append("\n");
            sb.append("Hạng VIP: ").append(customer.getVipTierDisplay()).append("\n");
            sb.append("Điểm tích lũy: ").append(customer.getLoyaltyPoints()).append(" điểm\n");
            sb.append("Cần thêm để lên hạng: ").append(customer.getPointsToNextTier()).append(" điểm\n");
            sb.append("Tổng chi tiêu: ").append(formatVnd(customer.getTotalSpent())).append("\n");
            sb.append("Giảm giá hiện tại: ").append(customer.getDiscountRate()).append("%\n");
            sb.append("Số đơn hàng: ").append(customer.getOrderCount()).append("\n");
            if (customer.getVipNote() != null && !customer.getVipNote().isBlank()) {
                sb.append("Ghi chú VIP: ").append(customer.getVipNote()).append("\n");
            }
            sb.append("\nGọi khách bằng tên '").append(customer.getName()).append("'.");
        } else {
            sb.append("\nKhách chưa đăng nhập. Gợi ý tạo tài khoản để tích điểm.");
        }

        return sb.toString();
    }

    private String buildStaffSystemPrompt(String staffUsername, CustomerResponse customer) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Bạn là AI assistant hỗ trợ nhân viên bán hàng RetailPro.
                Trả lời tiếng Việt, chính xác, chuyên nghiệp.

                NHIỆM VỤ:
                1. Thông tin chi tiết về khách (điểm, hạng, lịch sử)
                2. Gợi ý kịch bản upsell/cross-sell theo profile khách
                3. Hỗ trợ xử lý tình huống khó (khiếu nại, hoàn hàng)
                4. Chiến lược tiếp cận theo phân khúc REGULAR/VIP
                """);

        sb.append("Nhân viên: ").append(staffUsername).append("\n");

        if (customer != null) {
            sb.append("\n--- KHÁCH ĐANG HỖ TRỢ ---\n");
            sb.append("Tên: ").append(customer.getName()).append("\n");
            sb.append("Email: ").append(customer.getEmail()).append("\n");
            sb.append("Điện thoại: ").append(customer.getPhone()).append("\n");
            sb.append("Hạng VIP: ").append(customer.getVipTierDisplay()).append("\n");
            sb.append("Điểm: ").append(customer.getLoyaltyPoints()).append(" | Còn thiếu: ").append(customer.getPointsToNextTier()).append("\n");
            sb.append("Tổng chi tiêu: ").append(formatVnd(customer.getTotalSpent())).append("\n");
            sb.append("Số đơn: ").append(customer.getOrderCount()).append("\n");
            sb.append("Loại: ").append(customer.getCustomerTypeDisplay()).append("\n");
            if (customer.getVipNote() != null && !customer.getVipNote().isBlank()) {
                sb.append("Ghi chú đặc biệt: ").append(customer.getVipNote()).append("\n");
            }
        }

        return sb.toString();
    }

    private String detectIntent(String userMessage, String aiReply) {
        String combined = (userMessage + " " + aiReply).toLowerCase();
        if (combined.matches(".*\\b(điểm|tích điểm|vip|hạng|tier|loyalty|thưởng)\\b.*")) return "LOYALTY_QUERY";
        if (combined.matches(".*\\b(đơn hàng|order|hủy|hoàn|đổi|trả|giao hàng)\\b.*")) return "ORDER_SUPPORT";
        if (combined.matches(".*\\b(sản phẩm|mua|giá|khuyến mãi|sale|giảm giá|tư vấn)\\b.*")) return "PRODUCT_ADVICE";
        if (combined.matches(".*\\b(upsell|thêm|combo|bundle|kèm theo)\\b.*")) return "UPSELL";
        return "GENERAL";
    }

    private boolean shouldEscalate(String userMessage, String aiReply) {
        String msg = userMessage.toLowerCase();
        return msg.contains("hoàn tiền") || msg.contains("khiếu nại")
                || msg.contains("tức") || msg.contains("bực")
                || msg.contains("quản lý") || msg.contains("gặp người thật")
                || aiReply.toLowerCase().contains("nhân viên hỗ trợ");
    }
@Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatSession resolveSession(String sessionId, String actorRole, Integer customerId) {
        if (sessionId != null) {
            return sessionRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createSession(sessionId, actorRole, customerId));
        }
        return createSession(UUID.randomUUID().toString(), actorRole, customerId);
    }

    private ChatSession createSession(String sessionId, String actorRole, Integer customerId) {
        return sessionRepository.save(ChatSession.builder()
                .sessionId(sessionId)
                .actorRole(actorRole)
                .customerId(customerId)
                .historyJson("[]")
                .build());
    }

    private List<ChatMessage> loadHistory(ChatSession session) {
        try {
            List<ChatMessage> history = objectMapper.readValue(
                    session.getHistoryJson(),
                    new TypeReference<List<ChatMessage>>() {});
            int max = MAX_HISTORY_TURNS * 2;
            if (history.size() > max) history = history.subList(history.size() - max, history.size());
            return new ArrayList<>(history);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveHistory(ChatSession session, List<ChatMessage> history) {
        try {
            session.setHistoryJson(objectMapper.writeValueAsString(history));
        } catch (Exception e) {
            log.error("Could not serialize chat history", e);
        }
    }

    private String formatVnd(java.math.BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        return String.format("%,d VNĐ", amount.longValue());
    }
    private boolean isProductQuery(String message) {
        if (message == null) return false;
        String m = message.toLowerCase();
        return m.matches(".*\\b(sản phẩm|mua|giá|tìm|có bán|còn hàng|tư vấn|gợi ý|thương hiệu|brand|loại|dòng)\\b.*");
    }
}