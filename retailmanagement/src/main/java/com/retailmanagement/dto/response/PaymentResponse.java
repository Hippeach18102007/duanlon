package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    // ===== THÔNG TIN CƠ BẢN (đã có sẵn) =====
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String method;
    private String transactionRef;
    private String status;
    private Instant paidAt;
    private Instant createdAt;
    private Integer customerId;
    private String customerName;

    // ===== THÔNG TIN BỔ SUNG (cho detail) =====
    private String orderNumber;
    private String customerEmail;
    private String customerPhone;
    private String customerType;
    private String vipTier;
    private Instant deletedAt;

    // Thông tin đơn hàng
    private String orderStatus;
    private String channel;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String appliedPromotionCode;
    private String notes;

    // Danh sách sản phẩm (CHỈ có khi load detail)
    private List<PaymentItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentItem {
        private Long itemId;
        private Integer productId;
        private String productName;
        private Integer variantId;
        private String variantName;
        private String sku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal lineTotal;
        private String imageUrl;
    }
}