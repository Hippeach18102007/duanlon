package com.retailmanagement.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertPriceRequest {

    private String currencyCode;   // null -> dùng DEFAULT_CURRENCY
    private BigDecimal price;
    private BigDecimal costPrice;
    private String reason;          // MANUAL / PROMO ...
}
