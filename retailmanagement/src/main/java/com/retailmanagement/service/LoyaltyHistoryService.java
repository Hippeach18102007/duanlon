package com.retailmanagement.service;

import com.retailmanagement.dto.response.LoyaltyLedgerResponse;
import com.retailmanagement.entity.LoyaltyLedger;
import com.retailmanagement.entity.VipTier;
import com.retailmanagement.repository.LoyaltyLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoyaltyHistoryService {

    private final LoyaltyLedgerRepository loyaltyLedgerRepository;

    /**
     * Lấy toàn bộ lịch sử của khách hàng
     */
    public List<LoyaltyLedgerResponse> getCustomerHistory(Integer customerId) {
        List<LoyaltyLedger> ledgers = loyaltyLedgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        System.out.println("=== DEBUG: Loyalty History ===");
        System.out.println("Total records: " + ledgers.size());

        return ledgers.stream()
                .map(ledger -> {
                    LoyaltyLedgerResponse response = mapToResponse(ledger);
                    System.out.println(String.format(
                            "ID: %d | Type: %s | RefType: %s | RefId: %d | Points: %d",
                            response.getId(),
                            response.getTransactionType(),
                            response.getReferenceType(),
                            response.getReferenceId(),
                            response.getPointsDelta()
                    ));
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch sử thay đổi hạng VIP
     */
    public List<LoyaltyLedgerResponse> getTierChanges(Integer customerId) {
        return loyaltyLedgerRepository.findTierChanges(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy 10 giao dịch gần nhất
     */
    public List<LoyaltyLedgerResponse> getRecentHistory(Integer customerId) {
        return loyaltyLedgerRepository.findTop10ByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * ✅ CRITICAL: Map LoyaltyLedger entity to Response DTO
     * Đảm bảo referenceType và referenceId được trả về đầy đủ
     */
    private LoyaltyLedgerResponse mapToResponse(LoyaltyLedger ledger) {
        // Debug log
        System.out.println("Mapping ledger: " + ledger.getId() +
                " | RefType: " + ledger.getReferenceType() +
                " | RefId: " + ledger.getReferenceId());

        return LoyaltyLedgerResponse.builder()
                .id(ledger.getId())
                .customerId(ledger.getCustomer().getId())
                .customerName(ledger.getCustomer().getName())
                .pointsDelta(ledger.getPointsDelta())
                .transactionType(ledger.getTransactionType())
                .transactionTypeDisplay(getTransactionTypeDisplay(ledger.getTransactionType()))
                .tierBefore(ledger.getTierBefore())
                .tierBeforeDisplay(getTierDisplay(ledger.getTierBefore()))
                .tierAfter(ledger.getTierAfter())
                .tierAfterDisplay(getTierDisplay(ledger.getTierAfter()))
                .reason(ledger.getReason())
                .note(ledger.getNote())
                // ✅ CRITICAL: Đảm bảo referenceType và referenceId được map
                .referenceType(ledger.getReferenceType())
                .referenceId(ledger.getReferenceId())
                .createdAt(ledger.getCreatedAt())
                .build();
    }

    /**
     * Get display name for transaction type
     */
    private String getTransactionTypeDisplay(String type) {
        if (type == null) return "—";

        Map<String, String> displayMap = new HashMap<>();
        displayMap.put("EARN", "Cộng điểm");
        displayMap.put("DEDUCT", "Trừ điểm");
        displayMap.put("TIER_UPGRADE", "Thăng hạng");
        displayMap.put("TIER_DOWNGRADE", "Hạ hạng");
        displayMap.put("PENALTY", "Phạt");

        return displayMap.getOrDefault(type, type);
    }

    /**
     * Get display name for VIP tier
     */
    private String getTierDisplay(String tier) {
        if (tier == null) return "Member";

        try {
            return VipTier.valueOf(tier).getDisplayName();
        } catch (Exception e) {
            return tier;
        }
    }
}