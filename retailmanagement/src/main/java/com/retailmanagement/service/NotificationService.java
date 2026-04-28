package com.retailmanagement.service;

import com.retailmanagement.dto.request.SendNotificationRequest;
import com.retailmanagement.entity.*;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.repository.NotificationRepository;
import com.retailmanagement.repository.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.Attachment;
import org.springframework.beans.factory.annotation.Value;


import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomRes customerRepository;
    private final SpinWheelService spinWheelService;
    private final PromotionRepository promotionRepository;
    private final PromotionRedemptionRepository promotionRedemptionRepository;

    @Value("${resend.api.key}")
    private String apiKey;

    private static final String BIRTHDAY_VOUCHER_CODE = "BIRTHDAY250K";
    private static final BigDecimal BIRTHDAY_DISCOUNT_AMOUNT = new BigDecimal("250000");
    private static final BigDecimal BIRTHDAY_MIN_ORDER = new BigDecimal("1000000");
    private static final int BIRTHDAY_VALID_DAYS = 7;

    public NotificationService(
            NotificationRepository notificationRepository,
            CustomRes customerRepository,
            @Lazy SpinWheelService spinWheelService,
            PromotionRepository promotionRepository,
            PromotionRedemptionRepository promotionRedemptionRepository) {
        this.notificationRepository = notificationRepository;
        this.customerRepository = customerRepository;
        this.spinWheelService = spinWheelService;
        this.promotionRepository = promotionRepository;
        this.promotionRedemptionRepository = promotionRedemptionRepository;
    }

    // =========================================================
    // GENERIC CREATE
    // =========================================================

    @Transactional
    public void createAndSaveNotification(
            Integer customerId,
            NotificationType type,
            String title,
            String message) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + customerId));

        Notification notification = Notification.builder()
                .customer(customer)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    // =========================================================
    // CUSTOMER API
    // =========================================================

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications(Integer customerId) {
        return notificationRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Integer customerId) {
        return notificationRepository
                .findByCustomerIdAndIsReadFalseOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public long countUnread(Integer customerId) {
        return notificationRepository
                .countByCustomerIdAndIsReadFalse(customerId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Integer customerId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy notification: " + notificationId));

        if (!notification.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Không có quyền truy cập notification này");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(Integer customerId) {

        List<Notification> notifications = notificationRepository
                .findByCustomerIdAndIsReadFalseOrderByCreatedAtDesc(customerId);

        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }

        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Integer customerId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy notification: " + notificationId));

        if (!notification.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Không có quyền xoá notification này");
        }

        notificationRepository.delete(notification);
    }

    // =========================================================
    // ADMIN - PURCHASE REMINDER HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<Notification> getPurchaseReminderHistory() {
        return notificationRepository
                .findByTypeOrderByCreatedAtDesc(NotificationType.PURCHASE_REMINDER);
    }

    // =========================================================
    // ADMIN - MANUAL SEND
    // =========================================================

    @Transactional
    public Map<String, Object> sendToCustomers(SendNotificationRequest request) {

        List<Integer> customerIds = request.getCustomerIds();

        NotificationType type;
        try {
            type = request.getType() != null
                    ? NotificationType.valueOf(request.getType())
                    : NotificationType.SYSTEM;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Loại notification không hợp lệ: " + request.getType());
        }

        int success = 0;
        int failed = 0;

        for (Integer customerId : customerIds) {
            try {
                createAndSaveNotification(
                        customerId,
                        type,
                        request.getTitle(),
                        request.getMessage());
                success++;
            } catch (Exception ex) {
                failed++;
            }
        }

        return Map.of(
                "total", customerIds.size(),
                "success", success,
                "failed", failed,
                "type", type.name());
    }

    // =========================================================
    // BIRTHDAY LOGIC
    // =========================================================

    private Promotion ensureBirthdayVoucherExists() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(BIRTHDAY_VALID_DAYS);

        Optional<Promotion> existing = promotionRepository.findByCode(BIRTHDAY_VOUCHER_CODE);

        if (existing.isPresent()) {
            Promotion p = existing.get();
            if (p.getIsActive() && p.getEndDate().isAfter(now)) {
                return p;
            }
            p.setStartDate(now);
            p.setEndDate(endDate);
            p.setIsActive(true);
            return promotionRepository.save(p);
        }

        Promotion voucher = Promotion.builder()
                .code(BIRTHDAY_VOUCHER_CODE)
                .name("🎂 Voucher sinh nhật — Giảm 250.000đ")
                .description("Giảm 250.000đ cho đơn từ 1.000.000đ. Hiệu lực 7 ngày.")
                .discountType("AMOUNT")
                .discountValue(BIRTHDAY_DISCOUNT_AMOUNT)
                .minOrderAmount(BIRTHDAY_MIN_ORDER)
                .priority(10)
                .stackable(false)
                .startDate(now)
                .endDate(endDate)
                .isActive(true)
                .createdAt(now)
                .build();

        Promotion saved = promotionRepository.save(voucher);

        PromotionRedemption redemption = new PromotionRedemption();
        redemption.setPromotionId(saved.getId());
        redemption.setUsedCount(0L);
        redemption.setUpdatedAt(now);
        promotionRedemptionRepository.save(redemption);

        return saved;
    }

    @Transactional
    public void sendBirthdayNotifications() {

        List<Customer> customers = customerRepository.findCustomersWithBirthdayToday();

        for (Customer customer : customers) {
            if (!customer.getIsActive())
                continue;

            LocalDateTime yearStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
            LocalDateTime yearEnd = yearStart.plusYears(1);

            boolean exists = notificationRepository
                    .existsByCustomerIdAndTypeAndCreatedAtBetween(
                            customer.getId(),
                            NotificationType.BIRTHDAY,
                            yearStart,
                            yearEnd);

            if (exists)
                continue;

            Promotion voucher = ensureBirthdayVoucherExists();

            String expiryDisplay = voucher.getEndDate()
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            String message = String.format(
                    "🎉 Sinh nhật vui vẻ %s!\n\n" +
                            "🎁 Mã: %s\n" +
                            "💰 Giảm: 250.000đ\n" +
                            "🛒 Đơn tối thiểu: 1.000.000đ\n" +
                            "⏳ Hết hạn: %s",
                    customer.getName(),
                    BIRTHDAY_VOUCHER_CODE,
                    expiryDisplay);

            createAndSaveNotification(
                    customer.getId(),
                    NotificationType.BIRTHDAY,
                    "🎂 Chúc mừng sinh nhật!",
                    message);
        }
    }

    @Transactional
    public void createTierUpgradeNotification(
            Integer customerId,
            String nextTierName,
            int pointsGap) {

        String message = String.format(
                "🔥 Bạn chỉ còn thiếu %,d điểm để đạt hạng %s!\n\n" +
                        "Tiếp tục mua sắm để thăng hạng và nhận ưu đãi hấp dẫn!",
                pointsGap,
                nextTierName);

        createAndSaveNotification(
                customerId,
                NotificationType.VIP_TIER_UPGRADE,
                "🎯 Bạn sắp lên hạng!",
                message);
    }

    @Transactional
    public void createUpgradeToVipNotification(
            Integer customerId,
            int pointsGap) {

        String message = String.format(
                "👑 Bạn chỉ còn thiếu %,d điểm để trở thành khách hàng VIP!\n\n" +
                        "Đạt 5.000 điểm để mở khóa quyền lợi VIP đặc biệt!",
                pointsGap);

        createAndSaveNotification(
                customerId,
                NotificationType.VIP_TIER_UPGRADE,
                "👑 Sắp trở thành khách hàng VIP!",
                message);
    }

    // =========================================================
    // PURCHASE REMINDER
    // =========================================================

    @Transactional
    public void sendPurchaseReminders() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upperBound = now.minusDays(30);
        LocalDateTime lowerBound = now.minusDays(60);
        LocalDateTime recentCutoff = now.minusDays(7);

        List<Customer> customers = customerRepository
                .findCustomersForPurchaseReminder(
                        upperBound,
                        lowerBound,
                        recentCutoff);

        for (Customer customer : customers) {
            createAndSaveNotification(
                    customer.getId(),
                    NotificationType.PURCHASE_REMINDER,
                    "🛒 Bạn đã lâu chưa mua sắm!",
                    "Chúng tôi nhớ bạn! Quay lại mua sắm để nhận thêm ưu đãi hấp dẫn 🎁");
        }
    }

    // =========================================================
    // SPIN EXPIRY
    // =========================================================

    @Transactional
    public void sendSpinExpiryWarnings() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(24);

        List<SpinWheelHistory> expiringBonuses = notificationRepository.findExpiringUnusedBonuses(now,
                threshold);

        for (SpinWheelHistory spin : expiringBonuses) {

            Customer customer = spin.getCustomer();
            if (!customer.getIsActive())
                continue;

            createAndSaveNotification(
                    customer.getId(),
                    NotificationType.SPIN_EXPIRY_WARNING,
                    "🎡 Ưu đãi vòng quay sắp hết hạn!",
                    String.format(
                            "Bonus giảm %s%% của bạn sẽ hết hạn vào %s.",
                            spin.getDiscountBonus(),
                            spin.getExpiresAt().toLocalDate()));
        }
    }

    // =========================================================
    // WELCOME ZERO ORDER
    // =========================================================

    @Transactional
    public void sendWelcomeToZeroOrderCustomers() {

        LocalDateTime registeredBefore = LocalDateTime.now().minusDays(7);
        List<Customer> customers = customerRepository.findZeroOrderCustomers(registeredBefore);

        for (Customer customer : customers) {

            boolean alreadySent = notificationRepository
                    .existsByCustomerIdAndTypeAndCreatedAtAfter(
                            customer.getId(),
                            NotificationType.WELCOME,
                            LocalDateTime.now().minusDays(30));

            if (alreadySent)
                continue;

            createAndSaveNotification(
                    customer.getId(),
                    NotificationType.WELCOME,
                    "🎉 Chào mừng bạn đến với cửa hàng!",
                    "Bạn đã đăng ký nhưng chưa mua hàng. Nhận ngay ưu đãi đặc biệt cho đơn đầu tiên!");
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getBirthdayNotificationHistory(LocalDateTime from, LocalDateTime to) {
        return notificationRepository
                .findByTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                        NotificationType.BIRTHDAY, from, to);
    }

    @Transactional(readOnly = true)
    public List<Notification> getBirthdayNotificationHistory() {
        return notificationRepository
                .findByTypeOrderByCreatedAtDesc(NotificationType.BIRTHDAY);
    }

    // =========================================================
    // ADMIN - SEND CUSTOM BIRTHDAY GREETING
    // =========================================================

    @Transactional
    public void sendCustomBirthdayGreeting(Integer customerId, String message) {
        createAndSaveNotification(
                customerId,
                NotificationType.BIRTHDAY,
                "🎂 Lời chúc sinh nhật từ cửa hàng",
                message);
    }

    // =========================================================
    // ADMIN - CHECK AND SEND TIER UPGRADE NOTIFICATIONS
    // =========================================================

    @Transactional
    public void checkAndSendTierUpgradeNotifications() {

        List<Customer> customers = customerRepository.findAll();

        for (Customer customer : customers) {
            if (!customer.getIsActive())
                continue;

            Integer points = customer.getLoyaltyPoints();
            if (points == null)
                continue;

            // Chưa đạt VIP (< 1000 điểm)
            if (points < 1000) {
                int pointsGap = 1000 - points;
                // Chỉ nhắc khi còn thiếu <= 200 điểm
                if (pointsGap <= 200) {
                    createUpgradeToVipNotification(customer.getId(), pointsGap);
                }
                continue;
            }

            VipTier currentTier = VipTier.fromPoints(points);
            if (currentTier == null)
                continue;

            // Đã đạt hạng cao nhất
            Integer nextThreshold = currentTier.getNextTierThreshold();
            if (nextThreshold == null)
                continue;

            int pointsGap = nextThreshold - points;

            // Chỉ nhắc khi còn thiếu <= 20% ngưỡng của tier tiếp theo
            VipTier[] tiers = VipTier.values();
            VipTier nextTier = tiers[currentTier.ordinal() + 1];
            int tierRange = nextTier.getMinPoints() - currentTier.getMinPoints();
            int threshold = (int) (tierRange * 0.2);

            if (pointsGap <= threshold) {
                createTierUpgradeNotification(customer.getId(), nextTier.getDisplayName(), pointsGap);
            }
        }
    }

    // =========================================================
    // EMAIL
    // =========================================================

    // =========================================================
// EMAIL
// =========================================================

    public void sendOrderDeliveredEmail(
            String email,
            Order order,
            byte[] pdf) {

        String subject = "Đơn hàng đã giao thành công";

        String content = """
            Xin chào %s,

            Đơn hàng %s đã được giao thành công.

            Cảm ơn bạn đã mua hàng!
            """.formatted(
                order.getCustomer().getName(),
                order.getOrderNumber());

        sendEmailWithAttachment(
                email,
                subject,
                content,
                pdf,
                "invoice-" + order.getOrderNumber() + ".pdf");
    }

    public void sendEmailWithAttachment(
            String to,
            String subject,
            String content,
            byte[] file,
            String filename) {

        try {

            Resend resend = new Resend(apiKey);

            Attachment attachment = Attachment.builder()
                    .fileName(filename)
                    .content(Base64.getEncoder().encodeToString(file))
                    .build();

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("BonBon Coffee <onboarding@resend.dev>")
                    .to(to)
                    .subject(subject)
                    .text(content)
                    .attachments(new Attachment[]{attachment})
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            throw new RuntimeException("Send mail failed", e);
        }
    }
}