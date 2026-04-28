package com.retailmanagement.controller;

import com.retailmanagement.dto.response.TierProgressResponse;
import com.retailmanagement.service.CustomerService;
import com.retailmanagement.service.TierProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth/tier-progress")
@RequiredArgsConstructor
public class TierProgressController {

    private final TierProgressService tierProgressService;
    private final CustomerService customerService;

    /**
     * Lấy tiến trình lên hạng của khách hàng hiện tại
     * GET /api/auth/tier-progress/me
     */
    @GetMapping("/me")
    public ResponseEntity<TierProgressResponse> getMyTierProgress() {
        Integer customerId = getCurrentCustomerId();
        TierProgressResponse progress = tierProgressService.calculateTierProgress(customerId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Tính toán với giỏ hàng hiện tại
     * POST /api/auth/tier-progress/check-cart
     * Body: { "cartTotal": 5000000 }
     */
    @PostMapping("/check-cart")
    public ResponseEntity<TierProgressResponse> checkWithCart(
            @RequestBody Map<String, BigDecimal> request
    ) {
        Integer customerId = getCurrentCustomerId();
        BigDecimal cartTotal = request.get("cartTotal");

        if (cartTotal == null || cartTotal.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().build();
        }

        TierProgressResponse progress = tierProgressService.calculateWithCart(customerId, cartTotal);
        return ResponseEntity.ok(progress);
    }

    /**
     * Lấy tiến trình của khách hàng cụ thể (cho admin)
     * GET /api/auth/tier-progress/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<TierProgressResponse> getCustomerTierProgress(
            @PathVariable Integer customerId
    ) {
        TierProgressResponse progress = tierProgressService.calculateTierProgress(customerId);
        return ResponseEntity.ok(progress);
    }

    // Helper method
    private Integer getCurrentCustomerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        var customer = customerService.findByEmail(email);
        if (customer == null) {
            throw new RuntimeException("Không tìm thấy khách hàng");
        }

        return customer.getId();
    }
}