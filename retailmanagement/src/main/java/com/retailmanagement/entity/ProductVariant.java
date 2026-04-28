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
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Size(max = 150)
    @NotNull
    @Nationalized
    @Column(name = "variant_name", nullable = false, length = 150)
    private String variantName;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Size(max = 100)
    @Nationalized
    @Column(name = "barcode", length = 100)
    private String barcode;

    @Size(max = 3)
    @NotNull
    @ColumnDefault("'VND'")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @NotNull
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Nationalized
    @Lob
    @Column(name = "price_tiers_json")
    private String priceTiersJson;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty;

    @Nationalized
    @Lob
    @Column(name = "attributes_json")
    private String attributesJson;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @NotNull
    @ColumnDefault("sysdatetime()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @ColumnDefault("sysdatetime()")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "variant")
    private Set<Image> images = new LinkedHashSet<>();

    @OneToMany(mappedBy = "variant")
    private Set<OrderItem> orderItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "variant")
    private Set<PriceHistory> priceHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "variant")
    private Set<StockTransaction> stockTransactions = new LinkedHashSet<>();

}