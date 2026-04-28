package com.retailmanagement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "spin_wheel_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpinWheelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "discount_bonus", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountBonus; // Percentage (e.g., 5.00 for 5%)

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate; // Monday of the week

    @Column(name = "spun_at", nullable = false)
    private LocalDateTime spunAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 1 week from spin

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (spunAt == null) {
            spunAt = LocalDateTime.now();
        }
    }
}