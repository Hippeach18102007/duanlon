package com.retailmanagement.controller;

import com.retailmanagement.dto.request.PaymentRequest;
import com.retailmanagement.dto.response.PaymentResponse;
import com.retailmanagement.repository.OrderRepository;
import com.retailmanagement.security.service.CustomUserDetails;
import com.retailmanagement.service.PaymentService;
import com.retailmanagement.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        String url = vnPayService.createPaymentUrl(orderId, request);
        return ResponseEntity.ok(Map.of("paymentUrl", url));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<?> vnpayIPN(@RequestParam Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        params.remove("vnp_SecureHash");

        String signValue = com.retailmanagement.config.VnPayConfig.hashAllFields(params);
        if (!signValue.equals(vnp_SecureHash)) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }

        String txnRef = params.get("vnp_TxnRef");
        Optional<com.retailmanagement.entity.Order> orderOpt = resolveOrderByTxnRef(txnRef);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
        }

        var order = orderOpt.get();

        if (!"UNPAID".equals(order.getPaymentStatus())) {
            return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
        }

        if ("00".equals(params.get("vnp_ResponseCode"))) {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setMethod("VNPAY");
            paymentRequest.setTransactionRef(params.getOrDefault("vnp_TransactionNo", "TXN-" + txnRef));
            paymentService.createPayment(paymentRequest, null);
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        Map<String, String> fields = new HashMap<>(params);
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String signValue = com.retailmanagement.config.VnPayConfig.hashAllFields(fields);
        if (!signValue.equals(vnp_SecureHash)) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }

        String txnRef = params.get("vnp_TxnRef");
        Optional<com.retailmanagement.entity.Order> orderOpt = resolveOrderByTxnRef(txnRef);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
        }

        var order = orderOpt.get();

        if ("PAID".equals(order.getPaymentStatus())) {
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Already paid"));
        }

        if ("00".equals(params.get("vnp_ResponseCode"))) {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setOrderId(order.getId());
            paymentRequest.setMethod("VNPAY");
            paymentRequest.setTransactionRef(params.getOrDefault("vnp_TransactionNo", "TXN-" + txnRef));
            paymentService.createPayment(paymentRequest, null);
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    private Optional<com.retailmanagement.entity.Order> resolveOrderByTxnRef(String txnRef) {
        if (txnRef == null || txnRef.isBlank()) return Optional.empty();

        // Hỗ trợ cả format thuần số (ID) và format "ID-timestamp"
        String idPart = txnRef.contains("-") ? txnRef.split("-")[0] : txnRef;
        try {
            return orderRepository.findById(Long.valueOf(idPart));
        } catch (NumberFormatException e) {
            return orderRepository.findByOrderNumber(txnRef);
        }
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        Integer userId = user != null ? user.getUserId() : null;
        PaymentResponse response = paymentService.createPayment(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(orderId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<PaymentResponse> getPaymentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentDetail(id));
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<Void> refundPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        Integer userId = user != null ? user.getUserId() : null;
        paymentService.refundPayment(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/soft-delete")
    public ResponseEntity<String> softDeletePayment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        try {
            String message = paymentService.softDeletePayment(id, user.getUserId());
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<String> restorePayment(@PathVariable Long id) {
        try {
            String message = paymentService.restorePayment(id);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}