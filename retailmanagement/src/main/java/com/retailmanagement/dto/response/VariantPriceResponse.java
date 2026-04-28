package com.retailmanagement.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantPriceResponse {

    private Integer variantId;
    private Integer productId;
    private String variantName;
    private String sku;

    private String currencyCode;

    private BigDecimal price;
    private BigDecimal costPrice;

    private String promotionCode;
    private String promotionName;

    private BigDecimal finalPrice;

    private BigDecimal discountAmount;

    private String comboInfo;

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
        if (this.price != null && finalPrice != null) {
            BigDecimal diff = this.price.subtract(finalPrice);
            this.discountAmount = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        } else {
            this.discountAmount = BigDecimal.ZERO;
        }
    }
}