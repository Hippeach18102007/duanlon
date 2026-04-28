package com.retailmanagement.dto.response;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyLedgerResponse {
    private Long id;
    private Integer customerId;
    private String customerName;

    private Integer pointsDelta; // + hoặc -
    private String transactionType; // EARN / DEDUCT / TIER_UPGRADE / TIER_DOWNGRADE / PENALTY
    private String transactionTypeDisplay;

    private String tierBefore;
    private String tierBeforeDisplay;
    private String tierAfter;
    private String tierAfterDisplay;

    private String reason;
    private String note;

    // ✅ CRITICAL: Đảm bảo referenceType và referenceId có trong response
    private String referenceType;  // "orders", "returns", etc.
    private Long referenceId;      // ID của đơn hàng, đơn trả, etc.

    private Instant createdAt;
}