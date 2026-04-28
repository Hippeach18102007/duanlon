package com.retailmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "order_item_serials")
public class OrderItemSerial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_serial_id", nullable = false, unique = true)
    private ProductSerial productSerial;

    @NotNull
    @ColumnDefault("sysdatetime()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "returned", nullable = false)
    @ColumnDefault("0")
    private Boolean returned = false;

    @PrePersist
    protected void onCreate() {

        createdAt = Instant.now();

        if (returned == null) {
            returned = false;
        }
    }


}
