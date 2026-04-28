package com.retailmanagement.controller;

import com.retailmanagement.dto.request.PaymentRequest;
import com.retailmanagement.dto.response.PaymentResponse;
import com.retailmanagement.repository.OrderRepository;
import com.retailmanagement.security.service.CustomUserDetails;
import com.retailmanagement.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.retailmanagement.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final VnPayService vnPayService;
    private final OrderRepository orderRepository;


    @GetMapping("/vnpay-url/{orderId}")
    public ResponseEntity<?> getVnPayUrl(
            @PathVariable Long orderId,
            HttpServletRequest request) {

        // Gọi service tạo URL
        String url = vnPayService.createPaymentUrl(orderId, request);
        return ResponseEntity.ok(Map.of("paymentUrl", url));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> vnpayIPN(@RequestParam Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        params.remove("vnp_SecureHash");

        // 1. Kiểm tra chữ ký bảo mật (Sử dụng VnPayConfig tao đưa ở turn trước)
        String signValue = com.retailmanagement.config.VnPayConfig.hashAllFields(params);
        if (!signValue.equals(vnp_SecureHash)) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }

        // 2. Tìm đơn hàng
        String orderNumber = params.get("vnp_TxnRef");
        var orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
        }

        var order = orderOpt.get();

        // 3. Kiểm tra trạng thái tránh cập nhật trùng
        if (!"UNPAID".equals(order.getPaymentStatus())) {
            return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
        }

        // 4. Kiểm tra mã phản hồi (00 là thành công)
        if ("00".equals(params.get("vnp_ResponseCode"))) {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setMethod("VNPAY");
            paymentRequest.setTransactionRef(params.getOrDefault("vnp_TransactionNo", "TXN-" + orderNumber));
            paymentService.createPayment(paymentRequest, null);
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    /**
     * Return URL — VNPay redirect user về đây sau thanh toán.
     * Dùng khi IPN không reach được (vd: localhost/dev).
     * Xác thực chữ ký và update DB giống IPN.
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        Map<String, String> fields = new HashMap<>(params);
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // Xác thực chữ ký
        String signValue = com.retailmanagement.config.VnPayConfig.hashAllFields(fields);
        if (!signValue.equals(vnp_SecureHash)) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }

        String orderNumber = params.get("vnp_TxnRef");
        var orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
        }

        var order = orderOpt.get();

        // Idempotent — nếu IPN đã xử lý trước thì bỏ qua
        if ("PAID".equals(order.getPaymentStatus())) {
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Already paid"));
        }

        if ("00".equals(params.get("vnp_ResponseCode"))) {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setMethod("VNPAY");
            paymentRequest.setTransactionRef(params.getOrDefault("vnp_TransactionNo", "TXN-" + orderNumber));
            paymentService.createPayment(paymentRequest, null);
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    /**
     * Helper method to extract user ID from authentication
     */
    private Integer getUserIdFromAuth() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                System.out.println("❌ No authentication found");
                return null;
            }

            Object principal = auth.getPrincipal();
            System.out.println("🔍 Principal type: " + principal.getClass().getName());

            if (principal instanceof CustomUserDetails) {
                CustomUserDetails user = (CustomUserDetails) principal;
                Integer userId = user.getUserId();
                System.out.println("✅ Got userId from CustomUserDetails: " + userId);
                return userId;
            }

            System.out.println("⚠️ Principal is not CustomUserDetails: " + principal);
            return null;

        } catch (Exception e) {
            System.out.println("❌ Error getting user ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * TEST ENDPOINT - Debug authentication
     */
    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuth(@AuthenticationPrincipal CustomUserDetails user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity.ok(Map.of(
                "authPrincipalNull", user == null,
                "authPrincipalType", user != null ? user.getClass().getName() : "null",
                "securityContextAuth", auth != null ? auth.getPrincipal().getClass().getName() : "null",
                "userId", user != null ? user.getUserId() : "N/A",
                "username", user != null ? user.getUsername() : "N/A"
        ));
    }

    /**
     * Tạo payment mới
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        Integer userId = user != null ? user.getUserId() : getUserIdFromAuth();
        PaymentResponse response = paymentService.createPayment(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách payments theo order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(orderId));
    }

    /**
     * Lấy thông tin cơ bản 1 payment
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /**
     * Lấy tất cả payments
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * Lấy chi tiết payment đầy đủ
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<PaymentResponse> getPaymentDetail(
            @PathVariable Long id) {

        return ResponseEntity.ok(paymentService.getPaymentDetail(id));
    }

    /**
     * Hoàn tiền
     */
    @PutMapping("/{id}/refund")
    public ResponseEntity<Void> refundPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        Integer userId = user != null ? user.getUserId() : getUserIdFromAuth();
        paymentService.refundPayment(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ FIXED: Soft delete with better authentication handling
     */
    @DeleteMapping("/{id}/soft-delete")
    public ResponseEntity<String> softDeletePayment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        System.out.println("🔵 Soft delete request for payment: " + id);

        // Try to get user ID from multiple sources
        Integer userId = null;

        if (user != null) {
            userId = user.getUserId();
            System.out.println("✅ Got userId from @AuthenticationPrincipal: " + userId);
        } else {
            System.out.println("⚠️ @AuthenticationPrincipal is null, trying SecurityContext");
            userId = getUserIdFromAuth();
        }

        if (userId == null) {
            System.out.println("❌ Could not get user ID from any source");
            return ResponseEntity.status(401).body("User not authenticated or user ID not available");
        }

        try {
            System.out.println("🔵 Calling service.softDeletePayment with userId: " + userId);
            String message = paymentService.softDeletePayment(id, userId);
            System.out.println("✅ Soft delete successful: " + message);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            System.out.println("❌ Error in soft delete: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Restore a soft-deleted payment
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<String> restorePayment(@PathVariable Long id) {
        System.out.println("🔵 Restore request for payment: " + id);

        try {
            String message = paymentService.restorePayment(id);
            System.out.println("✅ Restore successful: " + message);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            System.out.println("❌ Error in restore: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}