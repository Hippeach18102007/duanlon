package com.retailmanagement.entity;

import lombok.Getter;

@Getter
public enum NotificationType {
    WELCOME("Nhắc khách hàng chưa mua hàng ", ""),
    PURCHASE_REMINDER("Nhắc mua hàng", "🛒"),
    WINBACK("Khách hàng cũ", "💝"),
    BIRTHDAY("Sinh nhật", "🎂"),
    ORDER_STATUS("Trạng thái đơn hàng", "📦"),
    PROMOTION("Khuyến mãi", "🎁"),
    LOYALTY_POINTS("Điểm tích lũy", "⭐"),
    VIP_TIER_UPGRADE("Nâng hạng VIP", "👑"),
    SYSTEM("Hệ thống", "🔔"),
    SPIN_EXPIRY_WARNING("Ưu đãi vòng quay sắp hết hạn", "🎡"),
    ORDER_UPDATE("Cập nhật về đơn hàng", ""),
    ORDER_AUTO_COMPLETED("Giao hàng tự động", "✅");

    private final String displayName;
    private final String icon;

    NotificationType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
}