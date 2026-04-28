package com.retailmanagement.dto.response;

import com.retailmanagement.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;

    // ✅ Thông tin khách hàng
    private Integer customerId;
    private String customerName;
    private String customerEmail;

    // Thông tin thông báo
    private NotificationType type;
    private String typeDisplay;
    private String icon;
    private String title;
    private String message;

    // Trạng thái
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}