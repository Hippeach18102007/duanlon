package com.retailmanagement.controller;

import com.retailmanagement.dto.response.CustomerMyDashboardResponse;
import com.retailmanagement.service.CustomerMyDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Dashboard cá nhân cho khách hàng đang đăng nhập.
 *
 * Base path: /api/auth/customers/my
 *
 * Không cần truyền customerId — tự động lấy từ JWT/session hiện tại.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth/customers/my")
@RequiredArgsConstructor
public class CustomerMyDashboardController {

    private final CustomerMyDashboardService myDashboardService;

    // ================================================================
    // FULL DASHBOARD
    // GET /api/auth/customers/my/dashboard
    // ================================================================

    /**
     * Trả về toàn bộ data dashboard của khách hàng đang đăng nhập:
     *
     * - profile        : thông tin cá nhân + VIP tier
     * - summary        : điểm tích lũy, tổng chi tiêu, số đơn, spin bonus
     * - recentLoyaltyHistory : 5 giao dịch điểm gần nhất
     * - recentPayments       : 5 đơn thanh toán gần nhất
     * - spinWheel            : trạng thái vòng quay
     * - recentPromotions     : 5 mã giảm giá đã dùng gần nhất
     * - vipJourney           : tiến trình VIP + milestones
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getMyDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Chưa đăng nhập"));
        }

        try {
            CustomerMyDashboardResponse dashboard =
                    myDashboardService.getMyDashboard(auth.getName());
            return ResponseEntity.ok(dashboard);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Không tìm thấy")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống", "message", e.getMessage()));
        }
    }

    // ================================================================
    // SUMMARY ONLY — nhẹ hơn, dùng cho header / widget nhỏ
    // GET /api/auth/customers/my/summary
    // ================================================================

    /**
     * Chỉ trả về AccountSummary (điểm, hạng, chi tiêu, spin bonus).
     * Dùng cho header user info hoặc notification badge.
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getMySummary() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            CustomerMyDashboardResponse dashboard =
                    myDashboardService.getMyDashboard(auth.getName());
            return ResponseEntity.ok(dashboard.getSummary());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // VIP JOURNEY — tiến trình hạng VIP
    // GET /api/auth/customers/my/vip-journey
    // ================================================================

    /**
     * Trả về VipJourney: tier hiện tại, tiến trình, milestones đã đạt.
     * Dùng cho trang "Ưu đãi thành viên".
     */
    @GetMapping("/vip-journey")
    public ResponseEntity<?> getMyVipJourney() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            CustomerMyDashboardResponse dashboard =
                    myDashboardService.getMyDashboard(auth.getName());
            return ResponseEntity.ok(dashboard.getVipJourney());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}