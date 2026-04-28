package com.retailmanagement.repository;

import com.retailmanagement.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Category> findAllByOrderByDisplayOrderAscNameAsc();
    Page<Category> findByIsActiveTrue(Pageable pageable);
}
