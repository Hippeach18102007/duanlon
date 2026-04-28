package com.retailmanagement.controller;

import com.retailmanagement.dto.request.UpsertPriceRequest;
import com.retailmanagement.dto.response.ApiResponse;
import com.retailmanagement.dto.response.PriceHistoryResponse;
import com.retailmanagement.dto.response.VariantPriceResponse;
import com.retailmanagement.entity.PriceHistory;
import com.retailmanagement.service.PricingService;
import com.retailmanagement.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private final PricingService pricingService;

    public PriceController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    /** Set price for a variant */
    @PostMapping("/variants/{variantId}")
    public ApiResponse<PriceHistoryResponse> setVariantPrice(
            @PathVariable Integer variantId,
            @RequestBody UpsertPriceRequest req) {
        Integer userId = SecurityUtil.getCurrentUserId(); // fix created_by null
        PriceHistory ph = pricingService.setVariantPrice(variantId, req, userId);
        return ApiResponse.success(pricingService.toPriceHistoryResponse(ph));
    }

    /** List all variant prices for a product */
    @GetMapping("/products/{productId}")
    public ApiResponse<List<VariantPriceResponse>> listByProduct(
            @PathVariable Integer productId) {
        return ApiResponse.success(pricingService.listCurrentPricesByProduct(productId));
    }

    /** List variant prices for a product with customer group pricing */
    @GetMapping("/products/{productId}/customer/{customerId}")
    public ApiResponse<List<VariantPriceResponse>> listByProductForCustomer(
            @PathVariable Integer productId,
            @PathVariable Integer customerId) {
        return ApiResponse.success(
                pricingService.listCurrentPricesByProductForCustomer(productId, customerId));
    }

    /** Update price history record — tạo bản ghi mới thay vì overwrite */
    @PutMapping("/history/{id}")
    public ApiResponse<PriceHistoryResponse> updateLatest(
            @PathVariable Long id,
            @RequestBody UpsertPriceRequest req) {
        Integer userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(pricingService.updateLatestHistory(id, req, userId));
    }

    /** Delete latest price and rollback */
    @DeleteMapping("/history/{id}")
    public ApiResponse<String> deleteLatest(@PathVariable Long id) {
        pricingService.deleteLatestAndRollback(id);
        return ApiResponse.success("OK");
    }

    /** Get effective price for a variant */
    @GetMapping("/variants/{variantId}/effective")
    public ApiResponse<VariantPriceResponse> getEffective(
            @PathVariable Integer variantId) {
        return ApiResponse.success(pricingService.getEffectivePrice(variantId));
    }

    /** Get effective price for a variant with customer context */
    @GetMapping("/variants/{variantId}/effective/customer/{customerId}")
    public ApiResponse<VariantPriceResponse> getEffectiveForCustomer(
            @PathVariable Integer variantId,
            @PathVariable Integer customerId) {
        return ApiResponse.success(
                pricingService.getEffectivePriceForCustomer(variantId, customerId));
    }

    /** Lịch sử giá của variant — có phân trang */
    @GetMapping("/variants/{variantId}/history")
    public ApiResponse<Page<PriceHistoryResponse>> getPriceHistory(
            @PathVariable Integer variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(pricingService.getPriceHistory(variantId, pageable));
    }

    /** Cảnh báo giá thấp hơn giá nhập — 1 variant */
    @GetMapping("/variants/{variantId}/cost-warning")
    public ApiResponse<Map<String, Object>> getCostWarning(
            @PathVariable Integer variantId) {
        return ApiResponse.success(pricingService.checkPriceBelowCost(variantId));
    }

    /** Danh sách tất cả variant có giá < giá nhập */
    @GetMapping("/conflicts")
    public ApiResponse<List<Map<String, Object>>> getPricingConflicts() {
        return ApiResponse.success(pricingService.getAllConflictsBelowCost());
    }

    /** Tính giá cuối cho danh sách sản phẩm */
    @PostMapping("/calculate-order")
    public ApiResponse<Map<String, Object>> calculateOrderPrice(
            @RequestBody Map<String, Object> body) {
        Integer customerId = (Integer) body.get("customerId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        return ApiResponse.success(pricingService.calculateOrderPrice(items, customerId));
    }

    /** Dashboard giá & khuyến mãi */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        return ApiResponse.success(pricingService.getDashboard());
    }
}