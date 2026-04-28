package com.retailmanagement.repository;

import com.retailmanagement.entity.SpinWheelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpinWheelHistoryRepository extends JpaRepository<SpinWheelHistory, Long> {

    /**
     * Kiểm tra khách đã quay tuần này chưa.
     */
    Optional<SpinWheelHistory> findByCustomerIdAndWeekStartDate(
            Integer customerId, LocalDate weekStartDate);

    /**
     * Toàn bộ lịch sử quay của khách, mới nhất trước.
     */
    List<SpinWheelHistory> findByCustomerIdOrderBySpunAtDesc(Integer customerId);

    /**
     * Tất cả bonus còn hiệu lực (chưa dùng, chưa hết hạn) của khách.
     */
    @Query("""
        SELECT s FROM SpinWheelHistory s
        WHERE s.customer.id = :customerId
          AND s.isUsed = false
          AND s.expiresAt > :now
        ORDER BY s.spunAt DESC
    """)
    List<SpinWheelHistory> findActiveBonus(
            @Param("customerId") Integer customerId,
            @Param("now") LocalDateTime now);

    /**
     * Bonus mới nhất còn hiệu lực.
     *
     * BUG FIX: query cũ khai báo Optional nhưng có thể trả về nhiều row
     * → gây IncorrectResultSizeDataAccessException khi khách có 2+ bonus chưa hết hạn.
     *
     * Fix: dùng LIMIT 1 (SQL Server: FETCH FIRST 1 ROW ONLY) để đảm bảo
     * luôn trả về đúng 0 hoặc 1 kết quả, ưu tiên bonus mới nhất.
     */
    @Query("""
        SELECT s FROM SpinWheelHistory s
        WHERE s.customer.id = :customerId
          AND s.isUsed = false
          AND s.expiresAt > :now
        ORDER BY s.spunAt DESC
        LIMIT 1
    """)
    Optional<SpinWheelHistory> findMostRecentActiveBonus(
            @Param("customerId") Integer customerId,
            @Param("now") LocalDateTime now);

    /**
     * Đếm số lần quay trong tuần (dùng để validate phía service).
     */
    @Query("""
        SELECT COUNT(s) FROM SpinWheelHistory s
        WHERE s.customer.id = :customerId
          AND s.weekStartDate = :weekStart
    """)
    long countByCustomerAndWeek(
            @Param("customerId") Integer customerId,
            @Param("weekStart") LocalDate weekStart);

    /**
     * Tìm spin đã được dùng cho một đơn hàng cụ thể.
     * Dùng khi hủy đơn để restore bonus.
     */
    Optional<SpinWheelHistory> findByUsedOrderId(Long usedOrderId);

    /**
     * Tìm các bonus sắp hết hạn (chưa dùng, hết hạn trong khoảng now → threshold).
     * Dùng cho NotificationService gửi cảnh báo "bonus sắp hết hạn".
     */
    @Query("""
        SELECT s FROM SpinWheelHistory s
        WHERE s.isUsed = false
          AND s.expiresAt > :now
          AND s.expiresAt <= :threshold
        ORDER BY s.expiresAt ASC
    """)
    List<SpinWheelHistory> findExpiringUnusedBonuses(
            @Param("now")       LocalDateTime now,
            @Param("threshold") LocalDateTime threshold);
    // SpinWheelHistoryRepository hoặc CustomRes

}