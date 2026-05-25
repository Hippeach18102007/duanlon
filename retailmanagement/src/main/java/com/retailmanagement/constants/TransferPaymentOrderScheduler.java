package com.retailmanagement.constants;

import com.retailmanagement.entity.Order;
import com.retailmanagement.repository.OrderRepository;
import com.retailmanagement.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduler tự động hủy các đơn thanh toán chuyển khoản nhưng chưa thanh toán sau 24h.
 *
 * <p>Job chạy mỗi phút để đảm bảo đơn quá hạn được xử lý sớm nhất có thể.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferPaymentOrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Ho_Chi_Minh")
    public void autoCancelUnpaidTransferOrders() {
        Instant cancelCutoff = Instant.now().minus(24, ChronoUnit.HOURS);

        log.info("══════════════════════════════════════════════");
        log.info("⏰ TRANSFER AUTO-CANCEL JOB — cutoff = {}", cancelCutoff);
        log.info("══════════════════════════════════════════════");

        List<Order> ordersToCancel = orderRepository.findTransferOrdersToCancel(cancelCutoff);
        log.info("   Số đơn transfer chưa thanh toán cần hủy: {}", ordersToCancel.size());

        int success = 0;
        int failed = 0;

        for (Order order : ordersToCancel) {
            try {
                orderService.cancelOrder(order.getId(), "AUTO_CANCEL_UNPAID_TRANSFER_24H");
                success++;
                log.info("   ✅ Đã hủy tự động đơn transfer #{} (id={})",
                        order.getOrderNumber(), order.getId());
            } catch (Exception e) {
                failed++;
                log.error("   ❌ Lỗi khi hủy đơn transfer #{} (id={}): {}",
                        order.getOrderNumber(), order.getId(), e.getMessage(), e);
            }
        }

        log.info("══════════════════════════════════════════════");
        log.info("   Kết quả — Thành công: {} | Thất bại: {}", success, failed);
        log.info("══════════════════════════════════════════════\n");
    }
}

