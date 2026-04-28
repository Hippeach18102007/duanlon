package com.retailmanagement.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductImportResult {
    private int totalRows;
    private int successCount;
    private int failCount;
    private List<String> errors; // "Row 5: SKU đã tồn tại"
}