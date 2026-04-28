package com.retailmanagement.repository;

import com.retailmanagement.entity.ProductSerial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSerialRepository extends JpaRepository<ProductSerial, Long> {
    // Lấy toàn bộ số Seri của 1 Biến thể cụ thể
    List<ProductSerial> findByVariantId(Integer variantId);
    Optional<ProductSerial> findBySerialNumber(String serialNumber);

    // Đếm số lượng máy thực tế đang ở trong kho của 1 Biến thể
    int countByVariantIdAndStatus(Integer variantId, String status);
    // Xóa toàn bộ Seri thuộc về 1 biến thể (khi xóa biến thể)
    void deleteByVariantId(Integer variantId);
    boolean existsBySerialNumber(String serialNumber);
    List<ProductSerial> findByVariantIdAndStatus(Integer variantId, String status);
    // Tìm kiếm seri theo số seri hoặc tên sản phẩm
    @Query("SELECT s FROM ProductSerial s WHERE " +
            "(:keyword IS NULL OR s.serialNumber LIKE %:keyword%) AND " +
            "(:status IS NULL OR s.status = :status)")
    Page<ProductSerial> searchSerials(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);

    long countByStatus(String status);
}
