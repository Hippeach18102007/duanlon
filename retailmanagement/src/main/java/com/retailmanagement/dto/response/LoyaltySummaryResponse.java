package com.retailmanagement.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltySummaryResponse {

    private String period;           // "2025-W03" hoặc "2025-01"
    private String periodLabel;      // "Tuần 3, 2025" hoặc "Tháng 1, 2025"
    private LocalDate periodStart;
    private LocalDate periodEnd;

    private int totalPointsEarned;   // Tổng điểm cộng (EARN)
    private int totalPointsDeducted; // Tổng điểm trừ (DEDUCT)
    private int netPoints;           // = earned - deducted
    private long totalTransactions;  // Số giao dịch

    // Chỉ dùng cho Admin: breakdown theo từng khách hàng
    private List<CustomerPointSummary> customerBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerPointSummary {
        private Integer customerId;
        private String customerName;
        private String customerEmail;
        private String vipTier;
        private int pointsEarned;
        private int pointsDeducted;
        private int netPoints;
        private long transactionCount;
    }
}