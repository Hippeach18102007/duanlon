package com.retailmanagement.repository;

import com.retailmanagement.entity.Return;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {

    List<Return> findByStatusOrderByCreatedAtDesc(String status);

    List<Return> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Return> findByOrder_Customer_IdOrderByCreatedAtDesc(Integer customerId);

    /**
     * Thống kê trả hàng theo sản phẩm (chỉ tính các return không bị REJECTED).
     * Trả về: [productName, sku, variantName, returnCount, totalQty, totalRefund]
     */
    @Query("""
            SELECT oi.productName,
                   oi.sku,
                   oi.variantName,
                   COUNT(r.id),
                   SUM(r.quantity),
                   SUM(r.refundAmount)
            FROM Return r
            JOIN r.orderItem oi
            WHERE r.status <> 'REJECTED'
            GROUP BY oi.productName, oi.sku, oi.variantName
            ORDER BY COUNT(r.id) DESC
            """)
    List<Object[]> returnStatsByProduct();
}