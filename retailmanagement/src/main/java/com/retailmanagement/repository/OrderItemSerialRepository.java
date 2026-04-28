package com.retailmanagement.repository;

import com.retailmanagement.entity.OrderItem;
import com.retailmanagement.entity.OrderItemSerial;
import com.retailmanagement.entity.ProductSerial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemSerialRepository extends JpaRepository<OrderItemSerial, Long> {
    List<OrderItemSerial> findByOrderItem(OrderItem orderItem);
    Optional<OrderItemSerial> findByProductSerial(ProductSerial serial);
    List<OrderItemSerial> findByOrderItem_IdAndReturnedFalseOrderByIdAsc(Long orderItemId);
}
