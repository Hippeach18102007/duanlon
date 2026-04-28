package com.retailmanagement.controller;

import com.retailmanagement.dto.request.ProductVariantRequest;
import com.retailmanagement.dto.response.ProductVariantResponse;
import com.retailmanagement.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    // 1. Lấy tất cả biến thể của 1 sản phẩm
    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantResponse>> getVariantsByProduct(@PathVariable Integer productId) {
        return ResponseEntity.ok(productVariantService.getVariantsByProductId(productId));
    }

    // 2. Lấy chi tiết 1 biến thể
    @GetMapping("/variants/{id}")
    public ResponseEntity<ProductVariantResponse> getVariantById(@PathVariable Integer id) {
        return ResponseEntity.ok(productVariantService.getVariantById(id));
    }

    // 3. Thêm biến thể mới cho 1 sản phẩm
    @PostMapping("/{productId}/variants")
    public ResponseEntity<ProductVariantResponse> createVariant(
            @PathVariable Integer productId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(productVariantService.createVariant(productId, request));
    }

    // 4. Sửa biến thể
    @PutMapping("/variants/{id}")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @PathVariable Integer id,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(productVariantService.updateVariant(id, request));
    }

    // 5. Xóa biến thể
    @DeleteMapping("/variants/{id}")
    public ResponseEntity<String> deleteVariant(@PathVariable Integer id) {
        productVariantService.deleteVariant(id);
        return ResponseEntity.ok("Xóa biến thể thành công");
    }
}