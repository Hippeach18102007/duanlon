package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionHistoryResponse {

    private String type; // "PROMOTION_CODE" | "SPIN_WHEEL"

    // Thông tin đơn hàng
    private Long orderId;
    private String orderNumber;
    private LocalDateTime usedAt;

    // Thông tin ưu đãi
    private String promotionCode;
    private String promotionName;
    private String discountType;   // PERCENT / AMOUNT / SPIN
    private BigDecimal discountValue;
    private BigDecimal discountTotal; // số tiền thực tế được giảm

    // Trạng thái (dùng cho spin wheel)
    private String status; // "Đã sử dụng" | "Đang hoạt động" | "Đã hết hạn"
    private LocalDateTime expiresAt;
}