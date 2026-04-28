package com.retailmanagement.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {

    private Integer customerId;
    private String paymentMethod; // CASH / BANK / COD
    private String channel;        // ONLINE / OFFLINE
    private String notes;
    private String promotionCode;  // ✅ THÊM MỚI: mã giảm giá (nullable, VD: "BIRTHDAY250K")
    private BigDecimal shippingFee;     // ✅ THÊM MỚI: phí vận chuyển (nullable, VD: 15000)
    private Long spinHistoryId;
    private List<CreateOrderItemRequest> items;
    private Integer variantId;
    private Integer quantity;
    private List<String> serialNumbers;

}