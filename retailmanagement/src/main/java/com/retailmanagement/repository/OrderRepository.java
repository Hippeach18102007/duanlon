package com.retailmanagement.repository;

import com.retailmanagement.dto.response.RevenueByCustomerResponse;
import com.retailmanagement.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusOrderByCreatedAtDesc(String status);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatusInOrderByCreatedAtDesc(List<String> statuses);

    long countByCreatedAtBetween(Instant start, Instant end);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    @Query("""
                SELECT o
                FROM Order o
                WHERE o.createdAt BETWEEN :from AND :to
                ORDER BY o.createdAt DESC
            """)
    List<Order> findOrdersByDateRange(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
                SELECT o
                FROM Order o
                WHERE o.customer.id = :customerId
                  AND o.createdAt BETWEEN :from AND :to
                ORDER BY o.createdAt DESC
            """)
    List<Order> findOrdersByCustomerAndDate(
            @Param("customerId") Integer customerId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
                SELECT new com.retailmanagement.dto.response.RevenueByCustomerResponse(
                    c.id,
                    c.name,
                    SUM(oi.lineTotal)
                )
                FROM Order o
                JOIN o.customer c
                JOIN OrderItem oi ON oi.order = o
                WHERE o.status = 'DELIVERED'
                GROUP BY c.id, c.name
                ORDER BY SUM(oi.lineTotal) DESC
            """)
    List<RevenueByCustomerResponse> getRevenueByCustomer();

    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

    long countByCustomerId(Integer customerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId " +
            "AND o.createdAt > :cutoff " +
            "AND o.status IN :statuses")
    long countByCustomerIdAndCreatedAtAfterAndStatusIn(
            @Param("customerId") Integer customerId,
            @Param("cutoff") Instant cutoff,
            @Param("statuses") List<String> statuses
    );

    long countByCustomerIdAndStatus(Integer customerId, String status);

    @Query(value = """
            SELECT channel, SUM(total_amount) AS revenue
            FROM orders
            WHERE status = 'DELIVERED'
            GROUP BY channel
            """, nativeQuery = true)
    List<Object[]> revenueByChannel();

    /**
     * Doanh thu theo ngày — SQL Server dùng CAST(col AS date) ✔
     */
    @Query(value = """
            SELECT CAST(created_at AS date)  AS date,
                   SUM(total_amount)         AS revenue
            FROM orders
            WHERE status = 'DELIVERED'
            GROUP BY CAST(created_at AS date)
            ORDER BY CAST(created_at AS date)
            """, nativeQuery = true)
    List<Object[]> revenueByDate();

    @Query(value = """
            SELECT u.username, COUNT(o.id) AS orders
            FROM orders o
            JOIN users u ON o.user_id = u.id
            WHERE u.role_id = 2
            GROUP BY u.username
            ORDER BY COUNT(o.id) DESC
            """, nativeQuery = true)
    List<Object[]> ordersByStaff();

    List<Order> findByAppliedPromotionCodeOrderByCreatedAtDesc(String appliedPromotionCode);

    /**
     * Tổng hợp doanh thu theo trạng thái đơn hàng.
     * Trả về: [status, orderCount, totalRevenue]
     */
    @Query(value = """
            SELECT status,
                   COUNT(id)         AS orderCount,
                   SUM(total_amount) AS totalRevenue
            FROM orders
            GROUP BY status
            ORDER BY COUNT(id) DESC
            """, nativeQuery = true)
    List<Object[]> revenueByStatus();

    // ── THÊM MỚI (SQL Server) ─────────────────────────────────────────────

    /**
     * Doanh thu theo tuần (DELIVERED) → [week_start, revenue]
     * DATEADD(week, DATEDIFF(week, 0, col), 0) trả về thứ Hai đầu tuần
     */
    @Query(value = """
            SELECT DATEADD(week, DATEDIFF(week, 0, created_at), 0) AS week_start,
                   SUM(total_amount)                               AS revenue
            FROM orders
            WHERE status = 'DELIVERED'
            GROUP BY DATEADD(week, DATEDIFF(week, 0, created_at), 0)
            ORDER BY DATEADD(week, DATEDIFF(week, 0, created_at), 0)
            """, nativeQuery = true)
    List<Object[]> revenueByWeek();

    /**
     * Doanh thu theo tháng (DELIVERED) → [month_start, revenue]
     * DATEADD(month, DATEDIFF(month, 0, col), 0) trả về ngày 1 của tháng
     */
    @Query(value = """
            SELECT DATEADD(month, DATEDIFF(month, 0, created_at), 0) AS month_start,
                   SUM(total_amount)                                  AS revenue
            FROM orders
            WHERE status = 'DELIVERED'
            GROUP BY DATEADD(month, DATEDIFF(month, 0, created_at), 0)
            ORDER BY DATEADD(month, DATEDIFF(month, 0, created_at), 0)
            """, nativeQuery = true)
    List<Object[]> revenueByMonth();

    /**
     * Top 15 sản phẩm bán chạy theo số lượng (DELIVERED).
     * order_items đã có sẵn product_name, variant_name, sku → không cần JOIN.
     * Trả về: [productName, sku, variantName, totalQty, totalRevenue]
     */
    @Query(value = """
            SELECT TOP 15
                   oi.product_name             AS productName,
                   oi.sku                      AS sku,
                   oi.variant_name             AS variantName,
                   SUM(oi.quantity)            AS totalQty,
                   SUM(oi.line_total)          AS totalRevenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.status = 'DELIVERED'
            GROUP BY oi.product_name, oi.sku, oi.variant_name
            ORDER BY SUM(oi.quantity) DESC
            """, nativeQuery = true)
    List<Object[]> topSellingProducts();

    /**
     * Tìm tất cả đơn SHIPPING chưa được cập nhật trong hơn N ngày.
     * Dùng {@code updatedAt} để tính ngưỡng thời gian.
     *
     * @param status  OrderStatuses.SHIPPING
     * @param cutoff  Instant.now().minusSeconds(days * 86_400)
     */
    @Query("""
    SELECT o FROM Order o
    WHERE o.status = :status
      AND o.updatedAt < :cutoff
    ORDER BY o.updatedAt ASC
""")
    List<Order> findShippingOrdersOverdue(
            @Param("status") String status,
            @Param("cutoff") Instant cutoff
    );

    /**
     * Tìm đơn SHIPPING sắp bị auto-complete để gửi email cảnh báo.
     * Điều kiện: updatedAt nằm trong khoảng [cutoff, upperBound)
     * — tức là đã quá (N-1) ngày nhưng chưa đủ N ngày.
     *
     * @param cutoff      Instant.now().minusSeconds((days-1) * 86_400)
     * @param upperBound  Instant.now().minusSeconds((days-2) * 86_400)
     */
    @Query("""
    SELECT o FROM Order o
    WHERE o.status = 'SHIPPING'
      AND o.updatedAt < :cutoff
      AND o.updatedAt >= :upperBound
    ORDER BY o.updatedAt ASC
""")
    List<Order> findShippingOrdersToWarn(
            @Param("cutoff")      Instant cutoff,
            @Param("upperBound")  Instant upperBound
    );

}