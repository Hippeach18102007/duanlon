package com.retailmanagement.repository;

import com.retailmanagement.entity.PromotionCustomerUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionCustomerUsageRepository extends JpaRepository<PromotionCustomerUsage, Long> {

    /**
     * Check if a customer has already used a specific promotion
     */
    boolean existsByPromotionIdAndCustomerId(Integer promotionId, Integer customerId);

    /**
     * Find usage record for a specific customer + promotion
     */
    Optional<PromotionCustomerUsage> findByPromotionIdAndCustomerId(Integer promotionId, Integer customerId);

    /**
     * Get all promotion IDs that a customer has already used
     */
    List<PromotionCustomerUsage> findByCustomerId(Integer customerId);

    Optional<PromotionCustomerUsage> findByPromotionIdAndCustomerIdAndOrderId(Integer promotionId, Integer customerId, Long orderId);
}