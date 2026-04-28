package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ data hiển thị trên dashboard của khách hàng đang đăng nhập.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMyDashboardResponse {

    // ── Thông tin cá nhân + VIP ──────────────────────────────────────
    private ProfileSection profile;

    // ── Tổng quan tài khoản ─────────────────────────────────────────
    private AccountSummary summary;

    // ── Lịch sử điểm tích lũy (5 gần nhất) ─────────────────────────
    private List<LoyaltyLedgerResponse> recentLoyaltyHistory;

    // ── Đơn hàng gần đây (5 gần nhất) ──────────────────────────────
    private List<RecentPayment> recentPayments;

    // ── Spin wheel ──────────────────────────────────────────────────
    private SpinWheelStatus spinWheel;

    // ── Khuyến mãi đã sử dụng (5 gần nhất) ─────────────────────────
    private List<PromotionHistoryResponse> recentPromotions;

    // ── Hành trình VIP ──────────────────────────────────────────────
    private VipJourney vipJourney;

    private LocalDateTime generatedAt;

    // ================================================================
    // Nested DTOs
    // ================================================================

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProfileSection {
        private Integer id;
        private String name;
        private String email;
        private String phone;
        private String address;
        private String dateOfBirth;      // dd/MM/yyyy
        private String customerType;
        private String customerTypeDisplay;
        private String vipTier;
        private String vipTierDisplay;
        private Boolean isActive;
        private LocalDateTime memberSince;
        private String vipNote;          // ghi chú từ staff (nếu có)
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AccountSummary {
        private int loyaltyPoints;
        private int pointsToNextTier;     // điểm cần để lên hạng tiếp theo
        private String nextTier;          // tên hạng tiếp theo
        private double discountRate;      // % giảm giá hiện tại (dạng 0.05 = 5%)
        private String discountDisplay;   // "5%"
        private BigDecimal totalSpent;    // tổng đã chi tiêu
        private int totalOrders;          // tổng số đơn hàng
        private BigDecimal avgOrderValue; // trung bình mỗi đơn
        private BigDecimal spinBonus;     // % giảm thêm từ spin (nếu có)
        private boolean hasActiveSpinBonus;
        private LocalDateTime lastOrderAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentPayment {
        private Long paymentId;
        private Long orderId;
        private String orderNumber;
        private BigDecimal amount;
        private String method;
        private String status;
        private String paidAt;            // dd/MM/yyyy HH:mm
        private int itemCount;
        private BigDecimal discountTotal;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SpinWheelStatus {
        private boolean canSpin;
        private int spinsRemaining;
        private String nextSpinAt;        // khi nào được quay lại
        private BigDecimal currentBonus;  // % bonus đang giữ (chưa dùng)
        private boolean hasBonus;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VipJourney {
        private String currentTier;
        private String currentTierDisplay;
        private int currentPoints;

        // Tier trước (để hiển thị đã vượt qua)
        private String previousTier;

        // Tier tiếp theo
        private String nextTier;
        private String nextTierDisplay;
        private int pointsToNext;
        private double progressPercent;   // % thanh tiến trình

        // Mốc đã đạt được
        private List<TierMilestone> milestones;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TierMilestone {
        private String tier;
        private String tierDisplay;
        private int requiredPoints;
        private boolean achieved;
        private String achievedAt;        // dd/MM/yyyy nếu có
    }
}