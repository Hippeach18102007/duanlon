package com.retailmanagement.controller;

import com.retailmanagement.dto.response.ProductImportResult;
import com.retailmanagement.service.ProductImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Endpoints:
 *   POST /api/products/import          → Upload Excel, trả về kết quả
 *   GET  /api/products/import/template → Tải file Excel mẫu
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductImportController {

    private final ProductImportService importService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImportResult> importProducts(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest().build();
        }

        ProductImportResult result = importService.importFromExcel(file);
        return ResponseEntity.ok(result);
    }

    /**
     * Trả về file Excel template đặt tại:
     * src/main/resources/templates/product_import_template.xlsx
     */
    @GetMapping("/import/template")
    public ResponseEntity<Resource> downloadTemplate() {
        try {
            Path path = Paths.get("uploads/product_import_template.xlsx");
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"product_import_template.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}