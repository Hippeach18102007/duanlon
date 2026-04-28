package com.retailmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequest {

    @NotBlank(message = "Tên biến thể không được để trống")
    private String variantName;

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    private String barcode;

    @NotNull(message = "Giá bán không được để trống")
    private BigDecimal price;

    private BigDecimal costPrice;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    private Integer stockQuantity;

    private String attributesJson;

    private Boolean isActive;
}
