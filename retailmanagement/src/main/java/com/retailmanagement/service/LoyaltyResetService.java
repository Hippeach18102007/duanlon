package com.retailmanagement.service;

import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyResetService {

    private final CustomRes customerRepository;
    private final OrderRepository orderRepository;
    private final LoyaltyLedgerRepository loyaltyLedgerRepository;
    private final CustomerEventNotificationService eventNotificationService; // ✅ THÊM

    // ================================================================
    // TIER ORDER: BRONZE(0) → SILVER(1) → GOLD(2) → PLATINUM(3) → DIAMOND(4)
    // ================================================================
    private static final List<VipTier> TIER_ORDER = List.of(
            VipTier.BRONZE,
            VipTier.SILVER,
            VipTier.GOLD,
            VipTier.PLATINUM,
            VipTier.DIAMOND
    );

    // ================================================================
    // 1. RESET CUỐI NĂM — chạy lúc 00:00 ngày 31/12 hàng năm
    // ================================================================
    @Scheduled(cron = "0 0 0 31 12 *")
    public void yearEndReset() {
        log.info("🎯 [YEAR-END RESET] Bắt đầu reset cuối năm 31/12...");
        processYearEndReset();
        log.info("✅ [YEAR-END RESET] Hoàn thành.");
    }

    // ================================================================
    // 2. CHECK HÀNG THÁNG — chạy lúc 00:00 ngày 1 mỗi tháng (tháng 1-6)
    // ================================================================
    @Scheduled(cron = "0 0 0 1 * *")
    public void monthlyInactivityCheck() {
        log.info("📅 [MONTHLY CHECK] Kiểm tra inactivity tháng {}...",
                LocalDateTime.now().getMonthValue());
        processMonthlyInactivityCheck();
        log.info("✅ [MONTHLY CHECK] Hoàn thành.");
    }

    // ================================================================
    // YEAR-END RESET LOGIC
    // ================================================================
    @Transactional
    public void processYearEndReset() {
        List<Customer> vipCustomers = customerRepository.findAll()
                .stream()
                .filter(c -> c.getVipTier() != null && Boolean.TRUE.equals(c.getIsActive()))
                .toList();

        log.info("📋 Tìm thấy {} customer có VIP tier", vipCustomers.size());

        for (Customer customer : vipCustomers) {
            applyYearEndReset(customer);
        }
    }

    // ================================================================
    // MONTHLY INACTIVITY CHECK LOGIC
    // ================================================================
    @Transactional
    public void processMonthlyInactivityCheck() {
        List<Customer> vipCustomers = customerRepository.findAll()
                .stream()
                .filter(c -> c.getVipTier() != null && Boolean.TRUE.equals(c.getIsActive()))
                .toList();

        for (Customer customer : vipCustomers) {
            Instant cutoff = LocalDateTime.now()
                    .minusDays(30)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();

            long orderCount = orderRepository.countByCustomerIdAndCreatedAtAfterAndStatusIn(
                    customer.getId(),
                    cutoff,
                    List.of("PAID", "PROCESSING", "SHIPPING", "DELIVERED")
            );

            log.info("Customer #{} {} | Đơn 30 ngày qua: {}",
                    customer.getId(), customer.getVipTier(), orderCount);

            if (orderCount == 0) {
                applyMonthlyDemotion(customer);
            }
        }
    }

    // ================================================================
    // APPLY YEAR-END RESET
    // DIAMOND  → PLATINUM │ điểm = 10000
    // PLATINUM → GOLD     │ điểm = 5000
    // GOLD     → SILVER   │ điểm = 2000 + mất VIP
    // SILVER   → BRONZE   │ điểm = 1000
    // BRONZE   → BRONZE   │ điểm = 1000
    // ================================================================
    private void applyYearEndReset(Customer customer) {
        VipTier tierBefore   = customer.getVipTier();
        int     pointsBefore = customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints();

        VipTier tierAfter = demoteTier(tierBefore);
        int     newPoints = getMinPointsForTier(tierAfter);

        customer.setLoyaltyPoints(newPoints);
        customer.setVipTier(tierAfter);
        updateCustomerType(customer, tierAfter);
        customerRepository.save(customer);

        log.info("🔄 [YEAR-END] Customer #{} | {} → {} | Điểm: {} → {}",
                customer.getId(), tierBefore, tierAfter, pointsBefore, newPoints);

        saveLedger(
                customer,
                newPoints - pointsBefore,
                "PENALTY",
                tierBefore, tierAfter,
                "YEAR_END_RESET",
                String.format("Reset cuối năm: điểm %d → %d | %s → %s",
                        pointsBefore, newPoints,
                        tierBefore.getDisplayName(),
                        tierAfter.getDisplayName())
        );

        if (tierBefore != tierAfter) {
            saveLedger(
                    customer, 0, "TIER_DOWNGRADE",
                    tierBefore, tierAfter,
                    "YEAR_END_RESET",
                    String.format("Hạ hạng cuối năm: %s → %s",
                            tierBefore.getDisplayName(), tierAfter.getDisplayName())
            );
        }

        // ✅ THÊM: thông báo reset cuối năm
        eventNotificationService.onYearEndReset(
                customer, tierBefore, tierAfter, pointsBefore, newPoints);
    }

    // ================================================================
    // APPLY MONTHLY DEMOTION
    // ================================================================
    private void applyMonthlyDemotion(Customer customer) {
        VipTier tierBefore   = customer.getVipTier();
        int     pointsBefore = customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints();

        if (tierBefore == VipTier.BRONZE) {
            log.info("⏸ Customer #{} đang BRONZE, dừng hạ rank", customer.getId());
            return;
        }

        VipTier tierAfter = demoteTier(tierBefore);
        int     newPoints = getMinPointsForTier(tierAfter);

        customer.setLoyaltyPoints(newPoints);
        customer.setVipTier(tierAfter);
        updateCustomerType(customer, tierAfter);
        customerRepository.save(customer);

        log.info("⬇️ [MONTHLY] Customer #{} | {} → {} | Điểm: {} → {}",
                customer.getId(), tierBefore, tierAfter, pointsBefore, newPoints);

        saveLedger(
                customer,
                newPoints - pointsBefore,
                "PENALTY",
                tierBefore, tierAfter,
                "MONTHLY_INACTIVITY",
                String.format("Không có GD trong 30 ngày: điểm %d → %d | %s → %s",
                        pointsBefore, newPoints,
                        tierBefore.getDisplayName(), tierAfter.getDisplayName())
        );

        if (tierBefore != tierAfter) {
            saveLedger(
                    customer, 0, "TIER_DOWNGRADE",
                    tierBefore, tierAfter,
                    "MONTHLY_INACTIVITY",
                    String.format("Hạ hạng do không có GD: %s → %s",
                            tierBefore.getDisplayName(), tierAfter.getDisplayName())
            );
        }

        // ✅ THÊM: thông báo hạ hạng do không hoạt động
        eventNotificationService.onMonthlyInactivityDemotion(
                customer, tierBefore, tierAfter, newPoints);
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private VipTier demoteTier(VipTier current) {
        if (current == null) return null;
        int index = TIER_ORDER.indexOf(current);
        if (index <= 0) return VipTier.BRONZE;
        return TIER_ORDER.get(index - 1);
    }

    private int getMinPointsForTier(VipTier tier) {
        if (tier == null) return 0;
        return tier.getMinPoints();
    }

    private void updateCustomerType(Customer customer, VipTier tier) {
        if (tier == null || tier == VipTier.BRONZE || tier == VipTier.SILVER) {
            customer.setCustomerType(CustomerType.REGULAR);
        } else {
            customer.setCustomerType(CustomerType.VIP);
        }
    }

    private void saveLedger(Customer customer, int pointsDelta, String transactionType,
                            VipTier tierBefore, VipTier tierAfter, String reason, String note) {
        LoyaltyLedger ledger = LoyaltyLedger.builder()
                .customer(customer)
                .pointsDelta(pointsDelta)
                .transactionType(transactionType)
                .tierBefore(tierBefore != null ? tierBefore.name() : null)
                .tierAfter(tierAfter != null ? tierAfter.name() : null)
                .reason(reason)
                .note(note)
                .referenceType("system")
                .referenceId(null)
                .createdBy(null)
                .createdAt(Instant.now())
                .build();

        loyaltyLedgerRepository.save(ledger);
    }

    // ================================================================
    // MANUAL TRIGGER — gọi từ Controller
    // ================================================================
    @Transactional
    public void triggerYearEndReset() {
        log.info("🔧 [MANUAL] Trigger year-end reset...");
        processYearEndReset();
    }

    @Transactional
    public void triggerMonthlyCheck() {
        log.info("🔧 [MANUAL] Trigger monthly inactivity check...");
        processMonthlyInactivityCheck();
    }
}