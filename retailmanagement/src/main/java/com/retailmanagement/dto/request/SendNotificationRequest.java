package com.retailmanagement.dto.request;

import lombok.Data;
import java.util.List;

/**
 * ================================================================
 * FILE: src/main/java/com/retailmanagement/dto/request/SendNotificationRequest.java
 * DTO dùng cho POST /api/auth/notifications/send (admin gửi thủ công)
 * ================================================================
 */
@Data
public class SendNotificationRequest {
    /** Danh sách ID khách hàng cần gửi */
    private List<Integer> customerIds;
    /** Tiêu đề thông báo */
    private String title;
    /** Nội dung thông báo (đã được personalize {name} ở FE trước khi gửi) */
    private String message;
    /** Loại thông báo: "WELCOME", "PURCHASE_REMINDER", "WINBACK", ... */
    private String type;
}