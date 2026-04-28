package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    private Long orderItemId;
    private Integer productId;
    private Integer variantId;
    private String productName;
    private String variantName;
    private String sku;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal lineTotal;
}