package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;

    private Long orderItemId;
    private String productName;
    private String variantName;

    private Integer quantity;
    private String reason;

    private BigDecimal refundAmount;
    private String refundMethod;
    private String refundStatus;

    private String status;
    private String note;

    private Integer customerId;
    private String customerName;

    private Integer processedBy;
    private String processedByName;

    private Instant createdAt;
    private Instant refundedAt;

    private String imageUrl;
}