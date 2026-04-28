package com.retailmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Return {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String reason;

    @Column(name = "refund_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "refund_method", length = 50)
    private String refundMethod;

    @Column(name = "refund_status", length = 20, nullable = false)
    private String refundStatus = "PENDING";

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(length = 20, nullable = false)
    private String status = "PENDING";

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;

    // ✅ CHỈ LƯU ID, KHÔNG DÙNG @ManyToOne
    @Column(name = "processed_by")
    private Integer processedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "image_url", length = 800)
    private String imageUrl;
}