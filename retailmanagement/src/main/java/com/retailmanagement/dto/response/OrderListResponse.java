package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;


@Data
@AllArgsConstructor
public class OrderListResponse {

    private Long orderId;
    private String orderNumber;

    private Integer customerId;      // ✅ thêm
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private String customerEmail;

    private String staffName;

    private String channel;           // ✅ thêm
    private String paymentMethod;     // ✅ thêm

    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
    private Instant createdAt;

    private Instant shippedAt;
    private Instant updatedAt;

    private BigDecimal discountTotal;

    public OrderListResponse() {

    }
}


