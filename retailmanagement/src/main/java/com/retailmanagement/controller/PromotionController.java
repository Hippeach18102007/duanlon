package com.retailmanagement.controller;

import com.retailmanagement.dto.request.PromotionRequest;
import com.retailmanagement.dto.response.ApiResponse;
import com.retailmanagement.entity.Customer;
import com.retailmanagement.entity.Promotion;
import com.retailmanagement.entity.User;
import com.retailmanagement.repository.OrderRepository;
import com.retailmanagement.repository.PromotionRepository;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.repository.UserRepository;
import com.retailmanagement.security.service.CustomUserDetails;
import com.retailmanagement.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import com.retailmanagement.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final PromotionRepository promotionRepository;
    private final CustomRes customerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @PostMapping
    public ApiResponse<Promotion> create(@RequestBody PromotionRequest req) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        String email = auth.getName();

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        return ApiResponse.success(promotionService.create(req, user.getId()));
    }

    /**
     * List promotions
     */
    @GetMapping
    public ApiResponse<List<Promotion>> list(@RequestParam(required = false) Boolean activeOnly) {
        return ApiResponse.success(promotionService.list(activeOnly));
    }

    @GetMapping("/{id}")
    public ApiResponse<Promotion> getById(@PathVariable Integer id) {
        return ApiResponse.success(
                promotionRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Promotion not found: " + id)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Promotion> update(@PathVariable Integer id, @RequestBody PromotionRequest req) {
        return ApiResponse.success(promotionService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Integer id) {
        promotionService.delete(id);
        return ApiResponse.success("Promotion deactivated successfully");
    }

    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> getReport(
            @RequestParam(required = false) String period) {
        return ApiResponse.success(promotionService.getReport(period));
    }

    @GetMapping("/conflicts")
    public ApiResponse<List<Map<String, Object>>> getConflicts() {
        return ApiResponse.success(promotionService.detectConflicts());
    }

    @GetMapping("/expiring")
    public ApiResponse<List<Promotion>> getExpiring(
            @RequestParam(defaultValue = "3") int withinDays) {
        return ApiResponse.success(promotionService.getExpiringPromotions(withinDays));
    }

    @PostMapping("/{id}/redeem")
    public ApiResponse<String> recordRedemption(@PathVariable Integer id) {
        promotionService.recordRedemption(id, 1);
        return ApiResponse.success("Redemption recorded");
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCode(
            @RequestParam String code,
            @RequestParam(defaultValue = "0") BigDecimal orderTotal) {
        try {
            String upper = code.trim().toUpperCase();
            Promotion promo = promotionRepository.findByCode(upper).orElse(null);

            if (promo == null) {
                return ResponseEntity.ok(Map.of("valid", false, "message", "Mã '" + upper + "' không tồn tại"));
            }

            LocalDateTime now = LocalDateTime.now();
            if (!promo.getIsActive()
                    || promo.getStartDate().isAfter(now)
                    || promo.getEndDate().isBefore(now)) {
                return ResponseEntity
                        .ok(Map.of("valid", false, "message", "Mã '" + upper + "' đã hết hạn hoặc chưa có hiệu lực"));
            }

            // Check per-customer usage
            Customer currentCustomer = getCurrentCustomer();
            if (currentCustomer != null
                    && promotionService.hasCustomerUsedPromotion(promo.getId(), currentCustomer.getId())) {
                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "message", "Bạn đã sử dụng mã '" + upper + "' rồi"));
            }

            if (promo.getMinOrderAmount() != null
                    && orderTotal.compareTo(promo.getMinOrderAmount()) < 0) {
                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "message", String.format(
                                "Đơn hàng cần tối thiểu %,.0f đ (hiện tại: %,.0f đ)",
                                promo.getMinOrderAmount().doubleValue(),
                                orderTotal.doubleValue())));
            }

            BigDecimal afterDiscount = promotionService.applyDiscount(
                    orderTotal, promo.getDiscountType(), promo.getDiscountValue());
            BigDecimal discountAmount = orderTotal.subtract(afterDiscount);
            if (discountAmount.compareTo(orderTotal) > 0)
                discountAmount = orderTotal;

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Mã hợp lệ",
                    "code", upper,
                    "promotionName", promo.getName(),
                    "discountType", promo.getDiscountType(),
                    "discountValue", promo.getDiscountValue(),
                    "discountAmount", discountAmount,
                    "minOrderAmount", promo.getMinOrderAmount() != null ? promo.getMinOrderAmount() : 0,
                    "endDate", promo.getEndDate().toString()));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Lỗi kiểm tra mã: " + e.getMessage()));
        }
    }

    /**
     * Get available promotions for the currently logged-in customer.
     * Returns only promotions that are active, within date range,
     * not yet used by this customer, and matching customer type/tier.
     */
    @GetMapping("/available")
    public ApiResponse<List<Map<String, Object>>> getAvailableForCustomer() {
        Customer customer = getCurrentCustomer();
        if (customer == null) {
            throw new RuntimeException("Bạn cần đăng nhập để xem mã khuyến mãi");
        }
        return ApiResponse.success(promotionService.getAvailablePromotionsForCustomer(customer));
    }

    /**
     * FIX: Tìm customer qua userId thay vì email.
     *
     * Nguyên nhân lỗi cũ:
     *   auth.getName() trả về username ("phan.van.tu"), KHÔNG phải email ("pvtu@gmail.com")
     *   → findByEmail("phan.van.tu") → không tìm thấy → return null
     *
     * Cách fix:
     *   Lấy userId từ CustomUserDetails (đã có sẵn) → findByUserId(userId)
     */
    private Customer getCurrentCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        // ✅ Cách chính: lấy userId từ CustomUserDetails → tìm customer qua user_id
        if (principal instanceof CustomUserDetails userDetails) {
            Integer userId = userDetails.getUserId();
            if (userId != null) {
                return customerRepository.findByUserId(userId).orElse(null);
            }
        }

        // Fallback: thử tìm bằng username → userId → customer
        String name = auth.getName();
        return userRepository.findByUsername(name)
                .flatMap(user -> customerRepository.findByUserId(user.getId()))
                .orElse(null);
    }

    @GetMapping("/{id}/redemption-details")
    public ApiResponse<List<Map<String, Object>>> getRedemptionDetails(@PathVariable Integer id) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found: " + id));

        List<Order> orders = orderRepository.findByAppliedPromotionCodeOrderByCreatedAtDesc(promo.getCode());

        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", o.getId());
            row.put("orderNumber", o.getOrderNumber());
            row.put("customerName", o.getCustomer() != null ? o.getCustomer().getName() : "—");
            row.put("customerId", o.getCustomer() != null ? o.getCustomer().getId() : null);
            BigDecimal promoDiscount = o.getDiscountTotal() != null ? o.getDiscountTotal() : BigDecimal.ZERO;
            BigDecimal vipDiscount = o.getVipDiscount() != null ? o.getVipDiscount() : BigDecimal.ZERO;
            BigDecimal spinDiscount = o.getSpinDiscount() != null ? o.getSpinDiscount() : BigDecimal.ZERO;
            BigDecimal netPromoDiscount = promoDiscount.subtract(vipDiscount).subtract(spinDiscount).abs();
            row.put("discountTotal", netPromoDiscount);
            row.put("totalAmount", o.getTotalAmount());
            row.put("usedAt", o.getCreatedAt());
            row.put("status", o.getStatus());
            return row;
        }).collect(java.util.stream.Collectors.toList());

        return ApiResponse.success(result);
    }
    @GetMapping("/available-for-customer/{customerId}")
    public ApiResponse<List<Map<String, Object>>> getAvailableForCustomerId(
            @PathVariable Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        return ApiResponse.success(promotionService.getAvailablePromotionsForCustomer(customer));
    }
}