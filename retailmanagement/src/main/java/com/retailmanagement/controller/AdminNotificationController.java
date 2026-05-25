package com.retailmanagement.controller;

import com.retailmanagement.dto.response.NotificationResponse;
import com.retailmanagement.entity.Notification;
import com.retailmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/return-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponse>> getReturnRequests() {
        List<Notification> notifications = notificationService.getReturnRequestHistory();
        List<NotificationResponse> response = notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .customerId(notification.getCustomer() != null ? notification.getCustomer().getId() : null)
                .customerName(notification.getCustomer() != null ? notification.getCustomer().getName() : null)
                .customerEmail(notification.getCustomer() != null ? notification.getCustomer().getEmail() : null)
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

