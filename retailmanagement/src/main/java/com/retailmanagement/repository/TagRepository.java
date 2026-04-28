package com.retailmanagement.repository;

import com.retailmanagement.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Integer> {
    List<Tag> findByIsActiveTrue();
}
