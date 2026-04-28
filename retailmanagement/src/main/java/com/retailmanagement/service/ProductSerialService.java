package com.retailmanagement.service;

import com.retailmanagement.entity.ProductSerial;
import com.retailmanagement.entity.ProductVariant;
import com.retailmanagement.repository.ProductSerialRepository;
import com.retailmanagement.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductSerialService {

    private final ProductSerialRepository serialRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional
    public Map<String, Object> importFromExcel(Integer variantId, MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<ProductSerial> serialsToSave = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Bỏ qua dòng 1 (Tiêu đề)

                Cell cell = row.getCell(0); // Chỉ đọc Cột A
                if (cell == null || cell.getCellType() == CellType.BLANK) continue;

                String serialNumber = "";
                if (cell.getCellType() == CellType.STRING) {
                    serialNumber = cell.getStringCellValue().trim();
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    serialNumber = String.valueOf((long) cell.getNumericCellValue()).trim();
                }

                if (serialNumber.isEmpty()) continue;

                // Kiểm tra trùng lặp
                if (serialRepository.existsBySerialNumber(serialNumber)) {
                    errors.add("Dòng " + (row.getRowNum() + 1) + ": Seri '" + serialNumber + "' đã tồn tại.");
                    continue;
                }

                ProductSerial ps = new ProductSerial();
                ps.setVariantId(variantId);
                ps.setSerialNumber(serialNumber);
                ps.setStatus("IN_STOCK");
                serialsToSave.add(ps);
                successCount++;
            }

            if (!serialsToSave.isEmpty()) {
                serialRepository.saveAllAndFlush(serialsToSave);

                // Đồng bộ lại tồn kho
                ProductVariant variant = variantRepository.findById(variantId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Variant"));
                int currentStock = serialRepository.countByVariantIdAndStatus(variantId, "IN_STOCK");
                variant.setStockQuantity(currentStock);
                variant.setIsActive(currentStock > 0);
                variantRepository.save(variant);
            }

        } catch (Exception e) {
            errors.add("Không thể đọc file: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("errors", errors);
        return result;
    }
}