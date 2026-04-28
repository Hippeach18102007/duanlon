package com.retailmanagement.service;

import com.retailmanagement.dto.response.*;
import com.retailmanagement.entity.Customer;
import com.retailmanagement.entity.VipTier;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerMyDashboardService {

    private final CustomRes customerRepository;
    private final CustomerService customerService;
    private final LoyaltyHistoryService loyaltyHistoryService;
    private final PromotionHistoryService promotionHistoryService;
    private final SpinWheelService spinWheelService;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ================================================================
    // MAIN — gọi từ controller, nhận email từ SecurityContext
    // ================================================================

    public CustomerMyDashboardResponse getMyDashboard(String username) {
        // 1. Tìm khách hàng theo username/email
        CustomerResponse profile = customerService.findByEmail(username);
        if (profile == null) {
            throw new RuntimeException("Không tìm thấy khách hàng: " + username);
        }

        Customer customer = customerRepository.findById(profile.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + profile.getId()));

        Integer customerId = customer.getId();

        // 2. Fetch song song các phần data
        List<LoyaltyLedgerResponse> loyaltyHistory = safeGet(
                () -> loyaltyHistoryService.getCustomerHistory(customerId)
                        .stream().limit(5).toList(),
                List.of()
        );

        List<PromotionHistoryResponse> promoHistory = safeGet(
                () -> promotionHistoryService.getPromotionHistory(customerId)
                        .stream().limit(5).toList(),
                List.of()
        );

        List<PaymentResponse> payments = safeGet(
                () -> paymentService.getPaymentsByCustomerId(customerId),
                List.of()
        );

        Map<String, Object> spinStatus = safeGet(
                () -> spinWheelService.getSpinStatus(customerId),
                Map.of()
        );

        long totalOrders = orderRepository.countByCustomerId(customerId);

        // 3. Assemble
        return CustomerMyDashboardResponse.builder()
                .profile(buildProfile(customer))
                .summary(buildSummary(customer, totalOrders, payments))
                .recentLoyaltyHistory(loyaltyHistory)
                .recentPayments(buildRecentPayments(payments))
                .spinWheel(buildSpinStatus(spinStatus, customer))
                .recentPromotions(promoHistory)
                .vipJourney(buildVipJourney(customer, loyaltyHistoryService.getTierChanges(customerId)))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ================================================================
    // BUILDERS
    // ================================================================

    private CustomerMyDashboardResponse.ProfileSection buildProfile(Customer c) {
        return CustomerMyDashboardResponse.ProfileSection.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .dateOfBirth(c.getDateOfBirth() != null
                        ? c.getDateOfBirth().format(DATE_FMT) : null)
                .customerType(c.getCustomerType() != null
                        ? c.getCustomerType().name() : null)
                .customerTypeDisplay(c.getCustomerType() != null
                        ? c.getCustomerType().getDisplayname() : null)
                .vipTier(c.getVipTier() != null ? c.getVipTier().name() : null)
                .vipTierDisplay(c.getVipTier() != null
                        ? c.getVipTier().getDisplayName() : "Member")
                .isActive(c.getIsActive())
                .memberSince(c.getCreatedAt())
                .vipNote(c.getVipNote())
                .build();
    }

    private CustomerMyDashboardResponse.AccountSummary buildSummary(
            Customer c, long totalOrders, List<PaymentResponse> payments) {

        int points = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
        VipTier currentTier = c.getVipTier();

        // Tính pointsToNextTier và nextTier
        int pointsToNext = 0;
        String nextTierName = null;
        if (currentTier == null) {
            pointsToNext = VipTier.BRONZE.getMinPoints() - points;
            nextTierName = VipTier.BRONZE.getDisplayName();
        } else {
            Integer nextThreshold = currentTier.getNextTierThreshold();
            if (nextThreshold != null) {
                pointsToNext = Math.max(0, nextThreshold - points);
                // Lấy tên tier tiếp theo
                VipTier[] tiers = VipTier.values();
                for (int i = 0; i < tiers.length - 1; i++) {
                    if (tiers[i] == currentTier) {
                        nextTierName = tiers[i + 1].getDisplayName();
                        break;
                    }
                }
            }
        }

        // Avg order value từ payments
        BigDecimal totalSpent = c.getTotalSpent() != null ? c.getTotalSpent() : BigDecimal.ZERO;
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalSpent.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Spin bonus
        BigDecimal spinBonus = c.getSpinDiscountBonus() != null ? c.getSpinDiscountBonus() : BigDecimal.ZERO;

        // Discount rate display
        double rate = currentTier != null ? currentTier.getDiscountRate() : 0.0;
        String discountDisplay = rate > 0
                ? String.format("%.0f%%", rate * 100) : "0%";

        return CustomerMyDashboardResponse.AccountSummary.builder()
                .loyaltyPoints(points)
                .pointsToNextTier(Math.max(0, pointsToNext))
                .nextTier(nextTierName)
                .discountRate(rate)
                .discountDisplay(discountDisplay)
                .totalSpent(totalSpent)
                .totalOrders((int) totalOrders)
                .avgOrderValue(avgOrderValue)
                .spinBonus(spinBonus)
                .hasActiveSpinBonus(spinBonus.compareTo(BigDecimal.ZERO) > 0)
                .lastOrderAt(c.getLastOrderAt())
                .build();
    }

    private List<CustomerMyDashboardResponse.RecentPayment> buildRecentPayments(
            List<PaymentResponse> payments) {

        return payments.stream()
                .limit(5)
                .map(p -> CustomerMyDashboardResponse.RecentPayment.builder()
                        .paymentId(p.getId())
                        .orderId(p.getOrderId())
                        .orderNumber(p.getOrderNumber())
                        .amount(p.getAmount())
                        .method(p.getMethod())
                        .status(p.getStatus())
                        .paidAt(p.getPaidAt() != null
                                ? LocalDateTime.ofInstant(p.getPaidAt(),
                                        java.time.ZoneId.systemDefault())
                                .format(DATETIME_FMT)
                                : null)
                        .discountTotal(p.getDiscountTotal())
                        // itemCount cần load detail — bỏ qua để tránh N+1
                        .itemCount(0)
                        .build())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private CustomerMyDashboardResponse.SpinWheelStatus buildSpinStatus(
            Map<String, Object> spinMap, Customer customer) {

        boolean canSpin     = Boolean.TRUE.equals(spinMap.get("canSpin"));
        Object remaining    = spinMap.get("spinsRemaining");
        int spinsRemaining  = remaining instanceof Number n ? n.intValue() : 0;
        Object nextSpin     = spinMap.get("nextSpinAt");

        BigDecimal bonus = customer.getSpinDiscountBonus() != null
                ? customer.getSpinDiscountBonus() : BigDecimal.ZERO;

        return CustomerMyDashboardResponse.SpinWheelStatus.builder()
                .canSpin(canSpin)
                .spinsRemaining(spinsRemaining)
                .nextSpinAt(nextSpin != null ? nextSpin.toString() : null)
                .currentBonus(bonus)
                .hasBonus(bonus.compareTo(BigDecimal.ZERO) > 0)
                .build();
    }

    private CustomerMyDashboardResponse.VipJourney buildVipJourney(
            Customer customer, List<LoyaltyLedgerResponse> tierHistory) {

        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
        VipTier currentTier = customer.getVipTier();
        VipTier[] allTiers = VipTier.values(); // BRONZE, SILVER, GOLD, DIAMOND

        // Tier tiếp theo
        VipTier nextTier = null;
        for (int i = 0; i < allTiers.length; i++) {
            if (allTiers[i] == currentTier && i < allTiers.length - 1) {
                nextTier = allTiers[i + 1];
                break;
            }
            if (currentTier == null) {
                nextTier = allTiers[0]; // BRONZE
                break;
            }
        }

        // pointsToNext & progress
        int pointsToNext = 0;
        double progressPct = 100.0;

        if (nextTier != null) {
            int nextMin = nextTier.getMinPoints();
            int currentMin = currentTier != null ? currentTier.getMinPoints() : 0;
            pointsToNext = Math.max(0, nextMin - currentPoints);
            int range = nextMin - currentMin;
            int earned = currentPoints - currentMin;
            progressPct = range > 0 ? Math.min(100.0, (earned * 100.0 / range)) : 100.0;
            progressPct = Math.round(progressPct * 10) / 10.0;
        }

        // Milestones
        List<CustomerMyDashboardResponse.TierMilestone> milestones = new ArrayList<>();

        // Lấy thời điểm đạt tier từ tierHistory (nếu có)
        for (VipTier tier : allTiers) {
            boolean achieved = currentTier != null && (
                    tier == currentTier ||
                            tier.ordinal() < currentTier.ordinal()
            );

            // Tìm ngày đạt tier từ lịch sử
            String achievedAt = null;
            if (achieved && tierHistory != null) {
                achievedAt = tierHistory.stream()
                        .filter(h -> tier.name().equals(h.getTierAfter()))
                        .map(h -> h.getCreatedAt() != null
                                ? h.getCreatedAt().toString().substring(0, 10) : null)
                        .filter(d -> d != null)
                        .findFirst().orElse(null);
            }

            milestones.add(CustomerMyDashboardResponse.TierMilestone.builder()
                    .tier(tier.name())
                    .tierDisplay(tier.getDisplayName())
                    .requiredPoints(tier.getMinPoints())
                    .achieved(achieved)
                    .achievedAt(achievedAt)
                    .build());
        }

        // Tier trước
        String previousTier = null;
        if (currentTier != null && currentTier.ordinal() > 0) {
            previousTier = allTiers[currentTier.ordinal() - 1].name();
        }

        return CustomerMyDashboardResponse.VipJourney.builder()
                .currentTier(currentTier != null ? currentTier.name() : null)
                .currentTierDisplay(currentTier != null ? currentTier.getDisplayName() : "Member")
                .currentPoints(currentPoints)
                .previousTier(previousTier)
                .nextTier(nextTier != null ? nextTier.name() : null)
                .nextTierDisplay(nextTier != null ? nextTier.getDisplayName() : null)
                .pointsToNext(pointsToNext)
                .progressPercent(progressPct)
                .milestones(milestones)
                .build();
    }

    // ================================================================
    // UTIL
    // ================================================================

    /** Bọc các lời gọi có thể throw exception — trả về fallback thay vì crash toàn dashboard */
    private <T> T safeGet(java.util.concurrent.Callable<T> supplier, T fallback) {
        try {
            return supplier.call();
        } catch (Exception e) {
            log.warn("Dashboard section failed: {}", e.getMessage());
            return fallback;
        }
    }
}