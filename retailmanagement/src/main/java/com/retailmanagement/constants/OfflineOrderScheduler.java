package com.retailmanagement.constants;

import com.retailmanagement.entity.Order;
import com.retailmanagement.repository.OrderRepository;
import com.retailmanagement.service.OrderEmailService;
import com.retailmanagement.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfflineOrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderEmailService orderEmailService;

    // Chạy vào lúc 02:00 sáng mỗi ngày
    @Scheduled(cron = "0 * * * * ?")
    public void processOverdueOfflineOrders() {
        log.info("Bắt đầu quét đơn hàng OFFLINE quá hạn...");

        Instant now = Instant.now();
        Instant warningCutoff = now.minus(2, ChronoUnit.DAYS); // Đã qua 48h
        Instant cancelCutoff = now.minus(3, ChronoUnit.DAYS);  // Đã qua 72h

        // 1. Quét đơn cần gửi nhắc nhở (Đã qua 48h nhưng chưa tới 72h)
        List<Order> ordersToWarn = orderRepository.findOfflineOrdersToWarn(warningCutoff, cancelCutoff);
        for (Order order : ordersToWarn) {
            try {
                orderEmailService.sendOfflinePickupReminderEmail(order);
                log.info("Đã gửi mail nhắc nhở nhận hàng cho đơn: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Lỗi khi gửi nhắc nhở đơn {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }

        // 2. Quét đơn cần Hủy (Đã qua 72h)
        List<Order> ordersToCancel = orderRepository.findOfflineOrdersToCancel(cancelCutoff);
        for (Order order : ordersToCancel) {
            try {
                // Tái sử dụng hàm cancelOrder của OrderService. Hàm này đã xử lý xả kho, trả point các kiểu rồi.
                orderService.cancelOrder(order.getId(), "AUTO_CANCEL_EXPIRED_PICKUP");
                orderEmailService.sendOfflineAutoCancelEmail(order);

                log.info("Đã HỦY TỰ ĐỘNG đơn hàng OFFLINE quá 72h: {}", order.getOrderNumber());
            } catch (Exception e) {
                log.error("Lỗi khi auto-cancel đơn {}: {}", order.getOrderNumber(), e.getMessage());
            }
        }

        log.info("Hoàn tất quét đơn hàng OFFLINE.");
    }
}