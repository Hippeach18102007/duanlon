package com.retailmanagement.repository;

import com.retailmanagement.entity.PriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    // Dùng cho deleteLatestAndRollback — cần toàn bộ list để rollback
    List<PriceHistory> findByVariant_IdOrderByEffectiveFromDesc(Integer variantId);

    // Dùng cho phân trang trong getPriceHistory
    Page<PriceHistory> findByVariant_IdOrderByEffectiveFromDesc(Integer variantId, Pageable pageable);

    // Dùng để tìm bản ghi đang active khi set giá mới
    Optional<PriceHistory> findFirstByVariant_IdAndEffectiveToIsNullOrderByEffectiveFromDesc(Integer variantId);
}