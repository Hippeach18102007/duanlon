    package com.retailmanagement.controller;
    
    
    import com.retailmanagement.dto.request.CustomerRequest;
    import com.retailmanagement.dto.response.CustomerResponse;
    import com.retailmanagement.dto.response.LoyaltyLedgerResponse;
    import com.retailmanagement.dto.response.PaymentResponse;
    import com.retailmanagement.dto.response.PromotionHistoryResponse;
    import com.retailmanagement.entity.CustomerType;
    import com.retailmanagement.service.*;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.web.bind.annotation.*;
    
    import java.math.BigDecimal;
    import java.util.List;
    import java.util.Map;
    
    @RestController
    @CrossOrigin(origins = "*")
    @RequestMapping("/api/auth/customers")
    @RequiredArgsConstructor
    public class CusController {
        @Autowired
        private CustomerService cusservice;
        @Autowired
        private LoyaltyHistoryService loyaltyHistoryService;
        @Autowired
        private PromotionHistoryService promotionHistoryService;
        @Autowired
        private SpinWheelService spinWheelService;
        @Autowired
        private PaymentService paymentService;
        @PostMapping("")
        public ResponseEntity<CustomerResponse> addCustomer(@Valid @RequestBody CustomerRequest cus) {
           CustomerResponse response= cusservice.create(cus);
            return ResponseEntity.status(HttpStatus.CREATED).body(response) ;
        }
        @GetMapping("")
        public ResponseEntity<List<CustomerResponse>> findAll() {
            return ResponseEntity.status(HttpStatus.OK).body(cusservice.findAll());
        }
    
        @DeleteMapping("/{id}")
        public ResponseEntity<CustomerResponse> deleteCustomer(@Valid @PathVariable int id) {
            cusservice.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        @PutMapping("/{id}")
        public ResponseEntity<CustomerResponse> updateCustomer(@Valid @PathVariable int id, @RequestBody CustomerRequest cus) {
            CustomerResponse up = cusservice.updateById(id, cus);
            return ResponseEntity.ok().body(up) ;
        }
        @PostMapping("/{id}/points")
        public ResponseEntity<String> addPoints(
                @PathVariable Integer id,
                @RequestBody Map<String, BigDecimal> payload) {
    
            BigDecimal amount = payload.get("amount");
    
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount phải lớn hơn 0");
            }
    
            cusservice.addLoyaltyPoints(id, amount);
            return ResponseEntity.ok("Đã cộng điểm và cập nhật hạng thành công!");
        }
    
        // ĐƯA HÀM NÀY LÊN TRÊN
        @GetMapping("/me")
        public ResponseEntity<?> getMyProfile() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("DEBUG: Không có authentication hoặc chưa xác thực");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");
            }
    
            String currentUsername = authentication.getName();
            System.out.println("DEBUG: Username từ SecurityContext: " + currentUsername);
            System.out.println("DEBUG: Principal: " + authentication.getPrincipal());
            System.out.println("DEBUG: Authorities: " + authentication.getAuthorities());
    
            try {
                CustomerResponse profile = cusservice.findByEmail(currentUsername);
    
                if (profile == null) {
                    System.out.println("DEBUG: Không tìm thấy khách hàng với email: " + currentUsername);
    
                    // Thử tìm bằng username nếu không phải email
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of(
                                    "error", "Không tìm thấy thông tin khách hàng",
                                    "username", currentUsername
                            ));
                }
    
                System.out.println("DEBUG: Tìm thấy khách hàng: " + profile.getEmail());
                return ResponseEntity.ok(profile);
    
            } catch (Exception e) {
                System.out.println("DEBUG: Lỗi khi tìm khách hàng: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Lỗi hệ thống", "message", e.getMessage()));
            }
        }
    
        @GetMapping("/type/{type}")
        public ResponseEntity<List<CustomerResponse>> findByCustomerType(@PathVariable CustomerType type) {
            return ResponseEntity.ok(cusservice.findbyCustomerType(type));
        }
        @GetMapping("/active-last-30-days")
        public ResponseEntity<List<CustomerResponse>> getActiveCustomersLast30Days() {
            List<CustomerResponse> activeCustomers = cusservice.findActiveInLast30Days();
            return ResponseEntity.ok(activeCustomers);
        }
        @GetMapping("/{customerId}/loyalty-history")
        public ResponseEntity<List<LoyaltyLedgerResponse>> getLoyaltyHistory(
                @PathVariable Integer customerId) {
            return ResponseEntity.ok(loyaltyHistoryService.getCustomerHistory(customerId));
        }
    
        /**
         * Lấy lịch sử thay đổi hạng VIP
         * GET /api/auth/customers/{customerId}/tier-history
         */
        @GetMapping("/{customerId}/tier-history")
        public ResponseEntity<List<LoyaltyLedgerResponse>> getTierHistory(
                @PathVariable Integer customerId) {
            return ResponseEntity.ok(loyaltyHistoryService.getTierChanges(customerId));
        }
        @GetMapping("/points")
        public ResponseEntity<List<CustomerResponse>> getCustomersByPointsRange(
                @RequestParam(defaultValue = "0") int min,
                @RequestParam(defaultValue = "999999") int max
        ) {
            if (min < 0 || max < min) {
                return ResponseEntity.badRequest().build();
            }
    
            List<CustomerResponse> customers = cusservice.findByPointsRange(min, max);
            return ResponseEntity.ok(customers);
        }
    
        @GetMapping("/vip-tier/{tier}/points")
        public ResponseEntity<List<CustomerResponse>> getCustomersByVipTierAndPoints(
                @PathVariable String tier,
                @RequestParam(defaultValue = "0") int min,
                @RequestParam(defaultValue = "999999") int max
        ) {
            try {
                List<CustomerResponse> customers = cusservice.findByVipTierAndPointsRange(tier, min, max);
                return ResponseEntity.ok(customers);
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        @GetMapping("/vip-tier/{tier}")
        public ResponseEntity<List<CustomerResponse>> getCustomersByVipTier(
                @PathVariable String tier
        ) {
            try {
                List<CustomerResponse> customers = cusservice.findByVipTier(tier);
                return ResponseEntity.ok(customers);
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        // Add these endpoints to CusController.java
    
        /**
         * Lấy khách hàng theo khoảng chi tiêu
         * GET /api/auth/customers/spending?min=0&max=10000000
         */
        @GetMapping("/spending")
        public ResponseEntity<List<CustomerResponse>> getCustomersBySpendingRange(
                @RequestParam(defaultValue = "0") BigDecimal min,
                @RequestParam(defaultValue = "999999999") BigDecimal max
        ) {
            if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(min) < 0) {
                return ResponseEntity.badRequest().build();
            }
    
            List<CustomerResponse> customers = cusservice.findBySpendingRange(min, max);
            return ResponseEntity.ok(customers);
        }
    
        /**
         * Lấy top N khách hàng theo tổng chi tiêu
         * GET /api/auth/customers/top-spenders?limit=10
         */
        @GetMapping("/top-spenders")
        public ResponseEntity<List<CustomerResponse>> getTopSpenders(
                @RequestParam(defaultValue = "10") int limit
        ) {
            if (limit <= 0 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(null);
            }
    
            List<CustomerResponse> topCustomers = cusservice.findTopSpenders(limit);
            return ResponseEntity.ok(topCustomers);
        }
    
        /**
         * Lấy top N khách hàng theo VIP tier và chi tiêu
         * GET /api/auth/customers/vip-tier/{tier}/top-spenders?limit=10
         */
        @GetMapping("/vip-tier/{tier}/top-spenders")
        public ResponseEntity<List<CustomerResponse>> getTopSpendersByVipTier(
                @PathVariable String tier,
                @RequestParam(defaultValue = "10") int limit
        ) {
            try {
                if (limit <= 0 || limit > 100) {
                    return ResponseEntity.badRequest().build();
                }
    
                List<CustomerResponse> customers = cusservice.findTopSpendersByVipTier(tier, limit);
                return ResponseEntity.ok(customers);
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().build();
            }
        }
    
        /**
         * Lấy thống kê chi tiêu theo VIP tier
         * GET /api/auth/customers/spending-stats
         */
        @GetMapping("/spending-stats")
        public ResponseEntity<Map<String, Object>> getSpendingStatistics() {
            Map<String, Object> stats = cusservice.getSpendingStatistics();
            return ResponseEntity.ok(stats);
        }
        @GetMapping("/spin-wheel/status")
        public ResponseEntity<Map<String, Object>> getSpinWheelStatus() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
    
            String currentUsername = authentication.getName();
            CustomerResponse profile = cusservice.findByEmail(currentUsername);
    
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
    
            Map<String, Object> status = spinWheelService.getSpinStatus(profile.getId());
            return ResponseEntity.ok(status);
        }
    
        /**
         * Perform spin wheel
         * POST /api/auth/customers/spin-wheel/spin
         */
        @PostMapping("/spin-wheel/spin")
        public ResponseEntity<Map<String, Object>> spinWheel() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
    
            String currentUsername = authentication.getName();
            CustomerResponse profile = cusservice.findByEmail(currentUsername);
    
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
    
            try {
                Map<String, Object> result = spinWheelService.spin(profile.getId());
                return ResponseEntity.ok(result);
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()));
            }
        }
    
        /**
         * Get spin wheel history for current customer
         * GET /api/auth/customers/spin-wheel/history
         */
        @GetMapping("/spin-wheel/history")
        public ResponseEntity<List<Map<String, Object>>> getSpinWheelHistory() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
    
            String currentUsername = authentication.getName();
            CustomerResponse profile = cusservice.findByEmail(currentUsername);
    
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
    
            List<Map<String, Object>> history = spinWheelService.getSpinHistory(profile.getId());
            return ResponseEntity.ok(history);
        }
    
        /**
         * Get prize options for spin wheel display
         * GET /api/auth/customers/spin-wheel/prizes
         */
        @GetMapping("/spin-wheel/prizes")
        public ResponseEntity<List<Map<String, Object>>> getPrizeOptions() {
            List<Map<String, Object>> prizes = spinWheelService.getPrizeOptions();
            return ResponseEntity.ok(prizes);
        }
        @GetMapping("/{customerId}/payments")
        public ResponseEntity<List<PaymentResponse>> getCustomerPayments(
                @PathVariable Integer customerId) {
    
            // TODO: Add authorization check - only the customer or admin can access
    
            List<PaymentResponse> payments = paymentService.getPaymentsByCustomerId(customerId);
            return ResponseEntity.ok(payments);
        }
    
        /**
         * Lấy danh sách payments của customer (bao gồm đã xóa)
         * GET /api/auth/customers/{customerId}/payments/all
         */
        @GetMapping("/{customerId}/payments/all")
        public ResponseEntity<List<PaymentResponse>> getCustomerPaymentsIncludingDeleted(
                @PathVariable Integer customerId) {
    
            // TODO: Add authorization check - only admin can see deleted
    
            List<PaymentResponse> payments = paymentService.getAllPaymentsByCustomerIdIncludingDeleted(customerId);
            return ResponseEntity.ok(payments);
        }
    
        /**
         * Lấy danh sách payments đã xóa của customer
         * GET /api/auth/customers/{customerId}/payments/deleted
         */
        @GetMapping("/{customerId}/payments/deleted")
        public ResponseEntity<List<PaymentResponse>> getDeletedCustomerPayments(
                @PathVariable Integer customerId) {
    
            // TODO: Add authorization check - only admin can see delete
            List<PaymentResponse> payments = paymentService.getDeletedPaymentsByCustomerId(customerId);
            return ResponseEntity.ok(payments);
        }
        @GetMapping("/inactive-transaction")
        public ResponseEntity<List<CustomerResponse>> getInactiveTransactionCustomers(
                @RequestParam(defaultValue = "30") int days) {
            if (days <= 0) return ResponseEntity.badRequest().build();
            return ResponseEntity.ok(cusservice.findInactiveTransactionDays(days));
        }
        @PatchMapping("/{id}/vip-note")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
        public ResponseEntity<CustomerResponse> updateVipNote(
                @PathVariable Integer id,
                @RequestBody Map<String, String> body) {
    
            String note = body.getOrDefault("vipNote", "");
            CustomerResponse res = cusservice.updateVipNote(id, note);
            return ResponseEntity.ok(res);
        }
        /**
         * Lấy danh sách khách hàng chưa mua đơn nào
         * GET /api/auth/customers/zero-order?minDays=3
         *
         * minDays: chỉ trả về khách đã đăng ký >= minDays ngày mà vẫn chưa mua
         * Mặc định: 3 ngày (đủ thời gian tìm hiểu, nếu vẫn chưa mua thì cần hỗ trợ)
         */
        @GetMapping("/zero-order")
        @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
        public ResponseEntity<List<CustomerResponse>> getZeroOrderCustomers(
                @RequestParam(defaultValue = "3") int minDays) {

            if (minDays < 0) return ResponseEntity.badRequest().build();

            List<CustomerResponse> customers = cusservice.findZeroOrderCustomers(minDays);
            return ResponseEntity.ok(customers);
        }

        /**
         * Thống kê nhanh zero-order customers theo các mốc thời gian
         * GET /api/auth/customers/zero-order/stats
         */
        @GetMapping("/zero-order/stats")
        @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
        public ResponseEntity<Map<String, Object>> getZeroOrderStats() {
            return ResponseEntity.ok(cusservice.getZeroOrderStats());
        }
        @GetMapping("/{customerId}/promotion-history")
        public ResponseEntity<List<PromotionHistoryResponse>> getPromotionHistory(
                @PathVariable Integer customerId,
                @RequestParam(required = false) String type
        ) {
            List<PromotionHistoryResponse> history = promotionHistoryService.getPromotionHistory(customerId);

            if (type != null && !type.isBlank()) {
                String upperType = type.toUpperCase();
                history = history.stream()
                        .filter(h -> upperType.equals(h.getType()))
                        .toList();
            }

            return ResponseEntity.ok(history);
        }
        @Autowired
        private LoyaltyResetService loyaltyResetService;

        @PostMapping("/loyalty/reset/year-end")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Map<String, Object>> triggerYearEndReset() {
            loyaltyResetService.triggerYearEndReset();
            return ResponseEntity.ok(Map.of("success", true, "message", "Year-end reset hoàn tất"));
        }

        @PostMapping("/loyalty/reset/monthly")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Map<String, Object>> triggerMonthlyCheck() {
            try {
                loyaltyResetService.triggerMonthlyCheck();
                return ResponseEntity.ok(Map.of("success", true, "message", "Monthly check hoàn tất"));
            } catch (Exception e) {
                // ✅ In lỗi rõ ra
                e.printStackTrace();
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", e.getMessage()));
            }
        }
    }
    
