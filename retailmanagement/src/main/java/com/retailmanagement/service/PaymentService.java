package com.retailmanagement.service;

import com.retailmanagement.dto.request.PaymentRequest;
import com.retailmanagement.dto.response.PaymentResponse;
import com.retailmanagement.constants.OrderStatuses;
import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final CustomerService customerService;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final CustomRes customerRepository;
    private final SpinWheelService spinWheelService;
    private final CustomerEventNotificationService eventNotificationService; // ✅ THÊM

    // ============================================================
    // PAYMENT CREATION & PROCESSING
    // ============================================================

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, Integer userId) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if ("ONLINE".equalsIgnoreCase(order.getChannel())
                && !"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể thanh toán đơn hàng ở trạng thái PENDING");
        }

        BigDecimal finalAmount = order.getTotalAmount();
        Customer customer = order.getCustomer();

        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền đơn hàng không hợp lệ");
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(finalAmount)
                .method(request.getMethod())
                .transactionRef(request.getTransactionRef())
                .status("SUCCESS")
                .paidAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        payment = paymentRepository.save(payment);

        order.setPaymentStatus("PAID");
        if ("ONLINE".equalsIgnoreCase(order.getChannel())) {
            order.setStatus(OrderStatuses.PAID);
        }
        order.setPaidAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        processStockExport(order, userId);

        if (customer != null) {
            customer.setLastOrderAt(
                    Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            );
            // ✅ THÊM: thông báo thanh toán thành công
            eventNotificationService.onOrderPaid(order, payment);
        }

        orderRepository.save(order);
        return mapToResponse(payment);
    }

    private void processStockExport(Order order, Integer userId) {
        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = item.getVariant();

            int availableStock = variant.getStockQuantity() - variant.getReservedQty();
            if (availableStock < 0) {
                throw new RuntimeException("Lỗi tồn kho: variant " + variant.getSku() + " không đủ hàng");
            }

            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            variant.setReservedQty(variant.getReservedQty() - item.getQuantity());
            variantRepository.save(variant);

            StockTransaction exportTx = new StockTransaction();
            exportTx.setVariant(variant);
            exportTx.setQuantity(-item.getQuantity());
            exportTx.setType("EXPORT");
            exportTx.setReferenceType("orders");
            exportTx.setReferenceId(order.getId());
            exportTx.setNote("Xuất kho sau thanh toán đơn " + order.getOrderNumber());
            if (userId != null) {
                exportTx.setCreatedBy(order.getUser());
            }
            exportTx.setCreatedAt(Instant.now());
            stockTransactionRepository.save(exportTx);
        }
    }

    // ============================================================
    // QUERY OPERATIONS (Active Payments Only)
    // ============================================================

    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));
        return mapToResponse(payment);
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse getPaymentDetail(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));
        return mapToDetailResponse(payment);
    }

    // ============================================================
    // QUERY OPERATIONS (Including Deleted)
    // ============================================================

    public List<PaymentResponse> getAllPaymentsByOrderIdIncludingDeleted(Long orderId) {
        return paymentRepository.findAllByOrderIdIncludingDeleted(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getDeletedPaymentsByOrderId(Long orderId) {
        return paymentRepository.findDeletedByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CUSTOMER PAYMENT QUERIES
    // ============================================================

    public List<PaymentResponse> getPaymentsByCustomerId(Integer customerId) {
        return paymentRepository.findActiveByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getAllPaymentsByCustomerIdIncludingDeleted(Integer customerId) {
        return paymentRepository.findAllByCustomerIdIncludingDeleted(customerId).stream()
                .map(this::mapToResponseWithDeleted)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getDeletedPaymentsByCustomerId(Integer customerId) {
        return paymentRepository.findDeletedByCustomerId(customerId).stream()
                .map(this::mapToResponseWithDeleted)
                .collect(Collectors.toList());
    }

    // ============================================================
    // REFUND OPERATION
    // ============================================================

    @Transactional
    public void refundPayment(Long paymentId, Integer userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("Chỉ có thể hoàn tiền cho payment đã thành công");
        }

        Order order = payment.getOrder();
        if (!"PAID".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể hoàn tiền cho đơn hàng đã thanh toán");
        }

        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);

        order.setPaymentStatus("UNPAID");
        order.setStatus("PENDING");
        order.setPaidAt(null);

        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variant.setReservedQty(variant.getReservedQty() + item.getQuantity());
            variantRepository.save(variant);

            StockTransaction returnTx = new StockTransaction();
            returnTx.setVariant(variant);
            returnTx.setQuantity(item.getQuantity());
            returnTx.setType("RETURN");
            returnTx.setReferenceType("orders");
            returnTx.setReferenceId(order.getId());
            returnTx.setNote("Hoàn kho do refund payment " + payment.getId());
            if (userId != null) {
                returnTx.setCreatedBy(order.getUser());
            }
            returnTx.setCreatedAt(Instant.now());
            stockTransactionRepository.save(returnTx);
        }

        orderRepository.save(order);

        // ✅ THÊM: thông báo hoàn tiền
        eventNotificationService.onPaymentRefunded(order, payment.getAmount());
    }

    // ============================================================
    // SOFT DELETE OPERATIONS
    // ============================================================

    @Transactional
    public String softDeletePayment(Long paymentId, Integer userId) {
        Payment payment = paymentRepository.findActiveById(paymentId)
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found or already deleted: " + paymentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        payment.softDelete(user);
        paymentRepository.save(payment);

        System.out.println("✅ Soft deleted payment #" + paymentId + " by user " + user.getUsername());
        return "Payment #" + paymentId + " deleted successfully";
    }

    @Transactional
    public String restorePayment(Long paymentId) {
        Payment payment = paymentRepository.findDeletedById(paymentId)
                .orElseThrow(() -> new RuntimeException(
                        "Deleted payment not found: " + paymentId));

        payment.restore();
        paymentRepository.save(payment);

        System.out.println("✅ Restored payment #" + paymentId);
        return "Payment #" + paymentId + " restored successfully";
    }

    public boolean isPaymentDeleted(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(Payment::isDeleted)
                .orElse(false);
    }

    // ============================================================
    // MAPPING METHODS
    // ============================================================

    private PaymentResponse mapToResponse(Payment payment) {
        Order order = payment.getOrder();
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .transactionRef(payment.getTransactionRef())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt());

        Customer customer = order.getCustomer();
        if (customer != null) {
            builder.customerId(customer.getId())
                    .customerName(customer.getName());
        }

        return builder.build();
    }

    private PaymentResponse mapToResponseWithDeleted(Payment payment) {
        Order order = payment.getOrder();
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .transactionRef(payment.getTransactionRef())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .deletedAt(payment.getDeletedAt());

        Customer customer = order.getCustomer();
        if (customer != null) {
            builder.customerId(customer.getId())
                    .customerName(customer.getName());
        }

        return builder.build();
    }

    private PaymentResponse mapToDetailResponse(Payment payment) {
        Order order = payment.getOrder();
        Customer customer = order.getCustomer();

        List<PaymentResponse.PaymentItem> items = order.getOrderItems().stream()
                .map(this::mapToPaymentItem)
                .collect(Collectors.toList());

        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(order.getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .transactionRef(payment.getTransactionRef())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt());

        if (customer != null) {
            builder.customerId(customer.getId())
                    .customerName(customer.getName())
                    .customerEmail(customer.getEmail())
                    .customerPhone(customer.getPhone())
                    .customerType(customer.getCustomerType().name())
                    .vipTier(customer.getVipTier() != null ? customer.getVipTier().name() : null);
        }

        builder.orderNumber(order.getOrderNumber())
                .orderStatus(order.getStatus())
                .channel(order.getChannel())
                .subtotal(order.getSubtotal())
                .discountTotal(order.getDiscountTotal())
                .taxTotal(order.getTaxTotal())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .appliedPromotionCode(order.getAppliedPromotionCode())
                .notes(order.getNotes())
                .items(items);

        return builder.build();
    }

    private PaymentResponse.PaymentItem mapToPaymentItem(OrderItem item) {
        String imageUrl = null;

        if (item.getVariant() != null) {
            imageUrl = imageRepository.findFirstByProductIdAndIsPrimaryTrue(item.getVariant().getId())
                    .map(Image::getUrl)
                    .orElse(null);
        }

        if (imageUrl == null && item.getProduct() != null) {
            imageUrl = imageRepository.findFirstByProductIdAndIsPrimaryTrue(item.getProduct().getId())
                    .map(Image::getUrl)
                    .orElse(null);
        }

        return PaymentResponse.PaymentItem.builder()
                .itemId(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .variantName(item.getVariantName())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discount(item.getDiscount())
                .lineTotal(item.getLineTotal())
                .imageUrl(imageUrl)
                .build();
    }
}