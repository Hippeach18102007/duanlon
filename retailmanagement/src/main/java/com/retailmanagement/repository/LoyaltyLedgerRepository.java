package com.retailmanagement.repository;

import com.retailmanagement.entity.LoyaltyLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LoyaltyLedgerRepository extends JpaRepository<LoyaltyLedger, Long> {

    /**
     * Find all loyalty ledger entries for a specific customer
     */


    /**
     * Find loyalty ledger entries by reference (e.g., order or return)
     */
    List<LoyaltyLedger> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    /**
     * Find entries within a date range for reporting
     */
    @Query("SELECT l FROM LoyaltyLedger l WHERE l.customer.id = :customerId " +
            "AND l.createdAt BETWEEN :startDate AND :endDate ORDER BY l.createdAt DESC")
    List<LoyaltyLedger> findByCustomerAndDateRange(
            @Param("customerId") Integer customerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Calculate total points earned/spent by reason
     */
    @Query("SELECT SUM(l.pointsDelta) FROM LoyaltyLedger l " +
            "WHERE l.customer.id = :customerId AND l.reason = :reason")
    Integer sumPointsByCustomerAndReason(
            @Param("customerId") Integer customerId,
            @Param("reason") String reason
    );
    @Query("SELECT ll FROM LoyaltyLedger ll WHERE ll.customer.id = :customerId " +
            "AND ll.tierBefore IS NOT NULL AND ll.tierAfter IS NOT NULL " +
            "ORDER BY ll.createdAt DESC")
    List<LoyaltyLedger> findTierChanges(@Param("customerId") Integer customerId);
    List<LoyaltyLedger> findTop10ByCustomerIdOrderByCreatedAtDesc(Integer customerId);





    List<LoyaltyLedger> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    @Query("SELECT ll FROM LoyaltyLedger ll WHERE ll.customer.id = :customerId " +
            "AND ll.transactionType IN ('TIER_UPGRADE','TIER_DOWNGRADE') " +
            "ORDER BY ll.createdAt DESC")
    List<LoyaltyLedger> findTierChangesByCustomerId(@Param("customerId") Integer customerId);

    // ── Tổng hợp WEEKLY cho 1 khách hàng ──────────────────────────────────────
    /**
     * Trả về list Object[]:
     *  [0] yearWeek  (INT, e.g. 202503)
     *  [1] earnSum   (INT)
     *  [2] deductSum (INT, luôn >= 0)
     *  [3] txCount   (LONG)
     *
     * SQL Server dùng DATEPART(iso_week, ...) và YEAR(...)
     */
    @Query(value = """
        SELECT
            YEAR(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                * 100
            + DATEPART(iso_week,
                DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01')
              )                                          AS year_week,
            SUM(CASE WHEN ll.points_delta > 0 THEN ll.points_delta ELSE 0 END) AS earn_sum,
            SUM(CASE WHEN ll.points_delta < 0 THEN ABS(ll.points_delta) ELSE 0 END) AS deduct_sum,
            COUNT(*)                                    AS tx_count
        FROM loyalty_ledger ll
        WHERE ll.customer_id = :customerId
          AND ll.created_at >= :from
          AND ll.created_at <= :to
          AND ll.transaction_type IN ('EARN','DEDUCT')
        GROUP BY
            YEAR(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                * 100
            + DATEPART(iso_week,
                DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01')
              )
        ORDER BY year_week
        """, nativeQuery = true)
    List<Object[]> findWeeklySummaryByCustomer(
            @Param("customerId") Integer customerId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    // ── Tổng hợp MONTHLY cho 1 khách hàng ─────────────────────────────────────
    /**
     * Trả về list Object[]:
     *  [0] yearMonth (INT, e.g. 202501)
     *  [1] earnSum
     *  [2] deductSum
     *  [3] txCount
     */
    @Query(value = """
        SELECT
            YEAR(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                * 100
            + MONTH(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                                                        AS year_month,
            SUM(CASE WHEN ll.points_delta > 0 THEN ll.points_delta ELSE 0 END) AS earn_sum,
            SUM(CASE WHEN ll.points_delta < 0 THEN ABS(ll.points_delta) ELSE 0 END) AS deduct_sum,
            COUNT(*)                                    AS tx_count
        FROM loyalty_ledger ll
        WHERE ll.customer_id = :customerId
          AND ll.created_at >= :from
          AND ll.created_at <= :to
          AND ll.transaction_type IN ('EARN','DEDUCT')
        GROUP BY
            YEAR(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                * 100
            + MONTH(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
        ORDER BY year_month
        """, nativeQuery = true)
    List<Object[]> findMonthlySummaryByCustomer(
            @Param("customerId") Integer customerId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    // ── Tổng hợp WEEKLY toàn hệ thống (Admin) ─────────────────────────────────
    /**
     * Trả về list Object[]:
     *  [0] yearWeek      (INT)
     *  [1] customerId    (INT)
     *  [2] customerName  (VARCHAR)
     *  [3] customerEmail (VARCHAR)
     *  [4] vipTier       (VARCHAR)
     *  [5] earnSum       (INT)
     *  [6] deductSum     (INT)
     *  [7] txCount       (LONG)
     */
    @Query(value = """
        SELECT
            YEAR(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                * 100
            + DATEPART(iso_week,
                DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01')
              )                                          AS year_week,
            c.id                                        AS customer_id,
            c.name                                      AS customer_name,
            c.email                                     AS customer_email,
            c.vip_tier                                  AS vip_tier,
            SUM(CASE WHEN ll.points_delta > 0 THEN ll.points_delta ELSE 0 END) AS earn_sum,
            SUM(CASE WHEN ll.points_delta < 0 THEN ABS(ll.points_delta) ELSE 0 END) AS deduct_sum,
            COUNT(*)                                    AS tx_count
        FROM loyalty_ledger ll
        INNER JOIN customers c ON ll.customer_id = c.id
        WHERE ll.created_at >= :from
          AND ll.created_at <= :to
          AND ll.transaction_type IN ('EARN','DEDUCT')
        GROUP BY
            YEAR(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                * 100
            + DATEPART(iso_week,
                DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01')
              ),
            c.id, c.name, c.email, c.vip_tier
        ORDER BY year_week, earn_sum DESC
        """, nativeQuery = true)
    List<Object[]> findWeeklySummaryAllCustomers(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    // ── Tổng hợp MONTHLY toàn hệ thống (Admin) ────────────────────────────────
    @Query(value = """
        SELECT
            YEAR(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                * 100
            + MONTH(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                                                        AS year_month,
            c.id                                        AS customer_id,
            c.name                                      AS customer_name,
            c.email                                     AS customer_email,
            c.vip_tier                                  AS vip_tier,
            SUM(CASE WHEN ll.points_delta > 0 THEN ll.points_delta ELSE 0 END) AS earn_sum,
            SUM(CASE WHEN ll.points_delta < 0 THEN ABS(ll.points_delta) ELSE 0 END) AS deduct_sum,
            COUNT(*)                                    AS tx_count
        FROM loyalty_ledger ll
        INNER JOIN customers c ON ll.customer_id = c.id
        WHERE ll.created_at >= :from
          AND ll.created_at <= :to
          AND ll.transaction_type IN ('EARN','DEDUCT')
        GROUP BY
            YEAR(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01'))
                * 100
            + MONTH(DATEADD(SECOND, DATEDIFF(SECOND,'1970-01-01', CAST(ll.created_at AS DATETIME2)), '1970-01-01')),
            c.id, c.name, c.email, c.vip_tier
        ORDER BY year_month, earn_sum DESC
        """, nativeQuery = true)
    List<Object[]> findMonthlySummaryAllCustomers(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    boolean existsByReferenceTypeAndReferenceIdAndTransactionType(
            String referenceType,
            Long referenceId,
            String transactionType
    );

}