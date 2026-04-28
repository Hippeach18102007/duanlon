package com.retailmanagement.dto.request;

import lombok.Data;

@Data
public class UpdateOrderRequest {
    private String paymentMethod;
    private String notes;
}

