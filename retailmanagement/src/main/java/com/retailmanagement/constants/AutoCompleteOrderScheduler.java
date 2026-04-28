package com.retailmanagement.constants;

import com.retailmanagement.audit.AuditAction;
import com.retailmanagement.audit.AuditLogService;
import com.retailmanagement.audit.AuditModule;
import com.retailmanagement.audit.TargetType;
import com.retailmanagement.constants.OrderStatuses;
import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import com.retailmanagement.service.OrderEmailService;
import com.retailmanagement.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduler tự động hoàn tất đơn hàng ở trạng thái SHIPPING
 * sau N ngày (mặc định 3) mà khách không xác nhận.
 *
 * <p>Job chính chạy lúc 00:00 mỗi đêm.</p>
 * <p>Job cảnh báo chạy lúc 09:00 để báo trước 1 ngày.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoCompleteOrderScheduler {

    // ── Settings key ──────────────────────────────────────────────────────────
    private static final String SETTING_AUTO_COMPLETE_DAYS = "AUTO_COMPLETE_DAYS";
    private static final int    DEFAULT_AUTO_COMPLETE_DAYS = 3;

    // ── Points rate: 1 điểm / 10,000 VND ─────────────────────────────────────
    private static final String  SETTING_LOYALTY_RATE   = "LOYALTY_POINTS_RATE";
    private static final int     DEFAULT_LOYALTY_RATE   = 10_000;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final OrderRepository         orderRepository;
    private final PaymentRepository       paymentRepository;
    private final NotificationRepository  notificationRepository;
    private final LoyaltyLedgerRepository loyaltyLedgerRepository;
    private final CustomRes      customerRepository;
    private final AuditLogService         auditLogService;
    private final OrderEmailService       orderEmailService;
    private final SettingService          settingService;

    // =========================================================================
    // JOB 1 — Cảnh báo trước 1 ngày (09:00 sáng)
    // =========================================================================

    /**
     * Gửi mail cảnh báo cho các đơn SHIPPING sắp bị auto-complete sau 1 ngày.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendAutoCompleteWarningEmails() {
        int days = getAutoCompleteDays();
        Instant warningCutoff = Instant.now().minusSeconds((long) (days - 1) * 86_400);
        Instant warningUpperBound = Instant.now().minusSeconds((long) (days - 2) * 86_400);

        log.info("══════════════════════════════════════════════");
        log.info("⚠️  AUTO-COMPLETE WARNING JOB — {}",
                LocalDateTime.now().format(FORMATTER));
        log.info("   Tìm đơn SHIPPING cập nhật trong [{} - {} ngày] trước",
                days - 1, days - 2);
        log.info("══════════════════════════════════════════════");

        try {
            List<Order> orders = orderRepository
                    .findShippingOrdersToWarn(warningCutoff, warningUpperBound);

            log.info("   Số đơn cần cảnh báo: {}", orders.size());

            for (Order order : orders) {
                try {
                    orderEmailService.sendAutoCompleteWarningEmail(order, days);
                    log.info("   📧 Gửi cảnh báo → đơn #{}", order.getOrderNumber());
                } catch (Exception e) {
                    log.warn("   ❌ Lỗi gửi cảnh báo đơn #{}: {}",
                            order.getOrderNumber(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("❌ AutoCompleteWarningJob FAILED: {}", e.getMessage(), e);
        }

        log.info("══════════════════════════════════════════════\n");
    }

    // =========================================================================
    // JOB 2 — Auto-complete đơn quá hạn (00:00 đêm)
    // =========================================================================

    /**
     * Tự động chuyển đơn SHIPPING → DELIVERED sau N ngày.
     *
     * <p>Mỗi đơn được xử lý trong transaction riêng biệt ({@link #processOrder})
     * để một đơn lỗi không rollback toàn bộ batch.</p>
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void autoCompleteShippingOrders() {
        int days = getAutoCompleteDays();
        Instant cutoff = Instant.now().minusSeconds((long) days * 86_400);

        log.info("══════════════════════════════════════════════");
        log.info("🚀 AUTO-COMPLETE ORDER JOB — {}",
                LocalDateTime.now().format(FORMATTER));
        log.info("   Ngưỡng: {} ngày — cutoff = {}", days, cutoff);
        log.info("══════════════════════════════════════════════");

        List<Order> candidates = orderRepository
                .findShippingOrdersOverdue(OrderStatuses.SHIPPING, cutoff);

        log.info("   Tổng đơn cần xử lý: {}", candidates.size());

        int success = 0, failed = 0;

        for (Order order : candidates) {
            try {
                processOrder(order.getId());
                success++;
                log.info("   ✅ Hoàn tất đơn #{} (id={})",
                        order.getOrderNumber(), order.getId());
            } catch (Exception e) {
                failed++;
                log.error("   ❌ Lỗi đơn #{} (id={}): {}",
                        order.getOrderNumber(), order.getId(), e.getMessage(), e);
            }
        }

        log.info("══════════════════════════════════════════════");
        log.info("   Kết quả — Thành công: {} | Thất bại: {}", success, failed);
        log.info("══════════════════════════════════════════════\n");
    }

    // =========================================================================
    // CORE LOGIC — mỗi đơn trong transaction độc lập
    // =========================================================================

    /**
     * Xử lý một đơn hàng: cập nhật trạng thái, thanh toán COD,
     * loyalty, notification, audit, email.
     *
     * <p>Dùng {@code @Transactional} riêng để lỗi của 1 đơn
     * không ảnh hưởng đơn khác trong batch.</p>
     */
    @Transactional
    public void processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy đơn id=" + orderId));

        Instant now = Instant.now();

        // 1. Cập nhật trạng thái đơn → DELIVERED
        order.setStatus(OrderStatuses.DELIVERED);
        order.setDeliveredAt(now);
        orderRepository.save(order);

        // 2. Tạo payment COD nếu chưa thanh toán
        if ("CASH".equalsIgnoreCase(order.getPaymentMethod())
                && "UNPAID".equalsIgnoreCase(order.getPaymentStatus())) {

            Payment payment = Payment.builder()
                    .order(order)
                    .amount(order.getTotalAmount())
                    .method("CASH")
                    .transactionRef("AUTO-COD-" + order.getId())
                    .status("SUCCESS")
                    .paidAt(now)
                    .createdAt(now)
                    .build();

            paymentRepository.save(payment);

            order.setPaymentStatus("PAID");
            order.setPaidAt(now);
            orderRepository.save(order);

            log.debug("   💰 Tạo payment COD cho đơn #{}", order.getOrderNumber());
        }

        // 3. Cộng điểm loyalty
        earnLoyaltyPoints(order, now);

        // 4. Ghi audit log
        String details = String.format(
                "{\"orderId\":%d,\"orderNumber\":\"%s\",\"reason\":\"Khach hang khong xac nhan sau %d ngay\"}",
                order.getId(), order.getOrderNumber(), getAutoCompleteDays());

        auditLogService.save(
                null,                      // system action — không có userId
                AuditModule.ORDER,
                AuditAction.AUTO_COMPLETE,
                TargetType.ORDER,
                order.getId(),
                details,
                "SYSTEM"
        );

        // 5. Gửi notification cho khách
        if (order.getCustomer() != null) {
            sendAutoCompleteNotification(order);
        }

        // 6. Gửi email xác nhận giao hàng (async — không block)
        try {
            orderEmailService.sendDeliveredEmail(order);
        } catch (Exception e) {
            // Email lỗi không làm hỏng transaction chính
            log.warn("   ⚠️ Gửi email thất bại cho đơn #{}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Cộng điểm loyalty cho khách hàng dựa trên tổng tiền đơn.
     * Công thức: 1 điểm / {@code LOYALTY_POINTS_RATE} VND.
     *
     * <p>Bỏ qua nếu đơn không có khách hàng hoặc tổng tiền = 0.</p>
     */
    private void earnLoyaltyPoints(Order order, Instant now) {
        Customer customer = order.getCustomer();
        if (customer == null) return;
        if (order.getTotalAmount() == null
                || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) return;

        int rate = getLoyaltyRate();
        int points = order.getTotalAmount()
                .divideToIntegralValue(BigDecimal.valueOf(rate))
                .intValue();

        if (points <= 0) return;

        // Cập nhật tổng điểm trên Customer
        int currentPoints = customer.getLoyaltyPoints() != null
                ? customer.getLoyaltyPoints() : 0;
        customer.setLoyaltyPoints(currentPoints + points);
        customerRepository.save(customer);

        // Ghi ledger entry
        LoyaltyLedger ledger = new LoyaltyLedger();
        ledger.setCustomer(customer);
        ledger.setPointsDelta(points);
        ledger.setTransactionType("EARN");
        ledger.setReferenceType("ORDER");
        ledger.setReferenceId(order.getId());
        ledger.setReason("Tich diem don hang #" + order.getOrderNumber() + " (auto-complete)");
        ledger.setCreatedAt(now);
        loyaltyLedgerRepository.save(ledger);

        log.debug("   🎯 Cộng {} điểm cho khách {} (đơn #{})",
                points, customer.getName(), order.getOrderNumber());
    }

    /**
     * Tạo notification in-app cho khách hàng.
     */
    private void sendAutoCompleteNotification(Order order) {
        try {
            Notification notification = Notification.builder()
                    .customer(order.getCustomer())
                    .type(NotificationType.ORDER_AUTO_COMPLETED)
                    .title("Đơn hàng đã được giao thành công")
                    .message(String.format(
                            "Đơn hàng #%s của bạn đã được xác nhận giao thành công tự động. "
                                    + "Cảm ơn bạn đã mua sắm tại TechStore!",
                            order.getOrderNumber()))
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("   ⚠️ Lỗi tạo notification cho đơn #{}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    /** Đọc số ngày auto-complete từ settings (fallback = 3). */
    private int getAutoCompleteDays() {
        try {
            String val = settingService.getSetting(
                    SETTING_AUTO_COMPLETE_DAYS,
                    String.valueOf(DEFAULT_AUTO_COMPLETE_DAYS));
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return DEFAULT_AUTO_COMPLETE_DAYS;
        }
    }

    /** Đọc loyalty rate từ settings (fallback = 10_000 VND/điểm). */
    private int getLoyaltyRate() {
        try {
            String val = settingService.getSetting(
                    SETTING_LOYALTY_RATE,
                    String.valueOf(DEFAULT_LOYALTY_RATE));
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return DEFAULT_LOYALTY_RATE;
        }
    }
}