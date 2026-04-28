package com.retailmanagement.service;

import com.retailmanagement.dto.response.ProductImportResult;
import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Import sản phẩm hàng loạt từ file Excel (.xlsx)
 *
 * Cấu trúc Excel (bắt đầu đọc từ row 3 — row 1: header, row 2: hint):
 * | name* | sku* | description | isNew | categoryIds | tagIds |
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final ProductRepository        productRepository;
    private final CategoryRepository       categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final TagRepository            tagRepository;
    private final ProductTagRepository     productTagRepository;

    // ──────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public ProductImportResult importFromExcel(MultipartFile file) {
        List<String> errors      = new ArrayList<>();
        int          successCount = 0;
        int          totalRows   = 0;

        try (InputStream is = file.getInputStream();
             Workbook wb   = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);

            // Row 1 = header (index 0), Row 2 = hint (index 1)
            // Data bắt đầu từ index 2
            int firstDataRow = 2;
            int lastRow      = sheet.getLastRowNum();

            for (int i = firstDataRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                totalRows++;
                int displayRow = i + 1; // 1-based cho user

                try {
                    String name = getCellString(row, 0);
                    String sku  = getCellString(row, 1);

                    // Validate bắt buộc
                    if (name.isBlank()) {
                        errors.add("Dòng " + displayRow + ": Tên sản phẩm không được để trống");
                        continue;
                    }
                    if (sku.isBlank()) {
                        errors.add("Dòng " + displayRow + ": SKU không được để trống");
                        continue;
                    }

                    String skuUpper = sku.trim().toUpperCase();

                    // Kiểm tra SKU trùng trong DB
                    if (productRepository.existsBySku(skuUpper)) {
                        errors.add("Dòng " + displayRow + ": SKU '" + skuUpper + "' đã tồn tại");
                        continue;
                    }

                    String  description = getCellString(row, 2);
                    boolean isNew       = getCellBoolean(row, 3, true);
                    String  catIdsRaw   = getCellString(row, 4);
                    String  tagIdsRaw   = getCellString(row, 5);

                    // Tạo sản phẩm
                    Product product = new Product();
                    product.setName(name.trim());
                    product.setSku(skuUpper);
                    product.setDescription(description.isBlank() ? null : description.trim());
                    product.setIsNew(isNew);
                    product.setIsFaulty(false);
                    product.setIsVisible(true);
                    product.setSoldCount(0);
                    product.setCreatedAt(LocalDateTime.now());
                    product.setUpdatedAt(LocalDateTime.now());

                    Product saved = productRepository.save(product);

                    // Gắn danh mục
                    if (!catIdsRaw.isBlank()) {
                        for (String idStr : catIdsRaw.split(";")) {
                            idStr = idStr.trim();
                            if (idStr.isEmpty()) continue;
                            try {
                                Integer catId = Integer.parseInt(idStr);
                                if (categoryRepository.existsById(catId)) {
                                    ProductCategory pc  = new ProductCategory();
                                    ProductCategoryId pcId = new ProductCategoryId(saved.getId(), catId);
                                    pc.setId(pcId);
                                    pc.setCategory(categoryRepository.getReferenceById(catId));
                                    pc.setIsPrimary(false);
                                    pc.setCreatedAt(Instant.now());
                                    productCategoryRepository.save(pc);
                                }
                            } catch (NumberFormatException ignored) {
                                log.warn("Dòng {}: categoryId không hợp lệ '{}'", displayRow, idStr);
                            }
                        }
                    }

                    // Gắn tags
                    if (!tagIdsRaw.isBlank()) {
                        for (String idStr : tagIdsRaw.split(";")) {
                            idStr = idStr.trim();
                            if (idStr.isEmpty()) continue;
                            try {
                                Integer tagId = Integer.parseInt(idStr);
                                if (tagRepository.existsById(tagId)) {
                                    ProductTag   pt   = new ProductTag();
                                    ProductTagId ptId = new ProductTagId();
                                    ptId.setProductId(saved.getId());
                                    ptId.setTagId(tagId);
                                    pt.setId(ptId);
                                    pt.setProduct(saved);
                                    pt.setTag(tagRepository.getReferenceById(tagId));
                                    pt.setCreatedAt(Instant.now());
                                    productTagRepository.save(pt);
                                }
                            } catch (NumberFormatException ignored) {
                                log.warn("Dòng {}: tagId không hợp lệ '{}'", displayRow, idStr);
                            }
                        }
                    }

                    successCount++;

                } catch (Exception e) {
                    log.error("Lỗi import dòng {}", i + 1, e);
                    errors.add("Dòng " + displayRow + ": Lỗi không xác định — " + e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Không thể đọc file Excel", e);
            errors.add("Không thể đọc file: " + e.getMessage());
        }

        return ProductImportResult.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failCount(totalRows - successCount)
                .errors(errors)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                // Tránh ".0" khi đọc số nguyên
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception ex) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private boolean getCellBoolean(Row row, int col, boolean defaultValue) {
        String val = getCellString(row, col).toLowerCase();
        if (val.isBlank()) return defaultValue;
        return val.equals("true") || val.equals("1") || val.equals("yes") || val.equals("có");
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < 6; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String v = getCellString(row, i);
                if (!v.isBlank()) return false;
            }
        }
        return true;
    }
}