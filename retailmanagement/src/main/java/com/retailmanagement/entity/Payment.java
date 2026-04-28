package com.retailmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "payments")
@Builder
@NoArgsConstructor
@AllArgsConstructor
// ✅ Hibernate will automatically filter out soft-deleted records
@Where(clause = "deleted_at IS NULL")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "method", length = 50, nullable = false)
    private String method;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @NotNull
    @Column(name = "status", length = 20, nullable = false)
    private String status; // PENDING, SUCCESS, FAILED, REFUNDED

    @Column(name = "paid_at")
    private Instant paidAt;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // ============================================================
    // SOFT DELETE FIELDS
    // ============================================================

    /**
     * Timestamp when this payment was soft-deleted
     * NULL = active (not deleted)
     * NOT NULL = soft-deleted
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * User who performed the soft delete
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Check if this payment is soft-deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Check if this payment is active (not deleted)
     */
    public boolean isActive() {
        return deletedAt == null;
    }

    /**
     * Soft delete this payment
     */
    public void softDelete(User deletedByUser) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUser;
    }

    /**
     * Restore this soft-deleted payment
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
    }
}