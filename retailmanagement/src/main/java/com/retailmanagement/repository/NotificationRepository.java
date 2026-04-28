package com.retailmanagement.repository;

import com.retailmanagement.entity.Notification;
import com.retailmanagement.entity.NotificationType;
import com.retailmanagement.entity.SpinWheelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    List<Notification> findByCustomerIdAndIsReadFalseOrderByCreatedAtDesc(Integer customerId);

    long countByCustomerIdAndIsReadFalse(Integer customerId);

    boolean existsByCustomerIdAndTypeAndCreatedAtBetween(
            Integer customerId,
            NotificationType type,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<Notification> findByCustomerIdAndTypeOrderByCreatedAtDesc(Integer customerId, NotificationType type);

    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    List<Notification> findByTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            NotificationType type, LocalDateTime from, LocalDateTime to);

    boolean existsByCustomerIdAndTypeAndCreatedAtAfter(
            Integer customerId,
            NotificationType type,
            LocalDateTime after
    );

    @Query("SELECT s FROM SpinWheelHistory s " +
            "WHERE s.isUsed = false " +
            "AND s.expiresAt > :now " +
            "AND s.expiresAt <= :threshold " +
            "ORDER BY s.expiresAt ASC")
    List<SpinWheelHistory> findExpiringUnusedBonuses(
            @Param("now")       LocalDateTime now,
            @Param("threshold") LocalDateTime threshold
    );

    // ✅ THÊM MỚI: Lấy mã promotion đã được gửi qua notification BIRTHDAY
    // Dùng trong PromotionHistoryService để hiện voucher "Đang hoạt động"
    // ngay cả khi khách chưa dùng mã (chưa có đơn hàng nào áp mã này)
    @Query("""
        SELECT DISTINCT 'BIRTHDAY250K'
        FROM Notification n
        WHERE n.customer.id = :customerId
          AND n.type = com.retailmanagement.entity.NotificationType.BIRTHDAY
    """)
    List<String> findClaimedPromotionCodesByCustomer(@Param("customerId") Integer customerId);
}