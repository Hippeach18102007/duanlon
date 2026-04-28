package com.retailmanagement.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderItemRequest {

    private Integer variantId;
    private Integer quantity;
    private List<String> serialNumbers;
}
