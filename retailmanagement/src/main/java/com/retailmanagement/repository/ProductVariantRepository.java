package com.retailmanagement.repository;

import com.retailmanagement.dto.response.ProductSummaryDto;
import com.retailmanagement.entity.ProductVariant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    List<ProductVariant> findByProduct_Id(Integer productId);

    // Thay thế findAll() trong getAllConflictsBelowCost() và getDashboard()
    // Chỉ lấy các variant có giá bán thực sự thấp hơn giá nhập
    @Query("SELECT v FROM ProductVariant v WHERE v.costPrice IS NOT NULL AND v.price IS NOT NULL AND v.price < v.costPrice")
    List<ProductVariant> findVariantsBelowCost();
    // Đếm số lượng biến thể có tồn kho = 0
    long countByStockQuantityLessThanEqual(Integer stockQuantity);
    // ProductVariantRepository.java - thêm method
    @Query("""
    SELECT new com.retailmanagement.dto.response.ProductSummaryDto(
        p.name, p.sku, p.brand, p.description,
        c.name,
        MIN(v.price), MAX(v.price),
        SUM(CAST(v.stockQuantity AS long) - CAST(v.reservedQty AS long)),
        p.isNew
    )
    FROM ProductVariant v
    JOIN v.product p
    LEFT JOIN ProductCategory pc ON pc.id.productId = p.id AND pc.isPrimary = true
    LEFT JOIN pc.category c
    WHERE v.isActive = true
      AND p.isVisible = true
      AND v.deletedAt IS NULL
      AND (:keyword IS NULL
           OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')))
    GROUP BY p.id, p.name, p.sku, p.brand, p.description, c.name, p.isNew, p.soldCount
    ORDER BY p.soldCount DESC
""")
    List<ProductSummaryDto> searchForAI(@Param("keyword") String keyword, Pageable pageable);
}