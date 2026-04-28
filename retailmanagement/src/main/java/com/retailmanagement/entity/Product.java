package com.retailmanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "attributes_json", columnDefinition = "nvarchar(max)")
    private String attributesJson;

    @Column(name = "sold_count")
    private Integer soldCount;

    @Column(name = "is_new")
    private Boolean isNew = true;

    @Column(name = "is_faulty")
    private Boolean isFaulty = false;

    @Column(name = "brand", length = 100)
    private String brand;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Image> images;
}
