package com.retailmanagement.controller;

import com.retailmanagement.dto.response.CustomerResponse;
import com.retailmanagement.dto.response.LoyaltySummaryResponse;
import com.retailmanagement.service.CustomerService;
import com.retailmanagement.service.LoyaltySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth/loyalty/summary")
@RequiredArgsConstructor
public class LoyaltySummaryController {

    private final LoyaltySummaryService summaryService;
    private final CustomerService customerService;

    // ─────────────────────────────────────────────────────────────────
    // CUSTOMER endpoints — khách hàng xem điểm của chính mình
    // ─────────────────────────────────────────────────────────────────

    /**
     * Tổng hợp điểm theo TUẦN của khách hàng đang đăng nhập
     * GET /api/auth/loyalty/summary/me/weekly?weeks=12
     */
    @GetMapping("/me/weekly")
    public ResponseEntity<?> getMyWeeklySummary(
            @RequestParam(defaultValue = "12") int weeks) {

        CustomerResponse me = getCurrentCustomer();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (weeks <= 0 || weeks > 52) weeks = 12;

        List<LoyaltySummaryResponse> result =
                summaryService.getWeeklySummaryForCustomer(me.getId(), weeks);
        return ResponseEntity.ok(result);
    }

    /**
     * Tổng hợp điểm theo THÁNG của khách hàng đang đăng nhập
     * GET /api/auth/loyalty/summary/me/monthly?months=12
     */
    @GetMapping("/me/monthly")
    public ResponseEntity<?> getMyMonthlySummary(
            @RequestParam(defaultValue = "12") int months) {

        CustomerResponse me = getCurrentCustomer();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (months <= 0 || months > 24) months = 12;

        List<LoyaltySummaryResponse> result =
                summaryService.getMonthlySummaryForCustomer(me.getId(), months);
        return ResponseEntity.ok(result);
    }

    /**
     * Tổng hợp điểm theo TUẦN của 1 khách hàng bất kỳ (khách tự xem của mình)
     * GET /api/auth/loyalty/summary/customer/{customerId}/weekly?weeks=12
     */
    @GetMapping("/customer/{customerId}/weekly")
    public ResponseEntity<List<LoyaltySummaryResponse>> getCustomerWeeklySummary(
            @PathVariable Integer customerId,
            @RequestParam(defaultValue = "12") int weeks) {

        if (weeks <= 0 || weeks > 52) weeks = 12;
        return ResponseEntity.ok(
                summaryService.getWeeklySummaryForCustomer(customerId, weeks));
    }

    /**
     * Tổng hợp điểm theo THÁNG của 1 khách hàng bất kỳ
     * GET /api/auth/loyalty/summary/customer/{customerId}/monthly?months=12
     */
    @GetMapping("/customer/{customerId}/monthly")
    public ResponseEntity<List<LoyaltySummaryResponse>> getCustomerMonthlySummary(
            @PathVariable Integer customerId,
            @RequestParam(defaultValue = "12") int months) {

        if (months <= 0 || months > 24) months = 12;
        return ResponseEntity.ok(
                summaryService.getMonthlySummaryForCustomer(customerId, months));
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN endpoints — tổng hợp toàn bộ khách hàng
    // ─────────────────────────────────────────────────────────────────

    /**
     * Admin: Tổng hợp điểm tất cả khách hàng theo TUẦN (có breakdown)
     * GET /api/auth/loyalty/summary/admin/weekly?weeks=12
     */
    @GetMapping("/admin/weekly")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<LoyaltySummaryResponse>> getAdminWeeklySummary(
            @RequestParam(defaultValue = "12") int weeks) {

        if (weeks <= 0 || weeks > 52) weeks = 12;
        return ResponseEntity.ok(summaryService.getWeeklySummaryForAdmin(weeks));
    }

    /**
     * Admin: Tổng hợp điểm tất cả khách hàng theo THÁNG (có breakdown)
     * GET /api/auth/loyalty/summary/admin/monthly?months=12
     */
    @GetMapping("/admin/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<LoyaltySummaryResponse>> getAdminMonthlySummary(
            @RequestParam(defaultValue = "12") int months) {

        if (months <= 0 || months > 24) months = 12;
        return ResponseEntity.ok(summaryService.getMonthlySummaryForAdmin(months));
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────

    private CustomerResponse getCurrentCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return customerService.findByEmail(auth.getName());
    }
}