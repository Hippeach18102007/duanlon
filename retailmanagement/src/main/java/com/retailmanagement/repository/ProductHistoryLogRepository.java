package com.retailmanagement.repository;

import com.retailmanagement.entity.ProductHistoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductHistoryLogRepository extends JpaRepository<ProductHistoryLog, Integer> {
    List<ProductHistoryLog> findByTargetEntityAndTargetIdOrderByCreatedAtDesc(String targetEntity, String targetId);
}