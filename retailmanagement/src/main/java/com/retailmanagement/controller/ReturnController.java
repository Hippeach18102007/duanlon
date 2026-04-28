package com.retailmanagement.controller;

import com.retailmanagement.dto.request.CreateReturnRequest;
import com.retailmanagement.dto.request.ProcessReturnRequest;
import com.retailmanagement.dto.response.ApiResponse;
import com.retailmanagement.dto.response.ReturnResponse;
import com.retailmanagement.service.ReturnService;
import com.retailmanagement.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    /**
     * =========================
     * CUSTOMER / SALES
     * =========================
     */

    // ✅ Tạo yêu cầu trả hàng — multipart để nhận kèm ảnh minh chứng
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReturnResponse> createReturn(
            @RequestPart("data") CreateReturnRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        Integer userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(returnService.createReturn(request, userId, image));
    }

    // Xem lịch sử return của chính mình
    @GetMapping("/me")
    public ApiResponse<List<ReturnResponse>> getMyReturns() {
        Integer userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(returnService.getReturnsByCustomer(userId));
    }

    // Customer / Sales hủy yêu cầu (khi còn PENDING)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancelReturn(@PathVariable Long id) {
        Integer userId = SecurityUtil.getCurrentUserId();
        returnService.cancelReturn(id, userId);
        return ApiResponse.success(null);
    }

    /**
     * =========================
     * ADMIN — xét duyệt
     * =========================
     */

    // ✅ Endpoint chính: Admin ra quyết định với 3 mức
    //    refundType = FULL (100%) / PARTIAL (80%) / REJECT (từ chối)
    @PostMapping("/{id}/decide")
    public ApiResponse<ReturnResponse> decide(
            @PathVariable Long id,
            @RequestBody ProcessReturnRequest request
    ) {
        Integer staffId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(returnService.decide(id, request, staffId));
    }

    // Giữ lại để tương thích ngược
    @PutMapping("/{id}/approve")
    public ApiResponse<Void> approveReturn(@PathVariable Long id) {
        Integer staffId = SecurityUtil.getCurrentUserId();
        returnService.approveReturn(id, staffId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<Void> rejectReturn(
            @PathVariable Long id,
            @RequestBody ProcessReturnRequest request
    ) {
        Integer staffId = SecurityUtil.getCurrentUserId();
        returnService.rejectReturn(id, request.getNote(), staffId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/process")
    public ApiResponse<ReturnResponse> processReturn(
            @PathVariable Long id,
            @RequestBody ProcessReturnRequest request
    ) {
        Integer staffId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(returnService.processReturn(id, request.getNote(), staffId));
    }

    /**
     * =========================
     * QUERY
     * =========================
     */

    @GetMapping
    public ApiResponse<List<ReturnResponse>> getAllReturns() {
        return ApiResponse.success(returnService.getAllReturns());
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<ReturnResponse>> getReturnsByStatus(@PathVariable String status) {
        return ApiResponse.success(returnService.getReturnsByStatus(status));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<ReturnResponse>> getReturnsByOrder(@PathVariable Long orderId) {
        return ApiResponse.success(returnService.getReturnsByOrder(orderId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReturnResponse> getReturnDetail(@PathVariable Long id) {
        return ApiResponse.success(returnService.getReturnDetail(id));
    }
}