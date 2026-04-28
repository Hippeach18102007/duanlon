package com.retailmanagement.dto.response;

import com.retailmanagement.entity.CustomerType;
import com.retailmanagement.entity.VipTier;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierProgressResponse {

    // Thông tin khách hàng hiện tại
    private Integer customerId;
    private String customerName;
    private CustomerType currentCustomerType;
    private VipTier currentTier;
    private String currentTierDisplay;
    private Integer currentPoints;
    private BigDecimal currentSpent;
    private Double currentDiscountRate;

    // Thông tin hạng tiếp theo
    private VipTier nextTier;
    private String nextTierDisplay;
    private Integer nextTierMinPoints;
    private Double nextTierDiscountRate;

    // Khoảng cách cần thiết
    private Integer pointsGap;
    private BigDecimal amountGapToNextTier; // Số tiền cần chi thêm (1 VND = 0.1 điểm)
    private Integer progressPercentage; // % đã hoàn thành

    // Upgrade from REGULAR to VIP
    private Boolean willUpgradeToVip;
    private Integer pointsNeededForVip; // Cần bao nhiêu điểm nữa để lên VIP (GOLD)
    private BigDecimal amountNeededForVip;

    // Thông báo động lực
    private String motivationMessage;
    private String tierBenefitMessage;

    // Trạng thái gần lên hạng
    private Boolean isCloseToUpgrade; // Nếu < 20% nữa là lên hạng
    private Boolean canUpgradeWithCurrentCart; // Nếu giỏ hàng hiện tại đủ lên hạng
}