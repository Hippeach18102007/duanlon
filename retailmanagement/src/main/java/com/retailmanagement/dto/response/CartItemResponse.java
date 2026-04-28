package com.retailmanagement.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CartItemResponse {

    private Integer cartItemId;
    private Integer variantId;
    private String productName;
    private String variantName;
    private String imageUrl;
    private BigDecimal price;
    private Integer quantity;
}
