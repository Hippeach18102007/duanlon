package com.retailmanagement.dto.response;

import lombok.*;

import java.math.BigDecimal;
// ProductSummaryDto.java - dùng @Value interface hoặc class với đúng constructor
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDto {
    private String name;        // p.name
    private String sku;         // p.sku
    private String brand;       // p.brand
    private String description; // p.description
    private String categoryName;// c.name
    private BigDecimal minPrice;// MIN(v.price)
    private BigDecimal maxPrice;// MAX(v.price)
    private Long totalStock;    // SUM(...) → Long, KHÔNG phải Integer
    private Boolean isNew;      // p.isNew
}