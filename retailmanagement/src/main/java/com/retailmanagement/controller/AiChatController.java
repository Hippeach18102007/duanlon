package com.retailmanagement.controller;

import com.retailmanagement.dto.request.ChatRequest;
import com.retailmanagement.dto.response.ChatResponse;
import com.retailmanagement.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * AI Chat Controller
 *
 * Endpoints:
 *  POST /api/chat/customer   → Khách hàng chat (cần login hoặc guest)
 *  POST /api/chat/staff      → Nhân viên sale dùng AI assistant
 *  DELETE /api/chat/session/{sessionId} → Xóa session
 *
 * Headers cần thiết:
 *  Authorization: Bearer <JWT_TOKEN>
 *
 * Cho Staff endpoint, thêm query param:
 *  ?customerEmail=khach@email.com   (optional - để AI có context về khách)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    // ========================================================================
    // CUSTOMER CHAT — public (guest) hoặc có JWT
    // Nếu guest: không có customer context
    // Nếu đã login: AI tự động load thông tin điểm/VIP của khách
    // ========================================================================

    @PostMapping("/customer")
    public ResponseEntity<ChatResponse> customerChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Lấy email từ JWT nếu đã đăng nhập, null nếu guest
        String customerEmail = userDetails != null ? userDetails.getUsername() : null;
        ChatResponse response = aiChatService.customerChat(request, customerEmail);
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // STAFF CHAT — chỉ dành cho nhân viên (ADMIN hoặc STAFF role)
    // Query param ?customerEmail= để AI hiểu đang hỗ trợ khách nào
    // ========================================================================

    @PostMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<ChatResponse> staffChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String customerEmail
    ) {
        String staffUsername = userDetails.getUsername();
        ChatResponse response = aiChatService.staffChat(request, staffUsername, customerEmail);
        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // XÓA SESSION — dùng khi khách logout hoặc bắt đầu cuộc hội thoại mới
    // ========================================================================

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        // Tùy chọn: xóa hoặc giữ lại để phân tích sau
        // sessionRepository.deleteBySessionId(sessionId);
        return ResponseEntity.noContent().build();
    }
}