package com.retailmanagement.repository;

import com.retailmanagement.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // ============================================================
    // ORDER QUERIES
    // ============================================================

    /** Active payments by order (filtered by @Where) */
    List<Payment> findByOrderId(Long orderId);

    /** Active payments by order + status */
    List<Payment> findByOrderIdAndStatus(Long orderId, String status);

    /** All payments by order including deleted (native bypasses @Where) */
    @Query(value = "SELECT * FROM payments WHERE order_id = :orderId ORDER BY created_at DESC",
            nativeQuery = true)
    List<Payment> findAllByOrderIdIncludingDeleted(@Param("orderId") Long orderId);

    /** Only deleted payments by order */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.deletedAt IS NOT NULL ORDER BY p.deletedAt DESC")
    List<Payment> findDeletedByOrderId(@Param("orderId") Long orderId);

    // ============================================================
    // CUSTOMER QUERIES
    // ============================================================

    /** Active payments by customer (deleted_at IS NULL) */
    @Query("SELECT p FROM Payment p WHERE p.order.customer.id = :customerId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Payment> findActiveByCustomerId(@Param("customerId") Integer customerId);

    /** All payments by customer including deleted (native bypasses @Where) */
    @Query(value = "SELECT p.* FROM payments p JOIN orders o ON p.order_id = o.id WHERE o.customer_id = :customerId ORDER BY p.created_at DESC",
            nativeQuery = true)
    List<Payment> findAllByCustomerIdIncludingDeleted(@Param("customerId") Integer customerId);

    /** Only deleted payments by customer */
    @Query("SELECT p FROM Payment p WHERE p.order.customer.id = :customerId AND p.deletedAt IS NOT NULL ORDER BY p.deletedAt DESC")
    List<Payment> findDeletedByCustomerId(@Param("customerId") Integer customerId);

    /** Count active payments by customer */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.order.customer.id = :customerId AND p.deletedAt IS NULL")
    long countActiveByCustomerId(@Param("customerId") Integer customerId);

    /** Count deleted payments by customer */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.order.customer.id = :customerId AND p.deletedAt IS NOT NULL")
    long countDeletedByCustomerId(@Param("customerId") Integer customerId);

    /** Total amount paid by customer (active SUCCESS payments only) */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.order.customer.id = :customerId AND p.status = 'SUCCESS' AND p.deletedAt IS NULL")
    BigDecimal getTotalAmountByCustomerId(@Param("customerId") Integer customerId);

    // ============================================================
    // SINGLE PAYMENT QUERIES
    // ============================================================

    /** Find active payment by ID */
    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Payment> findActiveById(@Param("id") Long id);

    /** Find deleted payment by ID (native bypasses @Where) */
    @Query(value = "SELECT * FROM payments WHERE id = :id AND deleted_at IS NOT NULL",
            nativeQuery = true)
    Optional<Payment> findDeletedById(@Param("id") Long id);

    // ============================================================
    // GLOBAL SOFT DELETE QUERIES
    // ============================================================

    /** All soft-deleted payments */
    @Query("SELECT p FROM Payment p WHERE p.deletedAt IS NOT NULL ORDER BY p.deletedAt DESC")
    List<Payment> findAllDeleted();

    /** Count all active payments */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.deletedAt IS NULL")
    long countActive();

    /** Count all deleted payments */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.deletedAt IS NOT NULL")
    long countDeleted();

    /** Count active payments created between dates */
    long countByCreatedAtBetween(Instant start, Instant end);

    // ============================================================
    // BULK OPERATIONS
    // ============================================================

    /** Soft delete payments older than date */
    @Modifying
    @Query("UPDATE Payment p SET p.deletedAt = :now, p.deletedBy = (SELECT u FROM User u WHERE u.id = :userId) WHERE p.createdAt < :beforeDate AND p.deletedAt IS NULL")
    int softDeleteOlderThan(@Param("beforeDate") Instant beforeDate,
                            @Param("userId") Integer userId,
                            @Param("now") Instant now);

    /** Soft delete REFUNDED payments older than date */
    @Modifying
    @Query("UPDATE Payment p SET p.deletedAt = :now, p.deletedBy = (SELECT u FROM User u WHERE u.id = :userId) WHERE p.status = 'REFUNDED' AND p.createdAt < :beforeDate AND p.deletedAt IS NULL")
    int softDeleteRefundedOlderThan(@Param("beforeDate") Instant beforeDate,
                                    @Param("userId") Integer userId,
                                    @Param("now") Instant now);

    /** Permanently delete soft-deleted payments older than date */
    @Modifying
    @Query("DELETE FROM Payment p WHERE p.deletedAt IS NOT NULL AND p.deletedAt < :beforeDate")
    int permanentlyDeleteOlderThan(@Param("beforeDate") Instant beforeDate);
}