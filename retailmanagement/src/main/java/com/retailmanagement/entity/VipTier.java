package com.retailmanagement.entity;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum VipTier {
    // DiscountRate: dùng cho PLATINUM & DIAMOND (theo %)
    // DiscountAmount: dùng cho BRONZE, SILVER, GOLD (số tiền cố định)
    BRONZE("Bronze",   1000,   1999, 0.0,  BigDecimal.valueOf(50_000)),
    SILVER("Silver",   2000,   4999, 0.0,  BigDecimal.valueOf(100_000)),
    GOLD("Gold",       5000,   9999, 0.0,  BigDecimal.valueOf(150_000)),
    PLATINUM("Platinum", 10000, 19999, 0.03, BigDecimal.ZERO),
    DIAMOND("Diamond",   20000, Integer.MAX_VALUE, 0.05, BigDecimal.ZERO);

    private final String displayName;
    private final Integer minPoints;
    private final Integer maxPoints;

    /**
     * Tỷ lệ % giảm giá — chỉ dùng cho PLATINUM & DIAMOND.
     * Với BRONZE/SILVER/GOLD = 0.0 (dùng discountAmount thay thế).
     */
    private final Double discountRate;

    /**
     * Số tiền giảm cố định — chỉ dùng cho BRONZE, SILVER, GOLD.
     * Với PLATINUM/DIAMOND = 0.
     */
    private final BigDecimal discountAmount;

    VipTier(String displayName, Integer minPoints, Integer maxPoints,
            Double discountRate, BigDecimal discountAmount) {
        this.displayName   = displayName;
        this.minPoints     = minPoints;
        this.maxPoints     = maxPoints;
        this.discountRate  = discountRate;
        this.discountAmount = discountAmount;
    }

    /** Đơn từ bao nhiêu mới được hưởng ưu đãi của tier này */
    public BigDecimal getMinOrderToApply() {
        return switch (this) {
            case BRONZE, SILVER, GOLD       -> BigDecimal.valueOf(5_000_000);
            case PLATINUM, DIAMOND          -> BigDecimal.valueOf(10_000_000);
        };
    }

    /** Tier dùng giảm theo % hay theo số tiền cố định? */
    public boolean isPercentageDiscount() {
        return discountRate > 0;
    }

    public static VipTier fromPoints(Integer points) {
        if (points == null || points < 1000) return null;
        for (VipTier tier : values()) {
            if (points >= tier.minPoints && points <= tier.maxPoints) {
                return tier;
            }
        }
        return DIAMOND;
    }

    public Integer getNextTierThreshold() {
        VipTier[] tiers = values();
        int currentIndex = this.ordinal();
        if (currentIndex < tiers.length - 1) {
            return tiers[currentIndex + 1].minPoints;
        }
        return null;
    }
}