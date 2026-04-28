package com.retailmanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PromotionRequest {

    private String code;
    private String name;
    private String description;

    // PERCENT / AMOUNT
    private String discountType;
    private BigDecimal discountValue;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // ALL / PRODUCT / VARIANT
    private String scope;
    private List<Integer> productIds;
    private List<Integer> variantIds;

    // ✅ VIP SUPPORT - Customer Groups
    private List<String> customerTypes;  // REGULAR, VIP
    private List<String> vipTiers;       // BRONZE, SILVER, GOLD, PLATINUM, DIAMOND

    private BigDecimal minOrderAmount;

    private Integer priority = 0;
    private Boolean stackable = false;
    private Boolean isActive = true;

    // Giới hạn số lần dùng (toàn hệ thống). Null = không giới hạn
    private Integer usageLimit;

    // Combo mua X tặng Y (ví dụ mua 2 tặng 1 => buyQty=2, getQty=1). Null/0 = không áp dụng
    private Integer buyQty;
    private Integer getQty;
    
}