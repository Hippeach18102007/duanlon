package com.retailmanagement.controller;

import com.retailmanagement.dto.request.ProductSerialRequest;
import com.retailmanagement.entity.ProductSerial;
import com.retailmanagement.entity.ProductVariant;
import com.retailmanagement.repository.ProductSerialRepository;
import com.retailmanagement.repository.ProductVariantRepository;
import com.retailmanagement.service.ProductSerialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products/variants")
@RequiredArgsConstructor
public class ProductSerialController {

    private final ProductSerialRepository serialRepository;
    private final ProductSerialService productSerialService;
    private final ProductVariantRepository variantRepository;

    @GetMapping("/{variantId}/serials")
    public ResponseEntity<List<ProductSerial>> getSerialsByVariant(@PathVariable Integer variantId) {
        return ResponseEntity.ok(serialRepository.findByVariantId(variantId));
    }

    @PostMapping("/{variantId}/serials")
    @Transactional
    public ResponseEntity<?> addSerialsToVariant(
            @PathVariable Integer variantId,
            @Valid @RequestBody ProductSerialRequest request) {

        List<ProductSerial> newSerials = new ArrayList<>();
        List<String> duplicateSerials = new ArrayList<>();

        for (String sn : request.getSerialNumbers()) {
            if(sn == null || sn.trim().isEmpty()) continue;

            // Xóa khoảng trắng 2 đầu và ép viết hoa
            String cleanedSn = sn.trim().toUpperCase();

            // Định dạng Seri Laptop: Chỉ A-Z, 0-9, dấu gạch ngang, dài từ 5-30 ký tự
            if (!cleanedSn.matches("^[A-Z0-9-]{5,30}$")) {
                return ResponseEntity.badRequest().body("Seri '" + cleanedSn + "' sai định dạng! Seri laptop chỉ gồm chữ IN HOA, số, dấu gạch ngang (5-30 ký tự).");
            }

            // Kiểm tra trùng lặp Seri trong kho
            if (serialRepository.existsBySerialNumber(cleanedSn)) {
                duplicateSerials.add(cleanedSn);
                continue;
            }

            ProductSerial serial = new ProductSerial();
            serial.setVariantId(variantId);
            serial.setSerialNumber(cleanedSn);
            serial.setStatus("IN_STOCK");
            newSerials.add(serial);
        }

        // Nếu nhập 1 cái mà trùng luôn cái đó
        if (newSerials.isEmpty() && !duplicateSerials.isEmpty()) {
            return ResponseEntity.badRequest().body("Số Seri '" + duplicateSerials.get(0) + "' đã tồn tại trong hệ thống.");
        }

        try {
            serialRepository.saveAllAndFlush(newSerials);
            ProductVariant variant = variantRepository.findById(variantId).orElse(null);
            if (variant != null) {
                int currentStock = serialRepository.countByVariantIdAndStatus(variantId, "IN_STOCK");
                variant.setStockQuantity(currentStock);
                variant.setIsActive(currentStock > 0);
                variantRepository.save(variant);
            }

            if (!duplicateSerials.isEmpty()) {
                return ResponseEntity.ok("Đã thêm " + newSerials.size() + " Seri. Bỏ qua " + duplicateSerials.size() + " Seri bị trùng.");
            }
            return ResponseEntity.ok("Đã thêm thành công " + newSerials.size() + " số Seri vào kho.");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi hệ thống khi lưu Seri: " + e.getMessage());
        }
    }

    @DeleteMapping("/serials/{serialId}")
    public ResponseEntity<String> deleteSerial(@PathVariable Long serialId) {
        ProductSerial serial = serialRepository.findById(serialId).orElse(null);
        if (serial != null) {
            serialRepository.deleteById(serialId);
            ProductVariant variant = variantRepository.findById(serial.getVariantId()).orElse(null);
            if (variant != null) {
                int currentStock = serialRepository.countByVariantIdAndStatus(variant.getId(), "IN_STOCK");
                variant.setStockQuantity(currentStock);
                variant.setIsActive(currentStock > 0);
                variantRepository.save(variant);
            }
        }
        return ResponseEntity.ok("Đã xóa số Seri thành công.");
    }

    // Endpoint mới để Import Excel
    @PostMapping("/{variantId}/serials/import")
    public ResponseEntity<?> importSerials(
            @PathVariable Integer variantId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File trống."));
        }

        Map<String, Object> result = productSerialService.importFromExcel(variantId, file);
        int successCount = (int) result.get("successCount");
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");

        // Trường hợp 1: Tạch 100% (tất cả đều trùng)
        if (successCount == 0 && !errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Nhập thất bại! Tất cả các Seri trong file đều đã tồn tại.",
                    "errors", errors
            ));
        }

        // Trường hợp 2: Trùng một phần, thành công một phần
        if (successCount > 0 && !errors.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "Thêm mới " + successCount + " mã. Bỏ qua " + errors.size() + " mã bị trùng.",
                    "errors", errors,
                    "errorCount", errors.size()
            ));
        }

        // Trường hợp 3: Thành công 100%
        return ResponseEntity.ok(Map.of(
                "message", "Nhập Excel thành công! Đã thêm " + successCount + " mã mới.",
                "errors", new ArrayList<>(),
                "errorCount", 0
        ));
    }

    @GetMapping("/serials/all")
    public ResponseEntity<?> getAllSerialsGlobal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        if (keyword != null && keyword.trim().isEmpty()) keyword = null;
        if (status != null && status.trim().isEmpty()) status = null;

        Pageable pageable = PageRequest.of(page, 20, Sort.by("id").descending());
        Page<ProductSerial> serialPage = serialRepository.searchSerials(keyword, status, pageable);

        List<Map<String, Object>> content = serialPage.getContent().stream().map(s -> {
            ProductVariant v = variantRepository.findById(s.getVariantId()).orElse(null);
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", s.getId());
            item.put("serialNumber", s.getSerialNumber());
            item.put("status", s.getStatus());
            item.put("createdAt", s.getCreatedAt());
            item.put("variantName", v != null ? v.getVariantName() : "N/A");
            item.put("productName", (v != null && v.getProduct() != null) ? v.getProduct().getName() : "Không xác định");
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(new PageImpl<>(content, pageable, serialPage.getTotalElements()));
    }

    @PutMapping("/serials/{id}/status")
    public ResponseEntity<?> updateSerialStatus(@PathVariable Long id, @RequestParam String status) {
        ProductSerial serial = serialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy số Seri ID: " + id));
        serial.setStatus(status);
        serialRepository.save(serial);
        ProductVariant variant = variantRepository.findById(serial.getVariantId()).orElse(null);
        if (variant != null) {
            int currentStock = serialRepository.countByVariantIdAndStatus(variant.getId(), "IN_STOCK");
            variant.setStockQuantity(currentStock);
            if (currentStock <= 0) {
                variant.setIsActive(false);
            } else {
                variant.setIsActive(true);
            }
            variantRepository.save(variant);
        }
        return ResponseEntity.ok("Đã cập nhật trạng thái Seri thành " + status + " và đồng bộ tồn kho tự động!");
    }
}