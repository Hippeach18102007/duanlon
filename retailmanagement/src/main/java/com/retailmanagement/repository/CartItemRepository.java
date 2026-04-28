package com.retailmanagement.repository;

import com.retailmanagement.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    Optional<CartItem> findByCustomerIdAndProductVariantId(
            Integer customerId,
            Integer productVariantId
    );

    List<CartItem> findByCustomerId(Integer customerId);

    long countByCustomerId(Integer customerId);

    void deleteByCustomerId(Integer customerId);
}
