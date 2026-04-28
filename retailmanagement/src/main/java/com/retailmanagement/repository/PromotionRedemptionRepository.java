package com.retailmanagement.repository;

import com.retailmanagement.entity.PromotionRedemption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PromotionRedemption r WHERE r.promotionId = :promotionId")
    Optional<PromotionRedemption> findByPromotionId(@Param("promotionId") Integer promotionId);
}