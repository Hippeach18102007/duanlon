package com.retailmanagement.controller;

import com.retailmanagement.dto.response.BestSellingProductDTO;
import com.retailmanagement.repository.OrderItemRepository;
import com.retailmanagement.service.PromotionService;
import com.retailmanagement.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final PromotionService promotionService;
    private final OrderItemRepository orderItemRepository;

    @GetMapping("/revenue-by-channel")
    public ResponseEntity<?> revenueByChannel() {
        return ResponseEntity.ok(reportService.revenueByChannel());
    }

    @GetMapping("/revenue-by-date")
    public ResponseEntity<?> revenueByDate() {
        return ResponseEntity.ok(reportService.revenueByDate());
    }

    @GetMapping("/orders-by-staff")
    public ResponseEntity<?> ordersByStaff() {
        return ResponseEntity.ok(reportService.ordersByStaff());
    }

    /**
     * Báo cáo khuyến mãi đang áp dụng
     * GET /api/reports/promotions/active
     */
    @GetMapping("/promotions/active")
    public ResponseEntity<?> activePromotions() {
        return ResponseEntity.ok(promotionService.list(true));
    }

    /**
     * Báo cáo giá & KM theo tuần/tháng
     * GET /api/reports/promotions/summary?period=week|month
     */
    @GetMapping("/promotions/summary")
    public ResponseEntity<?> promotionSummary(
            @RequestParam(required = false, defaultValue = "month") String period) {
        return ResponseEntity.ok(promotionService.getReport(period));
    }

    /**
     * Cảnh báo xung đột khuyến mãi
     * GET /api/reports/promotions/conflicts
     */
    @GetMapping("/promotions/conflicts")
    public ResponseEntity<?> promotionConflicts() {
        return ResponseEntity.ok(promotionService.detectConflicts());
    }

    /**
     * Tổng hợp doanh thu theo trạng thái đơn hàng.
     * GET /api/reports/revenue-by-status
     * Response: [{ status, orderCount, totalRevenue }]
     */
    @GetMapping("/revenue-by-status")
    public ResponseEntity<?> revenueByStatus() {
        return ResponseEntity.ok(reportService.revenueByStatus());
    }

    /**
     * Thống kê trả hàng theo sản phẩm (loại trừ REJECTED).
     * GET /api/reports/returns-by-product
     * Response: [{ productName, sku, variantName, returnCount, totalQty, totalRefund }]
     */
    @GetMapping("/returns-by-product")
    public ResponseEntity<?> returnsByProduct() {
        return ResponseEntity.ok(reportService.returnStatsByProduct());
    }

    /**
     * ✅ THÊM MỚI: Doanh thu theo tuần (DELIVERED).
     * GET /api/reports/revenue-by-week
     * Response: [{ weekStart, revenue }]
     */
    @GetMapping("/revenue-by-week")
    public ResponseEntity<?> revenueByWeek() {
        return ResponseEntity.ok(reportService.revenueByWeek());
    }

    /**
     * ✅ THÊM MỚI: Doanh thu theo tháng (DELIVERED).
     * GET /api/reports/revenue-by-month
     * Response: [{ monthStart, revenue }]
     */
    @GetMapping("/revenue-by-month")
    public ResponseEntity<?> revenueByMonth() {
        return ResponseEntity.ok(reportService.revenueByMonth());
    }

    /**
     * ✅ THÊM MỚI: Top sản phẩm bán chạy theo số lượng (DELIVERED).
     * GET /api/reports/top-selling-products
     * Response: [{ productName, sku, variantName, totalQty, totalRevenue }]
     */
    @GetMapping("/top-selling-products")
    public ResponseEntity<?> topSellingProducts() {
        return ResponseEntity.ok(reportService.topSellingProducts());
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportProductReportToExcel() throws IOException {
        // 1. Lấy dữ liệu (Dùng DTO và Query bạn đã viết ở bước trước)
        List<BestSellingProductDTO> reportData = orderItemRepository.getTopSellingProducts(Pageable.unpaged());

        // 2. Khởi tạo Workbook (File Excel)
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Top Sản Phẩm Bán Chạy");

        // 3. Tạo style cho Header (Nền xanh thẫm, chữ trắng, in đậm)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 4. In các cột Header
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID Sản Phẩm", "Tên Sản Phẩm", "Số Lượng Đã Bán", "Tổng Doanh Thu (VNĐ)"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // 5. Đổ dữ liệu từ Database vào các dòng tiếp theo
        int rowIdx = 1;
        for (BestSellingProductDTO data : reportData) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(data.getProductId());
            row.createCell(1).setCellValue(data.getProductName());
            row.createCell(2).setCellValue(data.getTotalSold());

            Cell revenueCell = row.createCell(3);
            revenueCell.setCellValue(data.getTotalRevenue().doubleValue());
        }

        // 6. Tự động căn chỉnh độ rộng cột cho đẹp
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 7. Xuất ra Byte Array để gửi về trình duyệt
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        // 8. Cấu hình HttpHeaders để ép trình duyệt tải file về
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        // Đặt tên file có chứa thời gian tải
        headers.setContentDispositionFormData("attachment", "Bao_Cao_Ban_Hang_" + System.currentTimeMillis() + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(out.toByteArray());
    }
}