package com.retailmanagement.controller;


import com.retailmanagement.dto.request.SendNotificationRequest;
import com.retailmanagement.dto.response.NotificationResponse;
import com.retailmanagement.entity.Notification;
import com.retailmanagement.security.service.CustomUserDetails;
import com.retailmanagement.service.CustomerService;

import com.retailmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CustomerService customerService;

    // ================================================================
    // CUSTOMER — thông báo của tôi
    // ================================================================

    @GetMapping("/my")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly
    ) {
        Integer customerId = getCurrentCustomerId();
        List<Notification> notifications = unreadOnly
                ? notificationService.getUnreadNotifications(customerId)
                : notificationService.getAllNotifications(customerId);
        return ResponseEntity.ok(notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Integer customerId = getCurrentCustomerId();
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.countUnread(customerId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, getCurrentCustomerId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(getCurrentCustomerId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id, getCurrentCustomerId());
        return ResponseEntity.noContent().build();
    }

    // ================================================================
    // ADMIN — Purchase Reminder
    // ================================================================

    @GetMapping("/purchase-reminders/history")
    public ResponseEntity<List<NotificationResponse>> getPurchaseReminderHistory() {
        return ResponseEntity.ok(notificationService.getPurchaseReminderHistory().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    @PostMapping("/purchase-reminders/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<?> triggerPurchaseReminders() {
        notificationService.sendPurchaseReminders();
        return ResponseEntity.ok(Map.of("message", "Triggered successfully"));
    }

    // ================================================================
    // ADMIN — Spin Expiry
    // ================================================================

    @PostMapping("/spin-expiry/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<?> triggerSpinExpiryWarnings() {
        notificationService.sendSpinExpiryWarnings();
        return ResponseEntity.ok(Map.of("message", "Spin expiry warnings triggered successfully"));
    }

    // ================================================================
    // ADMIN — Gửi thông báo tùy chỉnh
    // ================================================================

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @RequestBody SendNotificationRequest request) {

        if (request.getCustomerIds() == null || request.getCustomerIds().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "customerIds không được rỗng"));
        if (request.getTitle() == null || request.getTitle().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "title không được rỗng"));
        if (request.getMessage() == null || request.getMessage().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "message không được rỗng"));

        return ResponseEntity.ok(notificationService.sendToCustomers(request));
    }

    // ================================================================
    // ✅ THÊM MỚI — ADMIN: Birthday
    // ================================================================

    /**
     * Trigger thủ công job sinh nhật (test hoặc gửi bù nếu scheduler bị miss).
     * POST /api/auth/notifications/birthday/trigger
     */
    @PostMapping("/birthday/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> triggerBirthdayNotifications() {
        notificationService.sendBirthdayNotifications();
        return ResponseEntity.ok(Map.of(
                "message", "Đã gửi thông báo sinh nhật thành công",
                "voucherCode", "BIRTHDAY250K",
                "discount", "250.000đ",
                "minOrder", "1.000.000đ",
                "validDays", 7
        ));
    }

    /**
     * Xem lịch sử thông báo sinh nhật đã gửi.
     * GET /api/auth/notifications/birthday/history
     */
    @GetMapping("/birthday/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<NotificationResponse>> getBirthdayHistory() {
        return ResponseEntity.ok(notificationService.getBirthdayNotificationHistory().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private Integer getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Chưa đăng nhập");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Integer customerId = userDetails.getCustomerId();
        if (customerId == null) {
            throw new RuntimeException("Tài khoản này không phải customer");
        }
        return customerId;
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .typeDisplay(notification.getType().getDisplayName())
                .icon(notification.getType().getIcon())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}