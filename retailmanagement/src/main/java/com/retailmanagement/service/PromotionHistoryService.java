package com.retailmanagement.service;

import com.retailmanagement.dto.response.PromotionHistoryResponse;
import com.retailmanagement.entity.Order;
import com.retailmanagement.entity.Promotion;
import com.retailmanagement.entity.SpinWheelHistory;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.repository.NotificationRepository;
import com.retailmanagement.repository.PromotionHistoryRepository;
import com.retailmanagement.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PromotionHistoryService {

    private final PromotionHistoryRepository promotionHistoryRepository;
    private final PromotionRepository        promotionRepository;
    private final CustomRes                  customerRepository;
    private final NotificationRepository     notificationRepository; // ✅ THÊM

    public List<PromotionHistoryResponse> getPromotionHistory(Integer customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với id: " + customerId));

        List<PromotionHistoryResponse> result = new ArrayList<>();

        // ── 1. Mã đã dùng trong đơn hàng ────────────────────────────
        List<Order> orders = promotionHistoryRepository.findOrdersWithPromotion(customerId);
        Set<String> usedCodes = new HashSet<>(); // track mã đã dùng để tránh hiện 2 lần

        for (Order order : orders) {
            String code = order.getAppliedPromotionCode();
            usedCodes.add(code);

            Promotion promotion = promotionRepository.findByCode(code).orElse(null);
            LocalDateTime usedAt = order.getCreatedAt() != null
                    ? order.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : null;

            result.add(PromotionHistoryResponse.builder()
                    .type("PROMOTION_CODE")
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .usedAt(usedAt)
                    .promotionCode(code)
                    .promotionName(promotion != null ? promotion.getName() : code)
                    .discountType(promotion != null ? promotion.getDiscountType() : "UNKNOWN")
                    .discountValue(promotion != null ? promotion.getDiscountValue() : null)
                    .discountTotal(order.getDiscountTotal())
                    .status("Đã sử dụng")
                    .expiresAt(promotion != null ? promotion.getEndDate() : null)
                    .build());
        }

        // ── 2. ✅ THÊM MỚI: Voucher đã nhận (từ notification BIRTHDAY)
        //       nhưng chưa dùng → hiện với status "Đang hoạt động"
        List<String> claimedCodes = notificationRepository
                .findClaimedPromotionCodesByCustomer(customerId);

        for (String code : claimedCodes) {
            if (usedCodes.contains(code)) continue; // đã hiện ở mục "Đã sử dụng" rồi

            Promotion promotion = promotionRepository.findByCode(code).orElse(null);
            if (promotion == null) continue;

            LocalDateTime now = LocalDateTime.now();
            String status;
            if (!promotion.getIsActive() || promotion.getEndDate().isBefore(now)) {
                status = "Đã hết hạn";
            } else {
                status = "Đang hoạt động";
            }

            result.add(PromotionHistoryResponse.builder()
                    .type("PROMOTION_CODE")
                    .orderId(null)
                    .orderNumber(null)
                    .usedAt(null)
                    .promotionCode(code)
                    .promotionName(promotion.getName())
                    .discountType(promotion.getDiscountType())
                    .discountValue(promotion.getDiscountValue())
                    .discountTotal(null)
                    .status(status)
                    .expiresAt(promotion.getEndDate())
                    .build());
        }

        // ── 3. Lịch sử spin wheel ────────────────────────────────────
        List<SpinWheelHistory> spinHistories = promotionHistoryRepository.findSpinHistoryByCustomer(customerId);
        for (SpinWheelHistory spin : spinHistories) {
            String status;
            if (spin.getIsUsed()) {
                status = "Đã sử dụng";
            } else if (spin.getExpiresAt().isBefore(LocalDateTime.now())) {
                status = "Đã hết hạn";
            } else {
                status = "Đang hoạt động";
            }

            result.add(PromotionHistoryResponse.builder()
                    .type("SPIN_WHEEL")
                    .orderId(spin.getUsedOrderId())
                    .orderNumber(null)
                    .usedAt(spin.getIsUsed() ? spin.getSpunAt() : null)
                    .promotionCode("SPIN_WHEEL")
                    .promotionName("Vòng quay may mắn")
                    .discountType("PERCENT")
                    .discountValue(spin.getDiscountBonus())
                    .discountTotal(null)
                    .status(status)
                    .expiresAt(spin.getExpiresAt())
                    .build());
        }

        // ── 4. Sắp xếp: Đang hoạt động lên đầu, rồi mới nhất ───────
        result.sort(Comparator
                .comparing((PromotionHistoryResponse r) -> {
                    if ("Đang hoạt động".equals(r.getStatus())) return 0;
                    if ("Đã sử dụng".equals(r.getStatus())) return 1;
                    return 2; // Đã hết hạn
                })
                .thenComparing(
                        r -> r.getUsedAt() != null ? r.getUsedAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()
                )
        );

        return result;
    }
}