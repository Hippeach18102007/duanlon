package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryInfoResponse {

    private Long orderId;
    private String orderNumber;
    private String status;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String deliveryProofUrl;
    private Instant shipperConfirmedAt;
    private BigDecimal totalAmount;
    private String paymentMethod;
}

