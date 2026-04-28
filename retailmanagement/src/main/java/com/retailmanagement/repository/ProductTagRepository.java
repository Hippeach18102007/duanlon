package com.retailmanagement.repository;

import com.retailmanagement.entity.ProductTag;
import com.retailmanagement.entity.ProductTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, ProductTagId> {
    List<ProductTag> findByProduct_Id(Integer productId);
    void deleteByProduct_Id(Integer productId);
}