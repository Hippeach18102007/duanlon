package com.retailmanagement.service;

import com.retailmanagement.constants.OrderStatuses;
import com.retailmanagement.dto.request.CreateReturnRequest;
import com.retailmanagement.dto.request.ProcessReturnRequest;
import com.retailmanagement.dto.response.ReturnResponse;
import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerService customerService;
    private final StockTransactionRepository stockTransactionRepository;
    private final ProductVariantRepository variantRepository;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;
    private final ProductSerialRepository productSerialRepository;
    private final OrderItemSerialRepository orderItemSerialRepository;
    private final PromotionService promotionService;
    private final SpinWheelService spinWheelService;
    private final PromotionRepository promotionRepository;

    @Transactional
    public ReturnResponse createReturn(CreateReturnRequest req, Integer userId, MultipartFile image) {

        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!OrderStatuses.DELIVERED.equals(order.getStatus())) {
            throw new RuntimeException("Order is not eligible for return");
        }

        OrderItem item = orderItemRepository.findById(req.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        int returnedQty = returnRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .filter(r -> r.getOrderItem().getId().equals(item.getId()))
                .mapToInt(Return::getQuantity)
                .sum();

        if (returnedQty + req.getQuantity() > item.getQuantity()) {
            throw new RuntimeException("Return quantity exceeds purchased quantity");
        }

        // ✅ Upload ảnh nếu có
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                imageUrl = cloudinaryService.uploadFile(image);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
            }
        }

        Return ret = Return.builder()
                .order(order)
                .orderItem(item)
                .quantity(req.getQuantity())
                .reason(req.getReason())
                .refundAmount(req.getRefundAmount())
                .refundStatus("PENDING")
                .status("PENDING")
                .imageUrl(imageUrl)   // ✅
                .processedBy(userId)
                .createdAt(Instant.now())
                .build();

        returnRepository.save(ret);

        order.setStatus(OrderStatuses.RETURN_REQUESTED);
        orderRepository.save(order);

        return mapToResponse(ret);
    }

    /**
     * refundType = FULL    → hoàn 100% theo tỉ lệ thực tế của đơn (lỗi cửa hàng)
     * refundType = PARTIAL → hoàn 80%  theo tỉ lệ thực tế của đơn (lỗi khách hàng)
     * refundType = CHANGE_OF_MIND -> khách hàng đổi ý
     * refundType = REJECT  → từ chối, không hoàn tiền
     */
    @Transactional
    public ReturnResponse decide(Long returnId, ProcessReturnRequest request, Integer staffId) {

        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found"));

        if (!"PENDING".equals(returnEntity.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xử lý yêu cầu đang PENDING");
        }

        String refundType = request.getRefundType();
        Order order = returnEntity.getOrder();

        // ── Từ chối ──────────────────────────────────────────────────
        if ("REJECT".equals(refundType)) {
            returnEntity.setStatus("REJECTED");
            returnEntity.setRefundMethod("REJECT");
            returnEntity.setNote(request.getNote());
            returnEntity.setProcessedBy(staffId);
            returnRepository.save(returnEntity);

            order.setStatus(OrderStatuses.DELIVERED);
            orderRepository.save(order);

            emailService.sendReturnRejectedEmail(returnEntity);

            return mapToResponse(returnEntity);
        }

        // ── Tính hoàn tiền ───────────────────────────────────────────
        //
        // lineTotal đã bao gồm toàn bộ discount (cấp item + cấp đơn)
        // → hoàn trực tiếp theo tỉ lệ số lượng trả / số lượng mua
        //
        // Ví dụ: lineTotal = 17.955.000, mua 2, trả 1
        //        → itemRefundBase = 17.955.000 × 1/2 = 8.977.500đ
        //        FULL    → hoàn 8.977.500đ
        //        PARTIAL → hoàn 8.977.500 × 80% = 7.182.000đ

        OrderItem item = returnEntity.getOrderItem();

        BigDecimal returnQty = BigDecimal.valueOf(returnEntity.getQuantity());
        BigDecimal itemQty   = BigDecimal.valueOf(item.getQuantity());

        BigDecimal itemRefundBase = item.getLineTotal()
                .multiply(returnQty)
                .divide(itemQty, 2, RoundingMode.HALF_UP);

        BigDecimal finalRefund;
        switch (refundType) {

            case "FULL":
                finalRefund = itemRefundBase;
                break;

            case "PARTIAL":
                finalRefund = itemRefundBase
                        .multiply(new BigDecimal("0.8"))
                        .setScale(2, RoundingMode.HALF_UP);
                break;

            case "CHANGE_OF_MIND":
                finalRefund = itemRefundBase; // nếu có policy riêng thì chỉnh ở đây
                break;

            default:
                throw new IllegalArgumentException("Invalid refund type");
        }

        returnEntity.setStatus("APPROVED");
        returnEntity.setRefundAmount(finalRefund);
        returnEntity.setRefundMethod(refundType);
        returnEntity.setRefundStatus("REFUNDED");
        returnEntity.setRefundedAt(Instant.now());
        returnEntity.setNote(request.getNote());
        returnEntity.setProcessedBy(staffId);
        returnRepository.save(returnEntity);

        order.setReturnedAt(Instant.now());
        orderRepository.save(order);

        // xử lý serial theo loại refund
        switch (refundType) {

            case "FULL":
            case "PARTIAL":
                // hàng lỗi
                markReturnedSerialsAsFaulty(returnEntity);
                break;

            case "CHANGE_OF_MIND":
                // hàng tốt → nhập lại kho
                markReturnedSerialsAsIn_Stock(returnEntity);
                restoreStockForReturn(returnEntity);
                break;
        }

        // Trừ điểm loyalty tương ứng số tiền thực hoàn
        if ("PAID".equals(order.getPaymentStatus()) && order.getCustomer() != null) {
            customerService.deductLoyaltyPoints(
                    order.getCustomer().getId(),
                    finalRefund,
                    "RETURN",
                    "returns",
                    returnId
            );
        }

        updateOrderStatusAfterReturn(order);

        // ======================================================
        // ✅ RESTORE VOUCHER ONLY IF ORDER FULLY RETURNED
        // ======================================================
        if (OrderStatuses.RETURNED.equals(order.getStatus())) {

            if (order.getCustomer() != null) {

                // ----- Restore Promotion -----
                if (order.getAppliedPromotionCode() != null) {

                    promotionRepository
                            .findByCode(order.getAppliedPromotionCode())
                            .ifPresent(promotion ->
                                    promotionService.rollbackPromotionUsage(
                                            promotion.getId(),
                                            order.getCustomer().getId(),
                                            order.getId()
                                    )
                            );
                }

                // ----- Restore Spin Wheel -----
                spinWheelService.restoreBonusByOrder(order.getId());
            }
        }

        emailService.sendReturnApprovedEmail(returnEntity);
        return mapToResponse(returnEntity);
    }

    @Transactional
    public ReturnResponse processReturn(Long returnId, String note, Integer staffId) {

        Return ret = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found"));

        ret.setStatus("APPROVED");
        ret.setRefundStatus("REFUNDED");
        ret.setRefundedAt(Instant.now());
        ret.setNote(note);
        ret.setProcessedBy(staffId);

        returnRepository.save(ret);

        updateOrderStatusAfterReturn(ret.getOrder());

        return mapToResponse(ret);
    }



    @Transactional
    public void approveReturn(Long returnId, Integer userId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found"));

        if (!"PENDING".equals(returnEntity.getStatus())) {
            throw new IllegalStateException("Only pending returns can be approved");
        }

        Order order = returnEntity.getOrder();

        // 1. Update return
        returnEntity.setStatus("APPROVED");
        returnEntity.setRefundStatus("REFUNDED");
        returnEntity.setRefundedAt(Instant.now());
        returnEntity.setProcessedBy(userId);

        returnRepository.save(returnEntity);

        // 2. Restore stock
        restoreStockForReturn(returnEntity);

        // 3. Deduct loyalty
        if ("PAID".equals(order.getPaymentStatus()) && order.getCustomer() != null) {
            customerService.deductLoyaltyPoints(
                    order.getCustomer().getId(),
                    returnEntity.getRefundAmount(),
                    "RETURN",
                    "returns",
                    returnId
            );
        }

        // 4. ❗ UPDATE ORDER STATUS
        updateOrderStatusAfterReturn(order);
    }


    @Transactional
    public void rejectReturn(Long returnId, String reason, Integer userId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found"));

        if (!"PENDING".equals(returnEntity.getStatus())) {
            throw new IllegalStateException("Only pending returns can be rejected");
        }

        Order order = returnEntity.getOrder();

        // 1. Update return
        returnEntity.setStatus("REJECTED");
        returnEntity.setNote(reason);
        returnEntity.setProcessedBy(userId);

        returnRepository.save(returnEntity);

        // 2. ❗ ROLLBACK ORDER STATUS
        order.setStatus(OrderStatuses.DELIVERED);
        orderRepository.save(order);
    }


    @Transactional
    public void cancelReturn(Long returnId, Integer userId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found"));

        if (!"PENDING".equals(returnEntity.getStatus())) {
            throw new IllegalStateException("Only pending returns can be cancelled");
        }

        returnRepository.delete(returnEntity);
    }

    public List<ReturnResponse> getAllReturns() {
        return returnRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReturnResponse> getReturnsByStatus(String status) {
        return returnRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReturnResponse> getReturnsByOrder(Long orderId) {
        return returnRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReturnResponse> getReturnsByCustomer(Integer customerId) {
        return returnRepository.findByOrder_Customer_IdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ReturnResponse getReturnDetail(Long returnId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found"));
        return mapToResponse(returnEntity);
    }

    private void restoreStockForReturn(Return returnEntity) {
        OrderItem item = returnEntity.getOrderItem();

        StockTransaction tx = new StockTransaction();
        tx.setVariant(item.getVariant());
        tx.setQuantity(returnEntity.getQuantity());
        tx.setType("IN");
        tx.setNote("RETURN_ID_" + returnEntity.getId());
        tx.setReferenceType("returns");
        tx.setReferenceId(returnEntity.getId());
        tx.setCreatedAt(Instant.now());

        stockTransactionRepository.save(tx);

        // Cập nhật stock quantity
        ProductVariant variant = item.getVariant();
        variant.setStockQuantity(variant.getStockQuantity() + returnEntity.getQuantity());
        variantRepository.save(variant);
    }

    private void updateOrderStatusAfterReturn(Order order) {

        int totalOrdered = order.getOrderItems()
                .stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        int totalReturned = returnRepository
                .findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .filter(r -> "APPROVED".equals(r.getStatus()))
                .mapToInt(Return::getQuantity)
                .sum();

        if (totalReturned == totalOrdered) {
            order.setStatus(OrderStatuses.RETURNED);
        } else {
            order.setStatus(OrderStatuses.PARTIALLY_RETURNED);
        }

        order.setReturnedAt(Instant.now());
        orderRepository.save(order);
    }

    private void markReturnedSerialsAsFaulty(Return returnEntity) {

        OrderItem orderItem = returnEntity.getOrderItem();
        int returnQty = returnEntity.getQuantity();

        // Lấy serial chưa return
        List<OrderItemSerial> serialLinks =
                orderItemSerialRepository
                        .findByOrderItem_IdAndReturnedFalseOrderByIdAsc(orderItem.getId());

        if (serialLinks.size() < returnQty) {
            throw new RuntimeException("Không đủ serial để xử lý trả hàng");
        }

        for (int i = 0; i < returnQty; i++) {

            OrderItemSerial link = serialLinks.get(i);
            ProductSerial serial = link.getProductSerial();

            // chuyển sang hàng lỗi
            serial.setStatus("FAULTY");

            // đánh dấu serial đã return
            link.setReturned(true);

            productSerialRepository.save(serial);
            orderItemSerialRepository.save(link);
        }
    }

    private void markReturnedSerialsAsIn_Stock(Return returnEntity) {

        OrderItem orderItem = returnEntity.getOrderItem();
        int returnQty = returnEntity.getQuantity();

        List<OrderItemSerial> serialLinks =
                orderItemSerialRepository
                        .findByOrderItem_IdAndReturnedFalseOrderByIdAsc(orderItem.getId());

        if (serialLinks.size() < returnQty) {
            throw new RuntimeException("Không đủ serial để xử lý trả hàng");
        }

        for (int i = 0; i < returnQty; i++) {

            OrderItemSerial link = serialLinks.get(i);
            ProductSerial serial = link.getProductSerial();

            // quay lại kho
            serial.setStatus("IN_STOCK");

            // đánh dấu đã return
            link.setReturned(true);

            productSerialRepository.save(serial);
            orderItemSerialRepository.save(link);
        }
    }

    private ReturnResponse mapToResponse(Return r) {
        return ReturnResponse.builder()
                .id(r.getId())
                .orderId(r.getOrder().getId())
                .orderNumber(r.getOrder().getOrderNumber())
                .orderItemId(r.getOrderItem().getId())
                .productName(r.getOrderItem().getProductName())
                .variantName(r.getOrderItem().getVariantName())
                .quantity(r.getQuantity())
                .reason(r.getReason())
                .refundAmount(r.getRefundAmount())
                .refundMethod(r.getRefundMethod())
                .refundStatus(r.getRefundStatus())
                .status(r.getStatus())
                .note(r.getNote())
                .imageUrl(r.getImageUrl())
                .customerId(r.getOrder().getCustomer() != null ? r.getOrder().getCustomer().getId() : null)
                .customerName(r.getOrder().getCustomer() != null ? r.getOrder().getCustomer().getName() : null)
                .processedBy(r.getProcessedBy()) // ✅ Trả về Integer
                .createdAt(r.getCreatedAt())
                .refundedAt(r.getRefundedAt())
                .build();
    }
}