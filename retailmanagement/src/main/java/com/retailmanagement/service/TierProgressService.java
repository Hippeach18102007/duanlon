package com.retailmanagement.service;

import com.retailmanagement.dto.response.TierProgressResponse;
import com.retailmanagement.entity.Customer;
import com.retailmanagement.entity.CustomerType;
import com.retailmanagement.entity.VipTier;
import com.retailmanagement.repository.CustomRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class TierProgressService {

    private final CustomRes customerRepository;

    // Hằng số: 10,000 VND = 1 điểm
    private static final BigDecimal POINTS_CONVERSION_RATE = BigDecimal.valueOf(10000);

    /**
     * Tính toán tiến trình lên hạng cho khách hàng
     */
    public TierProgressResponse calculateTierProgress(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        VipTier currentTier = customer.getVipTier();
        CustomerType currentType = customer.getCustomerType();

        // Tìm hạng tiếp theo
        VipTier nextTier = getNextTier(currentTier);

        // Tính khoảng cách
        Integer pointsGap = 0;
        Integer nextTierMinPoints = 0;
        Integer progressPercentage = 0;

        if (nextTier != null) {
            nextTierMinPoints = nextTier.getMinPoints();
            pointsGap = nextTierMinPoints - currentPoints;

            // Tính % tiến trình (trong khoảng hạng hiện tại)
            if (currentTier != null) {
                int tierRange = nextTier.getMinPoints() - currentTier.getMinPoints();
                int progress = currentPoints - currentTier.getMinPoints();
                progressPercentage = tierRange > 0 ? (progress * 100) / tierRange : 0;
            } else {
                // Nếu chưa có tier, tính từ 0
                progressPercentage = (currentPoints * 100) / nextTierMinPoints;
            }
        }

        // Tính số tiền cần chi thêm
        BigDecimal amountGap = pointsGap > 0
                ? BigDecimal.valueOf(pointsGap).multiply(POINTS_CONVERSION_RATE)
                : BigDecimal.ZERO;

        // Kiểm tra có lên VIP không
        boolean willUpgradeToVip = false;
        Integer pointsNeededForVip = 0;
        BigDecimal amountNeededForVip = BigDecimal.ZERO;

        if (currentType == CustomerType.REGULAR) {
            // GOLD là hạng đầu tiên được tính VIP (5000 điểm)
            int goldMinPoints = VipTier.GOLD.getMinPoints();
            if (currentPoints < goldMinPoints) {
                pointsNeededForVip = goldMinPoints - currentPoints;
                amountNeededForVip = BigDecimal.valueOf(pointsNeededForVip)
                        .multiply(POINTS_CONVERSION_RATE);
                willUpgradeToVip = true;
            }
        }

        // Tạo message động lực
        String motivationMessage = generateMotivationMessage(
                currentPoints, pointsGap, progressPercentage, currentType, willUpgradeToVip
        );

        String tierBenefitMessage = generateTierBenefitMessage(nextTier);

        // Kiểm tra gần lên hạng (< 20% nữa)
        boolean isCloseToUpgrade = progressPercentage >= 80;

        return TierProgressResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .currentCustomerType(currentType)
                .currentTier(currentTier)
                .currentTierDisplay(currentTier != null ? currentTier.getDisplayName() : "Member")
                .currentPoints(currentPoints)
                .currentSpent(customer.getTotalSpent())
                .currentDiscountRate(currentTier != null ? currentTier.getDiscountRate() : 0.0)
                .nextTier(nextTier)
                .nextTierDisplay(nextTier != null ? nextTier.getDisplayName() : null)
                .nextTierMinPoints(nextTierMinPoints)
                .nextTierDiscountRate(nextTier != null ? nextTier.getDiscountRate() : null)
                .pointsGap(pointsGap)
                .amountGapToNextTier(amountGap)
                .progressPercentage(Math.min(progressPercentage, 100))
                .willUpgradeToVip(willUpgradeToVip)
                .pointsNeededForVip(pointsNeededForVip)
                .amountNeededForVip(amountNeededForVip)
                .motivationMessage(motivationMessage)
                .tierBenefitMessage(tierBenefitMessage)
                .isCloseToUpgrade(isCloseToUpgrade)
                .canUpgradeWithCurrentCart(false) // Sẽ tính khi có giỏ hàng
                .build();
    }

    /**
     * Tính toán với giỏ hàng hiện tại
     */
    public TierProgressResponse calculateWithCart(Integer customerId, BigDecimal cartTotal) {
        TierProgressResponse progress = calculateTierProgress(customerId);

        if (cartTotal != null && cartTotal.compareTo(BigDecimal.ZERO) > 0) {
            int pointsFromCart = cartTotal.divide(POINTS_CONVERSION_RATE, 0, RoundingMode.DOWN).intValue();
            int totalPointsAfterPurchase = progress.getCurrentPoints() + pointsFromCart;

            // Kiểm tra có lên hạng sau khi mua không
            boolean canUpgrade = false;
            if (progress.getNextTier() != null) {
                canUpgrade = totalPointsAfterPurchase >= progress.getNextTierMinPoints();
            }

            progress.setCanUpgradeWithCurrentCart(canUpgrade);

            // Cập nhật message nếu đủ điều kiện lên hạng
            if (canUpgrade) {
                progress.setMotivationMessage(
                        "🎉 Hoàn tất đơn hàng này để lên hạng " +
                                progress.getNextTierDisplay() + " ngay!"
                );
            }
        }

        return progress;
    }

    /**
     * Lấy hạng tiếp theo
     */
    private VipTier getNextTier(VipTier currentTier) {
        if (currentTier == null) {
            return VipTier.BRONZE;
        }

        VipTier[] tiers = VipTier.values();
        int currentIndex = currentTier.ordinal();

        if (currentIndex < tiers.length - 1) {
            return tiers[currentIndex + 1];
        }

        return null; // Đã ở hạng cao nhất
    }

    /**
     * Tạo thông báo động lực
     */
    private String generateMotivationMessage(
            int currentPoints,
            int pointsGap,
            int progressPercentage,
            CustomerType currentType,
            boolean willUpgradeToVip
    ) {
        if (pointsGap <= 0) {
            return "🏆 Bạn đã đạt hạng cao nhất! Cảm ơn sự ủng hộ của bạn.";
        }

        // Thông báo lên VIP
        if (currentType == CustomerType.REGULAR && willUpgradeToVip) {
            if (progressPercentage >= 80) {
                return String.format(
                        "👑 Chỉ còn %s VND nữa là bạn trở thành khách hàng VIP với ưu đãi đặc biệt!",
                        formatMoney(BigDecimal.valueOf(pointsGap).multiply(POINTS_CONVERSION_RATE))
                );
            } else if (progressPercentage >= 50) {
                return "⭐ Bạn đã đi được hơn nửa chặng đường để trở thành VIP!";
            } else {
                return "🌟 Tiếp tục mua sắm để nâng cấp lên khách hàng VIP và nhận nhiều ưu đãi hơn!";
            }
        }

        // Thông báo lên hạng thông thường
        if (progressPercentage >= 90) {
            return String.format(
                    "🔥 Chỉ còn %,d điểm nữa thôi! Bạn sắp lên hạng rồi!",
                    pointsGap
            );
        } else if (progressPercentage >= 70) {
            return String.format(
                    "💪 Gần đến đích rồi! Còn %,d điểm nữa là lên hạng mới.",
                    pointsGap
            );
        } else if (progressPercentage >= 50) {
            return String.format(
                    "📈 Bạn đã hoàn thành %d%% chặng đường lên hạng tiếp theo!",
                    progressPercentage
            );
        } else {
            return String.format(
                    "🎯 Tích điểm thêm %,d điểm để lên hạng mới và nhận ưu đãi tốt hơn!",
                    pointsGap
            );
        }
    }

    /**
     * Tạo thông báo lợi ích của hạng tiếp theo
     */
    private String generateTierBenefitMessage(VipTier nextTier) {
        if (nextTier == null) {
            return "Bạn đã ở hạng cao nhất với mức ưu đãi tốt nhất!";
        }

        double discountRate = nextTier.getDiscountRate() * 100;

        switch (nextTier) {
            case BRONZE:
                return String.format("Hạng Bronze: Giảm %.0f%% mọi đơn hàng + Ưu tiên hỗ trợ", discountRate);
            case SILVER:
                return String.format("Hạng Silver: Giảm %.0f%% + Quà tặng sinh nhật + Miễn phí ship", discountRate);
            case GOLD:
                return String.format("👑 Hạng Gold (VIP): Giảm %.0f%% + Ưu đãi độc quyền + Tích điểm x2", discountRate);
            case PLATINUM:
                return String.format("💎 Hạng Platinum (VIP): Giảm %.0f%% + Tư vấn cá nhân + Sự kiện riêng", discountRate);
            case DIAMOND:
                return String.format("💠 Hạng Diamond (VIP): Giảm %.0f%% + Đặc quyền cao cấp nhất + Quà tặng VIP", discountRate);
            default:
                return "Lên hạng để nhận nhiều ưu đãi hơn!";
        }
    }

    /**
     * Format tiền VND
     */
    private String formatMoney(BigDecimal amount) {
        return String.format("%,d", amount.longValue());
    }
}