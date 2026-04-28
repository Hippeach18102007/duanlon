package com.retailmanagement.repository;

import com.retailmanagement.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
        Optional<Promotion> findByCode(String code);

        List<Promotion> findByIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        LocalDateTime at1, LocalDateTime at2);

        List<Promotion> findByIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                        LocalDateTime at1, LocalDateTime at2, Integer idNot);

        // Khuyến mãi sắp hết hạn
        List<Promotion> findByIsActiveTrueAndEndDateBetween(LocalDateTime from, LocalDateTime to);

        // Tổng hợp theo khoảng thời gian
        @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.startDate <= :to AND p.endDate >= :from")
        List<Promotion> findActiveInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
        @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
                "AND p.startDate <= :now AND p.endDate >= :now")
        List<Promotion> findAllActive(@Param("now") LocalDateTime now);
}
