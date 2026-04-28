package com.retailmanagement.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ProductVariantResponse {
    private Integer id;
    private Integer productId;
    private String variantName;
    private String sku;
    private String barcode;
    private String currencyCode;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private String attributesJson;
    private Boolean isActive;
    private Instant createdAt;
}
