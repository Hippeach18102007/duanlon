package com.retailmanagement.repository;

import com.retailmanagement.dto.response.BestSellingProductDTO;
import org.springframework.data.domain.Pageable;
import com.retailmanagement.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT new com.retailmanagement.dto.response.BestSellingProductDTO(" +
            "p.id, p.name, SUM(oi.quantity), SUM(oi.lineTotal)) " +
            "FROM OrderItem oi JOIN oi.product p JOIN oi.order o " +
            "WHERE o.status != 'CANCELLED' AND o.status != 'RETURNED' " +
            "GROUP BY p.id, p.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<BestSellingProductDTO> getTopSellingProducts(Pageable pageable);
}

