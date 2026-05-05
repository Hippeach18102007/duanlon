package com.retailmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 40)
    @NotNull
    @Nationalized
    @ColumnDefault("concat(N'ORD-', CONVERT([char](8), sysdatetime(), 112), N'-', right(N'000000'+CONVERT([varchar](20), NEXT VALUE FOR [dbo].[order_seq]), 6))")
    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    private User user;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @ColumnDefault("'OFFLINE'")
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Size(max = 30)
    @NotNull
    @Nationalized
    @ColumnDefault("'PENDING'")
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Size(max = 50)
    @Nationalized
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @ColumnDefault("'UNPAID'")
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Size(max = 500)
    @Nationalized
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Size(max = 30)
    @Nationalized
    @Column(name = "shipping_status", length = 30)
    private String shippingStatus;

    @Size(max = 100)
    @Nationalized
    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "discount_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountTotal;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "tax_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxTotal;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Size(max = 50)
    @Nationalized
    @Column(name = "applied_promotion_code", length = 50)
    private String appliedPromotionCode;

    @Nationalized
    @Lob
    @Column(name = "applied_promotion_json")
    private String appliedPromotionJson;

    @Nationalized
    @Lob
    @Column(name = "notes")
    private String notes;

    @NotNull
    @ColumnDefault("sysdatetime()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @ColumnDefault("0")
    @Column(name = "spin_discount_rate", precision = 5, scale = 2)
    private BigDecimal spinDiscountRate = BigDecimal.ZERO;

    @ColumnDefault("0")
    @Column(name = "spin_discount", precision = 15, scale = 2)
    private BigDecimal spinDiscount = BigDecimal.ZERO;

    @ColumnDefault("0")
    @Column(name = "vip_discount_rate", precision = 5, scale = 2)
    private BigDecimal vipDiscountRate = BigDecimal.ZERO;

    @ColumnDefault("0")
    @Column(name = "vip_discount", precision = 15, scale = 2)
    private BigDecimal vipDiscount = BigDecimal.ZERO;
    @NotNull
    @ColumnDefault("sysdatetime()")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Nationalized
    @Lob
    @Column(name = "delivery_proof_url")
    private String deliveryProofUrl;

    @Column(name = "shipper_confirmed_at")
    private Instant shipperConfirmedAt;

    @OneToMany(mappedBy = "order")
    private Set<OrderItem> orderItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order")
    private Set<Payment> payments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order")
    private Set<Return> returns = new LinkedHashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (paymentStatus == null) {
            paymentStatus = "UNPAID";
        }
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}