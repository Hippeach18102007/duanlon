package com.retailmanagement.service;

import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tập trung xử lý thông báo cho khách hàng theo từng sự kiện.
 *
 * Các sự kiện được hỗ trợ:
 *  1. Đơn hàng được tạo              → onOrderCreated()
 *  2. Đơn hàng đã thanh toán         → onOrderPaid()
 *  3. Đơn hàng bị hủy               → onOrderCancelled()
 *  4. Đơn hàng đã giao               → onOrderDelivered()
 *  5. Cộng điểm loyalty              → onPointsEarned()
 *  6. Trừ điểm loyalty               → onPointsDeducted()
 *  7. Thăng hạng VIP                 → onTierUpgrade()
 *  8. Hạ hạng VIP                    → onTierDowngrade()
 *  9. Reset điểm cuối năm            → onYearEndReset()
 * 10. Hạ hạng do không hoạt động     → onMonthlyInactivityDemotion()
 * 11. Sinh nhật                      → (có sẵn trong NotificationService)
 * 12. Sắp lên hạng                   → onNearTierUpgrade()
 * 13. Đăng ký thành công             → onCustomerRegistered()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerEventNotificationService {

    private final NotificationService notificationService;

    @Value("${resend.api.key}")
    private String apiKey;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ================================================================
    // 1. ĐƠN HÀNG ĐƯỢC TẠO
    // ================================================================

    /**
     * Gọi từ OrderService sau khi tạo đơn thành công.
     */
    @Transactional
    public void onOrderCreated(Order order) {
        if (!hasCustomerEmail(order)) return;

        Customer customer = order.getCustomer();
        String title = "📦 Đơn hàng đã được tạo";
        String message = String.format(
                "Đơn hàng %s của bạn đã được tạo thành công.\n\n" +
                        "💰 Tổng tiền: %s VNĐ\n" +
                        "📅 Thời gian: %s\n\n" +
                        "Chúng tôi sẽ xử lý đơn hàng của bạn sớm nhất!",
                order.getOrderNumber(),
                formatMoney(order.getTotalAmount()),
                LocalDateTime.now().format(DATE_FMT)
        );

        // Lưu notification trong hệ thống
        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.ORDER_UPDATE,
                title,
                message
        );

        // Gửi email
        sendSimpleEmail(
                customer.getEmail(),
                "Xác nhận đơn hàng " + order.getOrderNumber(),
                buildEmailBody(customer.getName(), message)
        );

        log.info("✅ [ORDER_CREATED] Notified customer #{} for order {}",
                customer.getId(), order.getOrderNumber());
    }

    // ================================================================
    // 2. ĐƠN HÀNG ĐÃ THANH TOÁN
    // ================================================================

    /**
     * Gọi từ PaymentService sau khi thanh toán thành công.
     */
    @Transactional
    public void onOrderPaid(Order order, Payment payment) {
        if (!hasCustomer(order)) return;

        Customer customer = order.getCustomer();
        int estimatedPoints = order.getTotalAmount()
                .divide(BigDecimal.valueOf(10000)).intValue();

        String title = "✅ Thanh toán thành công";
        String message = String.format(
                "Đơn hàng %s đã được thanh toán thành công.\n\n" +
                        "💳 Phương thức: %s\n" +
                        "💰 Số tiền: %s VNĐ\n" +
                        "🎯 Điểm tích lũy: +%,d điểm\n" +
                        "📅 Thời gian: %s",
                order.getOrderNumber(),
                payment.getMethod(),
                formatMoney(payment.getAmount()),
                estimatedPoints,
                LocalDateTime.now().format(DATE_FMT)
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.ORDER_UPDATE,
                title,
                message
        );

        if (customer.getEmail() != null) {
            sendSimpleEmail(
                    customer.getEmail(),
                    "Thanh toán thành công - Đơn hàng " + order.getOrderNumber(),
                    buildEmailBody(customer.getName(), message)
            );
        }

        log.info("✅ [ORDER_PAID] Notified customer #{} for order {}",
                customer.getId(), order.getOrderNumber());
    }

    // ================================================================
    // 3. ĐƠN HÀNG BỊ HỦY
    // ================================================================

    /**
     * Gọi từ OrderService khi đơn bị hủy.
     * @param reason lý do hủy (ví dụ: "CANCEL_ORDER", "RETURN", hoặc mô tả tùy ý)
     */
    @Transactional
    public void onOrderCancelled(Order order, String reason) {
        if (!hasCustomer(order)) return;

        Customer customer = order.getCustomer();
        String title = "❌ Đơn hàng đã bị hủy";
        String message = String.format(
                "Đơn hàng %s đã bị hủy.\n\n" +
                        "📝 Lý do: %s\n" +
                        "💰 Tổng tiền: %s VNĐ\n" +
                        "📅 Thời gian: %s\n\n" +
                        "Nếu bạn đã thanh toán, tiền sẽ được hoàn lại trong 3-5 ngày làm việc.",
                order.getOrderNumber(),
                reason,
                formatMoney(order.getTotalAmount()),
                LocalDateTime.now().format(DATE_FMT)
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.ORDER_UPDATE,
                title,
                message
        );

        if (customer.getEmail() != null) {
            sendSimpleEmail(
                    customer.getEmail(),
                    "Đơn hàng " + order.getOrderNumber() + " đã bị hủy",
                    buildEmailBody(customer.getName(), message)
            );
        }

        log.info("✅ [ORDER_CANCELLED] Notified customer #{} for order {}",
                customer.getId(), order.getOrderNumber());
    }

    // ================================================================
    // 4. ĐƠN HÀNG ĐÃ GIAO
    // ================================================================

    /**
     * Gọi từ OrderService khi đơn chuyển sang trạng thái DELIVERED.
     */
    @Transactional
    public void onOrderDelivered(Order order) {
        if (!hasCustomer(order)) return;

        Customer customer = order.getCustomer();
        String title = "🎉 Đơn hàng đã được giao";
        String message = String.format(
                "Đơn hàng %s đã được giao thành công!\n\n" +
                        "📅 Thời gian giao: %s\n\n" +
                        "Cảm ơn bạn đã mua sắm tại cửa hàng chúng tôi! ❤️\n" +
                        "Hãy để lại đánh giá để giúp chúng tôi phục vụ bạn tốt hơn.",
                order.getOrderNumber(),
                LocalDateTime.now().format(DATE_FMT)
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.ORDER_UPDATE,
                title,
                message
        );

        if (customer.getEmail() != null) {
            sendSimpleEmail(
                    customer.getEmail(),
                    "Đơn hàng " + order.getOrderNumber() + " đã được giao thành công!",
                    buildEmailBody(customer.getName(), message)
            );
        }

        log.info("✅ [ORDER_DELIVERED] Notified customer #{} for order {}",
                customer.getId(), order.getOrderNumber());
    }

    // ================================================================
    // 5. CỘNG ĐIỂM LOYALTY
    // ================================================================

    /**
     * Gọi từ CustomerService sau khi cộng điểm thành công.
     */
    @Transactional
    public void onPointsEarned(Customer customer, int pointsEarned,
                               int newTotalPoints, BigDecimal fromOrderAmount) {
        String title = "🎯 Bạn vừa nhận được điểm thưởng!";
        String message = String.format(
                "Cộng %,d điểm từ đơn hàng %s VNĐ.\n\n" +
                        "📊 Tổng điểm hiện tại: %,d điểm\n" +
                        "🏆 Hạng thành viên: %s\n\n" +
                        "Tiếp tục mua sắm để tích thêm điểm và thăng hạng!",
                pointsEarned,
                formatMoney(fromOrderAmount),
                newTotalPoints,
                customer.getVipTier() != null
                        ? customer.getVipTier().getDisplayName()
                        : "Member"
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.LOYALTY_POINTS,
                title,
                message
        );

        log.info("✅ [POINTS_EARNED] Customer #{} earned {} points, total: {}",
                customer.getId(), pointsEarned, newTotalPoints);
    }

    // ================================================================
    // 6. TRỪ ĐIỂM LOYALTY
    // ================================================================

    /**
     * Gọi từ CustomerService sau khi trừ điểm.
     */
    @Transactional
    public void onPointsDeducted(Customer customer, int pointsDeducted,
                                 int newTotalPoints, String reason) {
        String title = "⚠️ Điểm tích lũy bị trừ";
        String reasonDisplay = switch (reason) {
            case "CANCEL_ORDER" -> "hủy đơn hàng";
            case "RETURN"       -> "trả hàng";
            default             -> reason.toLowerCase();
        };

        String message = String.format(
                "-%,d điểm do %s.\n\n" +
                        "📊 Điểm hiện tại: %,d điểm\n" +
                        "🏆 Hạng thành viên: %s",
                pointsDeducted,
                reasonDisplay,
                newTotalPoints,
                customer.getVipTier() != null
                        ? customer.getVipTier().getDisplayName()
                        : "Member"
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.LOYALTY_POINTS,
                title,
                message
        );

        log.info("✅ [POINTS_DEDUCTED] Customer #{} deducted {} points, total: {}",
                customer.getId(), pointsDeducted, newTotalPoints);
    }

    // ================================================================
    // 7. THĂNG HẠNG VIP
    // ================================================================

    /**
     * Gọi từ CustomerService khi phát hiện tier thay đổi lên cao hơn.
     */
    @Transactional
    public void onTierUpgrade(Customer customer, VipTier tierBefore, VipTier tierAfter,
                              int newTotalPoints) {
        String tierBeforeName = tierBefore != null ? tierBefore.getDisplayName() : "Member";
        String tierAfterName  = tierAfter  != null ? tierAfter.getDisplayName()  : "Member";

        String title = "🎉 Chúc mừng! Bạn đã thăng hạng " + tierAfterName + "!";
        String message = String.format(
                "🏆 Hạng thành viên: %s → %s\n" +
                        "📊 Điểm tích lũy: %,d điểm\n" +
                        "💎 Mức chiết khấu mới: %.0f%%\n\n" +
                        "Cảm ơn bạn đã tin tưởng và mua sắm tại cửa hàng!",
                tierBeforeName,
                tierAfterName,
                newTotalPoints,
                tierAfter != null ? tierAfter.getDiscountRate() * 100 : 0.0
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.VIP_TIER_UPGRADE,
                title,
                message
        );

        if (customer.getEmail() != null) {
            sendSimpleEmail(
                    customer.getEmail(),
                    "🎉 Bạn đã thăng hạng " + tierAfterName + "!",
                    buildEmailBody(customer.getName(), message)
            );
        }

        log.info("✅ [TIER_UPGRADE] Customer #{} upgraded: {} → {}",
                customer.getId(), tierBeforeName, tierAfterName);
    }

    // ================================================================
    // 8. HẠ HẠNG VIP
    // ================================================================

    /**
     * Gọi từ CustomerService / LoyaltyResetService khi tier bị hạ.
     * @param cause ví dụ: "CANCEL_ORDER", "YEAR_END_RESET", "MONTHLY_INACTIVITY"
     */
    @Transactional
    public void onTierDowngrade(Customer customer, VipTier tierBefore, VipTier tierAfter,
                                int newTotalPoints, String cause) {
        String tierBeforeName = tierBefore != null ? tierBefore.getDisplayName() : "Member";
        String tierAfterName  = tierAfter  != null ? tierAfter.getDisplayName()  : "Member";
        String causeDisplay = switch (cause) {
            case "CANCEL_ORDER"        -> "hủy đơn hàng";
            case "RETURN"              -> "trả hàng";
            case "YEAR_END_RESET"      -> "reset cuối năm";
            case "MONTHLY_INACTIVITY"  -> "không có giao dịch trong 30 ngày";
            default                    -> cause.toLowerCase();
        };

        String title = "📉 Hạng thành viên bị hạ xuống " + tierAfterName;
        String message = String.format(
                "Hạng của bạn đã thay đổi: %s → %s\n" +
                        "📝 Lý do: %s\n" +
                        "📊 Điểm hiện tại: %,d điểm\n\n" +
                        "Tiếp tục tích điểm để phục hồi hạng thành viên của bạn!",
                tierBeforeName,
                tierAfterName,
                causeDisplay,
                newTotalPoints
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.VIP_TIER_UPGRADE,  // dùng chung type, title phân biệt
                title,
                message
        );

        if (customer.getEmail() != null) {
            sendSimpleEmail(
                    customer.getEmail(),
                    "Thay đổi hạng thành viên - " + tierAfterName,
                    buildEmailBody(customer.getName(), message)
            );
        }

        log.info("✅ [TIER_DOWNGRADE] Customer #{} downgraded: {} → {} ({})",
                customer.getId(), tierBeforeName, tierAfterName, cause);
    }

    // ================================================================
    // 9. RESET ĐIỂM CUỐI NĂM
    // ================================================================

    /**
     * Gọi từ LoyaltyResetService trong processYearEndReset().
     */
    @Transactional
    public void onYearEndReset(Customer customer, VipTier tierBefore, VipTier tierAfter,
                               int pointsBefore, int pointsAfter) {
        String title = "📅 Thông báo reset điểm cuối năm";
        String message = String.format(
                "Hệ thống đã thực hiện reset điểm cuối năm.\n\n" +
                        "📊 Điểm: %,d → %,d\n" +
                        "🏆 Hạng: %s → %s\n\n" +
                        "Năm mới, hãy cùng nhau tích lũy điểm và thăng hạng! 🎊",
                pointsBefore,
                pointsAfter,
                tierBefore != null ? tierBefore.getDisplayName() : "Member",
                tierAfter  != null ? tierAfter.getDisplayName()  : "Member"
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.SYSTEM,
                title,
                message
        );

        if (customer.getEmail() != null) {
            sendSimpleEmail(
                    customer.getEmail(),
                    "Thông báo reset điểm thưởng cuối năm",
                    buildEmailBody(customer.getName(), message)
            );
        }

        log.info("✅ [YEAR_END_RESET] Notified customer #{}", customer.getId());
    }

    // ================================================================
    // 10. HẠ HẠNG DO KHÔNG HOẠT ĐỘNG (MONTHLY)
    // ================================================================

    /**
     * Gọi từ LoyaltyResetService trong applyMonthlyDemotion() — wrapper tiện lợi.
     */
    @Transactional
    public void onMonthlyInactivityDemotion(Customer customer, VipTier tierBefore,
                                            VipTier tierAfter, int newPoints) {
        onTierDowngrade(customer, tierBefore, tierAfter, newPoints, "MONTHLY_INACTIVITY");
    }

    // ================================================================
    // 11. SẮP LÊN HẠNG
    // ================================================================

    /**
     * Gọi thủ công hoặc từ scheduler để nhắc khách gần đạt hạng tiếp theo.
     */
    @Transactional
    public void onNearTierUpgrade(Customer customer, VipTier nextTier, int pointsGap) {
        String title = "🔥 Bạn sắp đạt hạng " + nextTier.getDisplayName() + "!";
        String message = String.format(
                "Chỉ còn %,d điểm nữa là bạn đạt hạng %s!\n\n" +
                        "💎 Quyền lợi hạng %s:\n" +
                        "   • Chiết khấu: %.0f%%\n\n" +
                        "Tiếp tục mua sắm để thăng hạng ngay hôm nay!",
                pointsGap,
                nextTier.getDisplayName(),
                nextTier.getDisplayName(),
                nextTier.getDiscountRate() * 100
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.VIP_TIER_UPGRADE,
                title,
                message
        );

        log.info("✅ [NEAR_UPGRADE] Customer #{} needs {} more points for {}",
                customer.getId(), pointsGap, nextTier.getDisplayName());
    }

    // ================================================================
    // 12. ĐĂNG KÝ THÀNH CÔNG
    // ================================================================

    /**
     * Gọi từ CustomerService sau khi tạo customer mới thành công.
     */
    @Transactional
    public void onCustomerRegistered(Customer customer) {
        String title = "🎉 Chào mừng bạn đến với cửa hàng!";
        String message = String.format(
                "Xin chào %s,\n\n" +
                        "Tài khoản của bạn đã được tạo thành công!\n\n" +
                        "🎯 Tích điểm ngay:\n" +
                        "   • Mỗi 10.000đ = 1 điểm\n" +
                        "   • 1.000 điểm → Hạng BRONZE\n" +
                        "   • 3.000 điểm → Hạng SILVER\n" +
                        "   • 5.000 điểm → Hạng GOLD (VIP)\n\n" +
                        "Chúc bạn mua sắm vui vẻ! ❤️",
                customer.getName()
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.WELCOME,
                title,
                message
        );

        if (customer.getEmail() != null) {
            sendSimpleEmail(
                    customer.getEmail(),
                    "Chào mừng bạn đến với cửa hàng!",
                    buildEmailBody(customer.getName(), message)
            );
        }

        log.info("✅ [REGISTERED] Welcomed new customer #{} - {}",
                customer.getId(), customer.getName());
    }

    // ================================================================
    // 13. HOÀN TIỀN (REFUND)
    // ================================================================

    /**
     * Gọi từ PaymentService sau khi refund thành công.
     */
    @Transactional
    public void onPaymentRefunded(Order order, BigDecimal refundAmount) {
        if (!hasCustomer(order)) return;

        Customer customer = order.getCustomer();
        String title = "💸 Hoàn tiền thành công";
        String message = String.format(
                "Đơn hàng %s đã được hoàn tiền.\n\n" +
                        "💰 Số tiền hoàn: %s VNĐ\n" +
                        "📅 Thời gian: %s\n\n" +
                        "Tiền sẽ được trả về tài khoản của bạn trong 3-5 ngày làm việc.",
                order.getOrderNumber(),
                formatMoney(refundAmount),
                LocalDateTime.now().format(DATE_FMT)
        );

        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.ORDER_UPDATE,
                title,
                message
        );

        if (customer.getEmail() != null) {
            sendSimpleEmail(
                    customer.getEmail(),
                    "Hoàn tiền đơn hàng " + order.getOrderNumber(),
                    buildEmailBody(customer.getName(), message)
            );
        }

        log.info("✅ [REFUNDED] Notified customer #{} for order {}",
                customer.getId(), order.getOrderNumber());
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private boolean hasCustomer(Order order) {
        return order.getCustomer() != null;
    }

    private boolean hasCustomerEmail(Order order) {
        return order.getCustomer() != null && order.getCustomer().getEmail() != null;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,d", amount.longValue());
    }

    private String buildEmailBody(String customerName, String content) {
        return String.format(
                "Xin chào %s,\n\n%s\n\n" +
                        "---\n" +
                        "Email này được gửi tự động từ hệ thống.\n" +
                        "Vui lòng không reply email này.",
                customerName, content
        );
    }

    private void sendSimpleEmail(String to, String subject, String body) {

        try {

            Resend resend = new Resend(apiKey);

            CreateEmailOptions params =
                    CreateEmailOptions.builder()
                            .from("BonBon Coffee <onboarding@resend.dev>")
                            .to(to)
                            .subject(subject)
                            .text(body)
                            .build();

            resend.emails().send(params);

            log.info("📧 Email sent to {}: {}", to, subject);

        } catch (Exception e) {

            log.warn("⚠️ Failed to send email to {}: {}", to, e.getMessage());

        }
    }
}