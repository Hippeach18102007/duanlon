package com.retailmanagement.repository;

import com.retailmanagement.entity.Order;
import com.retailmanagement.entity.SpinWheelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionHistoryRepository extends JpaRepository<Order, Long> {

    /**
     * Lấy tất cả đơn hàng có áp dụng mã khuyến mãi của một khách hàng.
     * JOIN với promotions theo applied_promotion_code.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.customer.id = :customerId
          AND o.appliedPromotionCode IS NOT NULL
        ORDER BY o.createdAt DESC
    """)
    List<Order> findOrdersWithPromotion(@Param("customerId") Integer customerId);

    /**
     * Lấy toàn bộ lịch sử spin wheel của khách hàng.
     */
    @Query("""
        SELECT s FROM SpinWheelHistory s
        WHERE s.customer.id = :customerId
        ORDER BY s.spunAt DESC
    """)
    List<SpinWheelHistory> findSpinHistoryByCustomer(@Param("customerId") Integer customerId);
}