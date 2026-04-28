package com.retailmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks per-customer promotion usage.
 * Each customer can only use a specific promotion code ONCE.
 */
@Entity
@Table(
        name = "promotion_customer_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_promo_customer",
                columnNames = {"promotion_id", "customer_id"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCustomerUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Integer promotionId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @PrePersist
    public void prePersist() {
        if (usedAt == null) usedAt = LocalDateTime.now();
    }
}