package com.retailmanagement.controller;

import com.retailmanagement.dto.request.ProductRequest;
import com.retailmanagement.dto.response.ProductResponse;
import com.retailmanagement.repository.OrderItemRepository;
import com.retailmanagement.repository.ProductRepository;
import com.retailmanagement.repository.ProductSerialRepository;
import com.retailmanagement.repository.ProductVariantRepository;
import com.retailmanagement.service.ProductService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Transactional
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) List<Integer> categoryIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(required = false) Integer tagId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean isNew,
            @RequestParam(required = false) Boolean isFaulty,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        return ResponseEntity.ok(productService.getProducts(page, categoryIds, keyword, sortBy, inStockOnly, tagId, startDate, endDate, isNew, isFaulty, brand, minPrice, maxPrice));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@ModelAttribute ProductRequest request) throws IOException {
        productService.createProduct(request);
        return ResponseEntity.ok("Thêm sản phẩm thành công");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @ModelAttribute ProductRequest request) throws IOException {
        productService.updateProduct(id, request);
        return ResponseEntity.ok("Cập nhật sản phẩm thành công");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        productService.softDeleteProduct(id);
        return ResponseEntity.ok("Đã ẩn sản phẩm thành công (Soft Delete)");
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<?> batchDeleteProducts(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("Danh sách ID trống");
        }
        for (Integer id : ids) {
            productService.softDeleteProduct(id);
        }
        return ResponseEntity.ok("Đã xóa (ẩn) hàng loạt thành công");
    }

    @GetMapping("/trash")
    public ResponseEntity<Page<ProductResponse>> getTrash(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(productService.getTrashProducts(page));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restoreProduct(@PathVariable Integer id) {
        productService.restoreProduct(id);
        return ResponseEntity.ok("Khôi phục sản phẩm thành công");
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<?> hardDeleteProduct(@PathVariable Integer id) {
        productService.hardDeleteProduct(id);
        return ResponseEntity.ok("Đã xóa vĩnh viễn sản phẩm");
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<?> setPrimaryImage(@PathVariable Integer id, @PathVariable Long imageId) {
        productService.setPrimaryImage(id, imageId);
        return ResponseEntity.ok("Đã đặt làm ảnh đại diện thành công");
    }

    public static class BatchUpdateRequest {
        public List<Integer> ids;
        public Boolean isVisible;
        public Boolean isNew;
        public Boolean isFaulty;
        public List<Integer> tagIds;
    }

    @PutMapping("/batch-update")
    public ResponseEntity<?> batchUpdateProducts(@RequestBody BatchUpdateRequest request) {
        productService.batchUpdateProducts(
                request.ids,
                request.isVisible,
                request.isNew,
                request.isFaulty,
                request.tagIds
        );
        return ResponseEntity.ok("Cập nhật hàng loạt thành công");
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.retailmanagement.repository.ProductHistoryLogRepository productHistoryLogRepository;
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getProductHistory(@PathVariable Integer id) {
        List<com.retailmanagement.entity.ProductHistoryLog> logs = productHistoryLogRepository.findByTargetEntityAndTargetIdOrderByCreatedAtDesc(
                "PRODUCT",
                String.valueOf(id)
        );

        return ResponseEntity.ok(logs);
    }

    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductVariantRepository variantRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductSerialRepository serialRepository;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getProductDashboardStats() {
        com.retailmanagement.dto.response.DashboardStatsResponse stats = new com.retailmanagement.dto.response.DashboardStatsResponse();

        // 1. Lấy Top 10 sản phẩm bán chạy nhất
        Pageable top10 = PageRequest.of(0, 10);
        stats.setTopSellingProducts(orderItemRepository.getTopSellingProducts(top10));

        // 2. Thống kê số liệu cơ bản
        stats.setTotalActiveProducts(productRepository.countByIsVisibleTrue());
        stats.setOutOfStockVariants(variantRepository.countByStockQuantityLessThanEqual(0));

        // 3. Đếm số Seri (Trong kho và Bị lỗi)
        stats.setTotalSerialsInStock(serialRepository.countByStatus("IN_STOCK"));
        stats.setTotalSerialsFaulty(serialRepository.countByStatus("FAULTY"));

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getBrands() {
        return ResponseEntity.ok(productRepository.findAll().stream()
                .map(com.retailmanagement.entity.Product::getBrand)
                .filter(b -> b != null && !b.trim().isEmpty())
                .distinct()
                .sorted()
                .toList());
    }
}