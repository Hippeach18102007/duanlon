package com.retailmanagement.constants;

import com.retailmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduler tự động gửi thông báo sinh nhật mỗi ngày
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayScheduler {

    private final NotificationService notificationService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ================================================================
    // PRODUCTION JOBS
    // ================================================================

    /**
     * Gửi thông báo sinh nhật — 6:00 sáng mỗi ngày
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyBirthdayNotifications() {
        String currentTime = LocalDateTime.now().format(formatter);
        log.info("═══════════════════════════════════════════════════");
        log.info("🎂 BẮT ĐẦU KIỂM TRA SINH NHẬT - {}", currentTime);
        log.info("═══════════════════════════════════════════════════");

        try {
            notificationService.sendBirthdayNotifications();
            log.info("✅ HOÀN THÀNH gửi thông báo sinh nhật - {}",
                    LocalDateTime.now().format(formatter));
        } catch (Exception e) {
            log.error("❌ LỖI khi gửi thông báo sinh nhật: {}", e.getMessage(), e);
        }

        log.info("═══════════════════════════════════════════════════\n");
    }

    /**
     * Nhắc mua hàng — 9:00 sáng mỗi ngày
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendPurchaseReminders() {
        log.info("⏰ Chạy job nhắc mua hàng...");
        notificationService.sendPurchaseReminders();
    }

    /**
     * Chào mừng khách chưa có đơn — 9:30 sáng mỗi ngày
     * (stagger 30 phút so với sendPurchaseReminders để tránh load đồng thời)
     */
    @Scheduled(cron = "0 30 9 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendWelcomeToZeroOrderCustomers() {
        log.info("⏰ [SCHEDULED] sendWelcomeToZeroOrderCustomers start...");
        try {
            notificationService.sendWelcomeToZeroOrderCustomers();
        } catch (Exception e) {
            log.error("❌ sendWelcomeToZeroOrderCustomers failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra lên hạng VIP — 10:00 sáng mỗi ngày
     */
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Ho_Chi_Minh")
    public void checkTierUpgrades() {
        log.info("⏰ Chạy job kiểm tra lên hạng...");
        notificationService.checkAndSendTierUpgradeNotifications();
    }

    /**
     * Cảnh báo spin bonus sắp hết hạn — mỗi 6 tiếng
     */
    @Scheduled(cron = "0 0 */6 * * *", zone = "Asia/Ho_Chi_Minh")
    public void checkExpiringSpinBonuses() {
        log.info("⏰ [Scheduler] Checking expiring spin bonuses...");
        notificationService.sendSpinExpiryWarnings();
    }

}