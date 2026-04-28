    package com.retailmanagement.repository;

    import com.retailmanagement.entity.Customer;
    import com.retailmanagement.entity.CustomerType;
    import com.retailmanagement.entity.NotificationType;
    import com.retailmanagement.entity.VipTier;
    import jakarta.persistence.LockModeType;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Lock;
    import org.springframework.data.jpa.repository.Modifying;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.*;
    import org.springframework.stereotype.Repository;
    import java.math.BigDecimal;
    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Optional;
    @Repository
    public interface CustomRes extends JpaRepository<Customer, Integer> {
        Optional<Customer> findByEmail(String Email);
        Optional<Customer> findByPhone(String phone);
        List<Customer> findAll();
        List<Customer> findByCustomerType(CustomerType type);
        List<Customer> findByName(String name);
        Optional<Customer> findById(Integer id);

        @Query("SELECT c FROM Customer c WHERE c.lastLoginAt >= :since AND c.isActive = true ORDER BY c.lastLoginAt DESC")
        List<Customer> findActiveCustomersSince(@Param("since") LocalDateTime since);

        @Query(value = "SELECT * FROM customers WHERE is_active = 1 " +
                "AND MONTH(date_of_birth) = MONTH(GETDATE()) " +
                "AND DAY(date_of_birth) = DAY(GETDATE())",
                nativeQuery = true)
        List<Customer> findCustomersWithBirthdayToday();

        /**
         * Tìm khách hàng có sinh nhật trong tháng cụ thể
         */
        @Query(value = "SELECT * FROM customers WHERE is_active = 1 " +
                "AND MONTH(date_of_birth) = :month " +
                "ORDER BY DAY(date_of_birth)",
                nativeQuery = true)
        List<Customer> findCustomersWithBirthdayInMonth(@Param("month") int month);

        /**
         * Tìm khách hàng có sinh nhật trong khoảng thời gian
         */
        @Query(value = "SELECT * FROM customers WHERE is_active = 1 " +
                "AND ((MONTH(date_of_birth) = :startMonth AND DAY(date_of_birth) >= :startDay) " +
                "     OR (MONTH(date_of_birth) = :endMonth AND DAY(date_of_birth) <= :endDay) " +
                "     OR (MONTH(date_of_birth) > :startMonth AND MONTH(date_of_birth) < :endMonth)) " +
                "ORDER BY MONTH(date_of_birth), DAY(date_of_birth)",
                nativeQuery = true)
        List<Customer> findCustomersWithBirthdayBetween(
                @Param("startMonth") int startMonth,
                @Param("startDay") int startDay,
                @Param("endMonth") int endMonth,
                @Param("endDay") int endDay
        );

        /**
         * Đếm số khách hàng có sinh nhật trong tháng
         */
        @Query(value = "SELECT COUNT(*) FROM customers WHERE is_active = 1 " +
                "AND MONTH(date_of_birth) = :month",
                nativeQuery = true)
        long countCustomersWithBirthdayInMonth(@Param("month") int month);


        @Query("SELECT c FROM Customer c WHERE c.isActive = true " +
                "AND c.loyaltyPoints >= :minPoints " +
                "AND c.loyaltyPoints <= :maxPoints " +
                "ORDER BY c.loyaltyPoints DESC")
        List<Customer> findByLoyaltyPointsBetween(
                @Param("minPoints") int minPoints,
                @Param("maxPoints") int maxPoints
        );
        @Query("SELECT c FROM Customer c WHERE c.isActive = true " +
                "AND c.vipTier = :vipTier " +
                "ORDER BY c.loyaltyPoints DESC")
        List<Customer> findByVipTier(@Param("vipTier") VipTier vipTier);

        @Query("SELECT c FROM Customer c WHERE c.isActive = true " +
                "AND c.vipTier = :vipTier " +
                "AND c.loyaltyPoints >= :minPoints " +
                "AND c.loyaltyPoints <= :maxPoints " +
                "ORDER BY c.loyaltyPoints DESC")
        List<Customer> findByVipTierAndLoyaltyPointsBetween(
                @Param("vipTier") VipTier vipTier,
                @Param("minPoints") int minPoints,
                @Param("maxPoints") int maxPoints
        );
        // Add these methods to CustomRes.java interface

        /**
         * Tìm khách hàng theo khoảng chi tiêu
         */
        @Query("SELECT c FROM Customer c WHERE c.isActive = true " +
                "AND c.totalSpent >= :minSpent " +
                "AND c.totalSpent <= :maxSpent " +
                "ORDER BY c.totalSpent DESC")
        List<Customer> findByTotalSpentBetween(
                @Param("minSpent") BigDecimal minSpent,
                @Param("maxSpent") BigDecimal maxSpent
        );

        /**
         * Tìm top N khách hàng theo tổng chi tiêu
         */
        @Query("SELECT c FROM Customer c WHERE c.isActive = true " +
                "ORDER BY c.totalSpent DESC")
        List<Customer> findTopByTotalSpent(Pageable pageable);

        /**
         * Tìm top N khách hàng theo VIP tier và chi tiêu
         */
        @Query("SELECT c FROM Customer c WHERE c.isActive = true " +
                "AND c.vipTier = :vipTier " +
                "ORDER BY c.totalSpent DESC")
        List<Customer> findTopByVipTierAndTotalSpent(
                @Param("vipTier") VipTier vipTier,
                Pageable pageable
        );

        @Query("SELECT SUM(c.totalSpent) FROM Customer c WHERE c.isActive = true")
        BigDecimal getTotalSpentAllCustomers();
        @Query("SELECT SUM(c.totalSpent) FROM Customer c WHERE c.isActive = true " +
                "AND c.vipTier = :vipTier")
        BigDecimal getTotalSpentByVipTier(@Param("vipTier") VipTier vipTier);

        @Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = true " +
                "AND c.totalSpent >= :minSpent " +
                "AND c.totalSpent <= :maxSpent")
        long countByTotalSpentBetween(
                @Param("minSpent") BigDecimal minSpent,
                @Param("maxSpent") BigDecimal maxSpent
        );


        @Query(value = """
    SELECT * FROM customers c
    WHERE c.is_active = 1
      AND c.last_order_at IS NOT NULL
      AND c.last_order_at <= :upperBound
      AND c.last_order_at > :lowerBound
      AND c.id NOT IN (
          SELECT n.customer_id FROM notifications n
          WHERE n.type IN ('PURCHASE_REMINDER', 'WINBACK')
            AND n.created_at >= :recentCutoff
      )
    """, nativeQuery = true)
        List<Customer> findCustomersForPurchaseReminder(
                @Param("upperBound") LocalDateTime upperBound,
                @Param("lowerBound") LocalDateTime lowerBound,
                @Param("recentCutoff") LocalDateTime recentCutoff
        );

        /**
         * Đếm tổng reminder đã gửi theo loại (cho stats)
         */
        @Query("""
    SELECT COUNT(DISTINCT n.customer.id) FROM Notification n
    WHERE n.type = :type
    AND n.createdAt >= :since
""")
        long countRemindersSentSince(
                @Param("type") NotificationType type,
                @Param("since") LocalDateTime since
        );
        Optional<Customer> findByUserId(Integer userId);
        @Query(value = """
    SELECT c.* FROM customers c
    CROSS APPLY (
        SELECT MAX(t) AS last_activity_at FROM (
            SELECT MAX(o.created_at)   AS t FROM orders o WHERE o.customer_id = c.id
            UNION ALL
            SELECT MAX(o.cancelled_at) FROM orders o WHERE o.customer_id = c.id
            UNION ALL
            SELECT MAX(p.paid_at)      FROM payments p
                INNER JOIN orders o ON p.order_id = o.id WHERE o.customer_id = c.id
            UNION ALL
            SELECT MAX(ll.created_at)  FROM loyalty_ledger ll WHERE ll.customer_id = c.id
            UNION ALL
            SELECT MAX(sw.spun_at)     FROM spin_wheel_history sw WHERE sw.customer_id = c.id
            UNION ALL
            SELECT MAX(r.created_at)   FROM returns r
                INNER JOIN orders o ON r.order_id = o.id WHERE o.customer_id = c.id
        ) AS all_activities(t)
    ) AS last_act
    WHERE c.is_active = 1
    AND (
        last_act.last_activity_at < :cutoff
        OR last_act.last_activity_at IS NULL
    )
    ORDER BY last_act.last_activity_at ASC
    """, nativeQuery = true)
        List<Customer> findCustomersInactiveTransaction(
                @Param("cutoff") LocalDateTime cutoff
        );
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT c FROM Customer c WHERE c.id = :id")
        Optional<Customer> findByIdWithLock(@Param("id") Integer id);


        // Tìm khách hàng chưa có đơn hàng nào, đã đăng ký >= minDaysAgo ngày
        @Query(value = """
    SELECT c.* FROM customers c
    WHERE c.is_active = 1
      AND c.id NOT IN (
          SELECT DISTINCT o.customer_id FROM orders o
          WHERE o.customer_id IS NOT NULL
      )
      AND c.created_at <= :registeredBefore
    ORDER BY c.created_at DESC
    """, nativeQuery = true)
        List<Customer> findZeroOrderCustomers(
                @Param("registeredBefore") LocalDateTime registeredBefore
        );

        // Đếm tổng
        @Query(value = """
    SELECT COUNT(*) FROM customers c
    WHERE c.is_active = 1
      AND c.id NOT IN (
          SELECT DISTINCT o.customer_id FROM orders o
          WHERE o.customer_id IS NOT NULL
      )
      AND c.created_at <= :registeredBefore
    """, nativeQuery = true)
        long countZeroOrderCustomers(
                @Param("registeredBefore") LocalDateTime registeredBefore
        );
        // SpinWheelHistoryRepository hoặc CustomRes
        @Modifying
        @Query("UPDATE Customer c SET c.spinDiscountBonus = :bonus WHERE c.id = :customerId")
        void updateSpinBonus(@Param("customerId") Integer customerId, @Param("bonus") BigDecimal bonus);
    }
