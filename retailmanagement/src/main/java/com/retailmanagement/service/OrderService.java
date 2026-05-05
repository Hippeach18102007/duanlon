package com.retailmanagement.service;

import com.retailmanagement.audit.Audit;
import com.retailmanagement.audit.AuditAction;
import com.retailmanagement.audit.AuditModule;
import com.retailmanagement.audit.TargetType;
import com.retailmanagement.constants.OrderStatuses;
import com.retailmanagement.dto.request.CreateOrderItemRequest;
import com.retailmanagement.dto.request.CreateOrderRequest;
import com.retailmanagement.dto.request.UpdateOrderRequest;
import com.retailmanagement.dto.response.BestVoucherSuggestion;
import com.retailmanagement.dto.response.CreateOrderResponse;
import com.retailmanagement.dto.response.DeliveryInfoResponse;
import com.retailmanagement.dto.response.OrderDetailResponse;
import com.retailmanagement.entity.*;
import com.retailmanagement.entity.NotificationType;
import com.retailmanagement.repository.*;
import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SensitiveOperation;
import com.retailmanagement.security.log.SeverityLevel;
import com.retailmanagement.security.service.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final ProductSerialRepository productSerialRepository;

    @lombok.Data
    private static class DiscountCalculation {
        private BigDecimal vipDiscountRate = BigDecimal.ZERO;
        private BigDecimal vipDiscount = BigDecimal.ZERO;
        private BigDecimal spinDiscountRate = BigDecimal.ZERO;
        private BigDecimal spinDiscount = BigDecimal.ZERO;
        private BigDecimal promoDiscount = BigDecimal.ZERO;
        private String promoCode = null;
        private Integer promotionId = null;

        private BigDecimal totalDiscount = BigDecimal.ZERO;
        private boolean hasSpinBonus = false;
        private String discountType = "NONE";
    }

    private final OrderItemSerialRepository orderItemSerialRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final CustomRes customerRepository;
    private final UserRepository userRepository;
    private final CustomerService customerService;
    private final SpinWheelService spinWheelService;
    private final EmailService emailService;
    private final OrderEmailService orderEmailService;
    private final LoyaltyResetService loyaltyResetService;
    private final PromotionRepository promotionRepository;
    private final PromotionService promotionService;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    private String generateOrderNumber() {
        LocalDate today = LocalDate.now();
        Instant start = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        long countToday = orderRepository.countByCreatedAtBetween(start, end);
        return "ORD-" + today.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", countToday + 1);
    }

    private DiscountCalculation calculateDiscount(Customer customer, BigDecimal subtotal) {
        DiscountCalculation result = new DiscountCalculation();
        System.out.println("🔍 DEBUG calculateDiscount:");
        System.out.println("   customerId = " + customer.getId());
        System.out.println("   spinDiscountBonus = " + customer.getSpinDiscountBonus());
        System.out.println("   vipTier = " + customer.getVipTier());
        System.out.println("   subtotal = " + subtotal);

        BigDecimal vipDiscount = BigDecimal.ZERO;
        BigDecimal vipRate = BigDecimal.ZERO;
        String discountType = "NONE";

        VipTier tier = customer.getVipTier();
        if (tier != null) {
            BigDecimal minOrder = tier.getMinOrderToApply();
            if (subtotal.compareTo(minOrder) >= 0) {
                if (tier.isPercentageDiscount()) {
                    vipRate = BigDecimal.valueOf(tier.getDiscountRate() * 100);
                    vipDiscount = subtotal
                            .multiply(BigDecimal.valueOf(tier.getDiscountRate()))
                            .setScale(0, RoundingMode.HALF_UP);
                    discountType = "PERCENTAGE";
                } else {
                    vipDiscount = tier.getDiscountAmount();
                    discountType = "FIXED";
                }
            }
        }

        BigDecimal spinDiscount = BigDecimal.ZERO;
        BigDecimal spinRate = BigDecimal.ZERO;
        boolean hasSpinBonus = false;

        if (customer.getSpinDiscountBonus() != null
                && customer.getSpinDiscountBonus().compareTo(BigDecimal.ZERO) > 0) {
            spinRate = customer.getSpinDiscountBonus();
            spinDiscount = subtotal
                    .multiply(spinRate.divide(BigDecimal.valueOf(100)))
                    .setScale(0, RoundingMode.HALF_UP);
            hasSpinBonus = true;
        }

        result.setVipDiscountRate(vipRate);
        result.setVipDiscount(vipDiscount);
        result.setSpinDiscountRate(spinRate);
        result.setSpinDiscount(spinDiscount);
        result.setTotalDiscount(vipDiscount.add(spinDiscount));
        result.setHasSpinBonus(hasSpinBonus);
        result.setDiscountType(discountType);

        return result;
    }

    private BigDecimal applyPromotionCode(
            String promoCode,
            Customer customer,
            BigDecimal subtotal,
            DiscountCalculation discountResult) {
        if (promoCode == null || promoCode.isBlank())
            return BigDecimal.ZERO;

        String code = promoCode.trim().toUpperCase();

        Promotion promotion = promotionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá '" + code + "' không tồn tại"));

        LocalDateTime now = LocalDateTime.now();
        if (!promotion.getIsActive()
                || promotion.getStartDate().isAfter(now)
                || promotion.getEndDate().isBefore(now)) {
            throw new RuntimeException("Mã giảm giá '" + code + "' đã hết hạn hoặc chưa có hiệu lực");
        }

        if (promotion.getMinOrderAmount() != null
                && subtotal.compareTo(promotion.getMinOrderAmount()) < 0) {
            throw new RuntimeException(String.format(
                    "Mã '%s' yêu cầu đơn hàng tối thiểu %s (đơn hiện tại: %s)",
                    code,
                    formatMoney(promotion.getMinOrderAmount()),
                    formatMoney(subtotal)));
        }

        if (!promotionService.isUsableByLimit(promotion)) {
            throw new RuntimeException("Mã giảm giá '" + code + "' đã đạt giới hạn sử dụng");
        }

        if (customer != null && promotionService.hasCustomerUsedPromotion(promotion.getId(), customer.getId())) {
            throw new RuntimeException("Bạn đã sử dụng mã giảm giá '" + code + "' rồi, mỗi mã chỉ được dùng 1 lần");
        }

        PromotionService.Applicability app = promotionService.parseApplicability(promotion.getApplicabilityJson());
        if (app.customer_types != null && !app.customer_types.isEmpty() && customer != null) {
            String customerTypeStr = customer.getCustomerType() != null ? customer.getCustomerType().name() : "REGULAR";
            if (!app.customer_types.contains(customerTypeStr)) {
                throw new RuntimeException("Mã '" + code + "' không áp dụng cho loại tài khoản của bạn");
            }
        }
        if (app.vip_tiers != null && !app.vip_tiers.isEmpty() && customer != null) {
            String tierStr = customer.getVipTier() != null ? customer.getVipTier().name() : null;
            if (tierStr == null || !app.vip_tiers.contains(tierStr)) {
                throw new RuntimeException(
                        "Mã '" + code + "' chỉ áp dụng cho hạng VIP: " + String.join(", ", app.vip_tiers));
            }
        }

        BigDecimal afterDiscount = promotionService.applyDiscount(
                subtotal,
                promotion.getDiscountType(),
                promotion.getDiscountValue());
        BigDecimal promoDiscount = subtotal.subtract(afterDiscount);
        if (promoDiscount.compareTo(subtotal) > 0) {
            promoDiscount = subtotal;
        }

        discountResult.setPromoDiscount(promoDiscount);
        discountResult.setPromoCode(code);
        discountResult.setPromotionId(promotion.getId());

        return promoDiscount;
    }

    @SensitiveOperation(action = ActionType.CREATE_OPERATION, entity = "ORDER", description = "Create new order", severity = SeverityLevel.MEDIUM)
    @Audit(module = AuditModule.ORDER, action = AuditAction.CREATE, targetType = TargetType.ORDER)
    public CreateOrderResponse createOrder(CreateOrderRequest request, Integer userId) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        System.out.println("🔍 Customer fetched: id=" + customer.getId()
                + ", spinBonus=" + customer.getSpinDiscountBonus());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setCustomer(customer);
        order.setUser(user);
        order.setChannel(request.getChannel());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatuses.PENDING);
        order.setPaymentStatus("UNPAID");
        order.setNotes(request.getNotes());
        order.setSubtotal(BigDecimal.ZERO);
        order.setDiscountTotal(BigDecimal.ZERO);
        order.setTaxTotal(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setOrderNumber(generateOrderNumber());
        String finalAddress = (request.getShippingAddress() != null && !request.getShippingAddress().isBlank())
                ? request.getShippingAddress()
                : customer.getAddress();
        order.setShippingAddress(finalAddress);
        if ((customer.getAddress() == null || customer.getAddress().isBlank())
                && finalAddress != null && !finalAddress.isBlank()) {
            customer.setAddress(finalAddress);
            customerRepository.save(customer);
        }
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        order = orderRepository.save(order);
        System.out.println(">>> STEP 1: order saved, id=" + order.getId());

        // ✅ FIX: Phí vận chuyển = 0 (theo yêu cầu bỏ phí ship)
        BigDecimal shippingFee = BigDecimal.ZERO;
        order.setShippingFee(shippingFee);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found"));
            subtotal = subtotal.add(
                    variant.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        DiscountCalculation discountCalc = calculateDiscount(customer, subtotal);

        String promoCode = request.getPromotionCode();
        if (promoCode != null && !promoCode.isBlank()) {
            BigDecimal promoDiscount = applyPromotionCode(promoCode, customer, subtotal, discountCalc);
            discountCalc.setTotalDiscount(
                    discountCalc.getTotalDiscount().add(promoDiscount));
            order.setAppliedPromotionCode(discountCalc.getPromoCode());
        }

        BigDecimal effectiveDiscount = discountCalc.getTotalDiscount();

        BigDecimal totalDiscountRate = BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
            totalDiscountRate = effectiveDiscount
                    .divide(subtotal, 4, RoundingMode.HALF_UP);
        }

        List<CreateOrderResponse.Item> responseItems = new ArrayList<>();
        BigDecimal totalLineDiscount = BigDecimal.ZERO;
        int itemIndex = 0;

        for (CreateOrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Variant not found"));

            int available;
            if ("OFFLINE".equals(request.getChannel())) {
                available = productSerialRepository
                        .countByVariantIdAndStatus(variant.getId(), "IN_STOCK");
            } else {
                available = variant.getStockQuantity() - variant.getReservedQty();
            }
            if (available < itemReq.getQuantity()) {
                throw new RuntimeException("Not enough stock for " + variant.getVariantName());
            }

            BigDecimal lineSubtotal = variant.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            boolean isLastItem = (itemIndex + 1 == request.getItems().size());
            itemIndex++;

            // Phân bổ discount
            BigDecimal lineDiscount;
            if (isLastItem) {
                lineDiscount = effectiveDiscount.subtract(totalLineDiscount);
            } else {
                lineDiscount = lineSubtotal
                        .multiply(totalDiscountRate)
                        .setScale(0, RoundingMode.HALF_UP);
                totalLineDiscount = totalLineDiscount.add(lineDiscount);
            }

            // ✅ FIX: shippingFee = 0, lineShipping = 0
            BigDecimal lineShipping = BigDecimal.ZERO;

            // lineTotal = tiền hàng - discount (không có shipping)
            BigDecimal lineTotal = lineSubtotal.subtract(lineDiscount);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setVariant(variant);
            item.setProduct(variant.getProduct());
            item.setProductName(variant.getProduct().getName());
            item.setVariantName(variant.getVariantName());
            item.setSku(variant.getSku());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(variant.getPrice());
            item.setDiscount(lineDiscount);
            item.setShippingFee(lineShipping);
            item.setLineTotal(lineTotal);
            item.setCreatedAt(Instant.now());
            orderItemRepository.save(item);
            System.out.println(">>> STEP 2: item saved, id=" + item.getId());

            // Removed auto-assign serials for OFFLINE orders; serials will be assigned manually later.

            variant.setReservedQty(variant.getReservedQty() + itemReq.getQuantity());
            variantRepository.save(variant);

            StockTransaction st = new StockTransaction();
            st.setVariant(variant);
            st.setQuantity(itemReq.getQuantity());
            st.setType("RESERVE");
            st.setReferenceType("orders");
            st.setReferenceId(order.getId());
            st.setCreatedBy(user);
            st.setCreatedAt(Instant.now());
            stockTransactionRepository.save(st);
            System.out.println(">>> STEP 3: stock transaction saved");
            List<String> savedSerials = orderItemSerialRepository
                    .findByOrderItem(item)
                    .stream()
                    .map(ois -> ois.getProductSerial().getSerialNumber())
                    .toList();

            responseItems.add(new CreateOrderResponse.Item(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getVariant().getId(),
                    item.getProductName(),
                    item.getVariantName(),
                    item.getSku(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getDiscount(),
                    item.getShippingFee(),
                    item.getLineTotal(),
                    item.getUnitPrice(),
                    savedSerials
            ));
        }

        order.setSubtotal(subtotal);
        order.setDiscountTotal(discountCalc.getTotalDiscount());
        order.setTotalAmount(subtotal.subtract(discountCalc.getTotalDiscount()));
        order.setSpinDiscountRate(discountCalc.getSpinDiscountRate());
        order.setSpinDiscount(discountCalc.getSpinDiscount());

        StringBuilder notes = new StringBuilder();
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            notes.append(request.getNotes()).append(" | ");
        }

        VipTier tier = customer.getVipTier();
        if (tier != null && discountCalc.getVipDiscount().compareTo(BigDecimal.ZERO) > 0) {
            if ("FIXED".equals(discountCalc.getDiscountType())) {
                notes.append("VIP ").append(tier.getDisplayName())
                        .append(": -").append(formatMoney(discountCalc.getVipDiscount()))
                        .append(" (đơn từ ").append(formatMoney(tier.getMinOrderToApply())).append(")");
            } else {
                notes.append("VIP ").append(tier.getDisplayName())
                        .append(": -").append(String.format("%.0f%%", discountCalc.getVipDiscountRate()))
                        .append(" (-").append(formatMoney(discountCalc.getVipDiscount())).append(")");
            }
        } else if (tier != null) {
            notes.append("VIP ").append(tier.getDisplayName())
                    .append(": không áp dụng (đơn < ").append(formatMoney(tier.getMinOrderToApply())).append(")");
        }

        // ✅ FIX: Đã bỏ block "Ship VIP: -50%..." vì phí ship = 0

        if (discountCalc.isHasSpinBonus()) {
            if (notes.length() > 0)
                notes.append(" | ");
            notes.append("Spin: -").append(String.format("%.0f%%", discountCalc.getSpinDiscountRate()))
                    .append(" (-").append(formatMoney(discountCalc.getSpinDiscount())).append(")");
        }

        if (discountCalc.getPromoCode() != null) {
            if (notes.length() > 0)
                notes.append(" | ");
            notes.append("Mã: ").append(discountCalc.getPromoCode())
                    .append(": -").append(formatMoney(discountCalc.getPromoDiscount()));
        }

        order.setNotes(notes.toString());
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        System.out.println(">>> STEP 4: order updated");

        if (discountCalc.isHasSpinBonus()) {
            System.out.println(">>> STEP 5: calling useBonus");
            spinWheelService.useBonus(customer.getId(), order.getId(), request.getSpinHistoryId());
            System.out.println(">>> STEP 5: useBonus done");
        }

        System.out.println(">>> STEP 6: building response");
        if (discountCalc.getPromotionId() != null) {
            promotionService.recordRedemption(discountCalc.getPromotionId(), 1L);
            promotionService.recordCustomerUsage(
                    discountCalc.getPromotionId(),
                    customer.getId(),
                    order.getId());
        }

        CreateOrderResponse response = new CreateOrderResponse();
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCustomerId(customer.getId());
        response.setCustomerName(customer.getName());
        response.setStaffId(user.getId());
        response.setStaffUsername(user.getUsername());
        response.setSubtotal(order.getSubtotal());
        response.setDiscountTotal(order.getDiscountTotal());
        response.setTaxTotal(order.getTaxTotal());
        response.setShippingFee(order.getShippingFee());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setItems(responseItems);
        response.setVipDiscountRate(discountCalc.getVipDiscountRate());
        response.setVipDiscount(discountCalc.getVipDiscount());
        response.setSpinDiscountRate(discountCalc.getSpinDiscountRate());
        response.setSpinDiscount(discountCalc.getSpinDiscount());
        response.setHasSpinBonus(discountCalc.isHasSpinBonus());
        response.setPromoCode(discountCalc.getPromoCode());
        response.setPromoDiscount(discountCalc.getPromoDiscount());

        emailService.sendOrderCreatedEmail(order);

        return response;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null)
            return "0";
        return String.format(java.util.Locale.US, "%,d", amount.longValue());
    }

    @SensitiveOperation(action = ActionType.UPDATE_OPERATION, entity = "ORDER", description = "Update order info", severity = SeverityLevel.MEDIUM)
    @Audit(module = AuditModule.ORDER, action = AuditAction.UPDATE, targetType = TargetType.ORDER)
    public void updateOrder(Long orderId, UpdateOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!OrderStatuses.PENDING.equals(order.getStatus())) {
            throw new RuntimeException("Chỉ được chỉnh sửa đơn PENDING");
        }
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (OrderStatuses.CANCELLED.equals(order.getStatus()))
            throw new IllegalStateException("Order already cancelled");
        if (OrderStatuses.DELIVERED.equals(order.getStatus()))
            throw new IllegalStateException("Delivered order cannot be cancelled");

        order.getOrderItems().forEach(item -> {
            ProductVariant variant = variantRepository.findById(item.getVariant().getId())
                    .orElseThrow(() -> new RuntimeException("Variant not found: " + item.getVariant().getId()));
            variant.setReservedQty(Math.max(0, variant.getReservedQty() - item.getQuantity()));
            variantRepository.save(variant);
        });

        order.setStatus(OrderStatuses.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setNotes(reason);

        restoreStock(order);

        if ("PAID".equals(order.getPaymentStatus()) && order.getCustomer() != null) {
            customerService.deductLoyaltyPoints(
                    order.getCustomer().getId(),
                    order.getTotalAmount(),
                    "CANCEL_ORDER",
                    "orders",
                    orderId);
        }

        boolean storeFault = isStoreFault(reason);

        if (storeFault && order.getCustomer() != null) {
            if (order.getAppliedPromotionCode() != null) {
                Promotion promotion =
                        promotionRepository
                                .findByCode(order.getAppliedPromotionCode())
                                .orElse(null);

                if (promotion != null) {
                    promotionService.rollbackPromotionUsage(
                            promotion.getId(),
                            order.getCustomer().getId(),
                            order.getId()
                    );
                }
            }
            spinWheelService.restoreBonusByOrder(order.getId());
        }

        orderRepository.save(order);
        emailService.sendOrderCancelledEmail(order, reason);
    }

    @SensitiveOperation(action = ActionType.DELETE_OPERATION, entity = "ORDER", description = "Delete order", severity = SeverityLevel.HIGH)
    @Audit(module = AuditModule.ORDER, action = AuditAction.DELETE, targetType = TargetType.ORDER)
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!OrderStatuses.PENDING.equals(order.getStatus()))
            throw new RuntimeException("Chỉ được xóa đơn PENDING");
        orderItemRepository.deleteAll(order.getOrderItems());
        orderRepository.delete(order);
    }

    public OrderDetailResponse getOrderDetailByNumber(String orderNumber, CustomUserDetails user) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
        return getOrderDetail(order.getId(), user);
    }

    public OrderDetailResponse getOrderDetail(Long orderId, CustomUserDetails user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean isCustomer = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        if (isCustomer) {
            Long customerIdOfOrder = order.getCustomer().getId().longValue();
            if (!customerIdOfOrder.equals(Long.valueOf(user.getCustomerId()))) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to view this order");
            }
        }

        List<CreateOrderResponse.Item> items = order.getOrderItems().stream()
                .map(i -> {
                    List<String> serials = orderItemSerialRepository
                            .findByOrderItem(i)
                            .stream()
                            .map(ois -> ois.getProductSerial().getSerialNumber())
                            .toList();

                    return new CreateOrderResponse.Item(
                            i.getId(),
                            i.getProduct().getId(),
                            i.getVariant().getId(),
                            i.getProductName(),
                            i.getVariantName(),
                            i.getSku(),
                            i.getQuantity(),
                            i.getUnitPrice(),
                            i.getDiscount(),
                            i.getShippingFee(),
                            i.getLineTotal(),
                            i.getUnitPrice(),
                            serials
                    );
                }).toList();
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getChannel(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getCustomer().getEmail(),
                order.getCustomer().getPhone(),
                (order.getShippingAddress() != null && !order.getShippingAddress().isBlank()) ? order.getShippingAddress() : order.getCustomer().getAddress(),
                order.getUser().getId(),
                order.getUser().getUsername(),
                order.getNotes(),
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getTaxTotal(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getDeliveryProofUrl(),
                order.getShipperConfirmedAt(),
                order.getCreatedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getPaidAt(),
                order.getShippedAt(),
                order.getUpdatedAt(),
                items,
                order.getAppliedPromotionCode(),
                order.getAppliedPromotionJson());
    }

    @Transactional
    public void markAsProcessing(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        String channel = order.getChannel();
        if ("ONLINE".equalsIgnoreCase(channel)
                && !OrderStatuses.PAID.equals(order.getStatus())) {
            throw new IllegalStateException("Only PAID ONLINE orders can be processed");
        }

        if (!"OFFLINE".equals(order.getChannel())) {
            for (OrderItem item : order.getOrderItems()) {
                long assigned = orderItemSerialRepository.findByOrderItem(item).size();
                if (assigned < item.getQuantity()) {
                    throw new RuntimeException(
                            "Chưa đủ serial cho \"" + item.getProductName()
                                    + "\" (" + assigned + "/" + item.getQuantity() + ")");
                }
            }
        }

        order.setStatus(OrderStatuses.PROCESSING);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
    }

    @Transactional
    public void markAsShipping(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!OrderStatuses.PROCESSING.equals(order.getStatus())) {
            throw new IllegalStateException("Only PROCESSING orders can be processed");
        }
        order.setStatus(OrderStatuses.SHIPPING);
        order.setUpdatedAt(Instant.now());
        order.setShippedAt(Instant.now());
        orderRepository.save(order);

        String channel = order.getChannel();
        if ("ONLINE".equalsIgnoreCase(channel)) {
            orderEmailService.sendShippingInProgressEmail(order);
        } else if ("OFFLINE".equalsIgnoreCase(channel)) {
            orderEmailService.sendReadyForPickupEmail(order);
        }
    }

    @Transactional
    public void markAsDelivered(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getDeliveredAt() != null) {
            return;
        }

        if (!OrderStatuses.SHIPPING.equals(order.getStatus()))
            throw new IllegalStateException("Only SHIPPING orders can be delivered");

        for (OrderItem item : order.getOrderItems()) {
            ProductVariant variant = variantRepository.findById(
                    item.getVariant().getId()).orElseThrow();

            int newReserved = Math.max(0, variant.getReservedQty() - item.getQuantity());
            variant.setReservedQty(newReserved);
            variantRepository.save(variant);

            int actualStock = productSerialRepository
                    .countByVariantIdAndStatus(variant.getId(), "IN_STOCK");
            variant.setStockQuantity(actualStock);
            variantRepository.save(variant);

            StockTransaction tx = new StockTransaction();
            tx.setVariant(variant);
            tx.setQuantity(-item.getQuantity());
            tx.setType("STOCK_OUT");
            tx.setReferenceType("orders");
            tx.setReferenceId(order.getId());
            tx.setCreatedAt(Instant.now());
            tx.setCreatedBy(order.getUser());
            stockTransactionRepository.save(tx);
        }

        order.setStatus(OrderStatuses.DELIVERED);
        order.setDeliveredAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        if (order.getCustomer() != null &&
                "PAID".equals(order.getPaymentStatus())) {
            customerService.addLoyaltyPoints(
                    order.getCustomer().getId(),
                    order.getTotalAmount());
        }
        orderEmailService.sendDeliveredEmail(order);
    }

    private void recalcOrderTotal(Order order) {
        BigDecimal subtotal = order.getOrderItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal
                .subtract(order.getDiscountTotal())
                .add(order.getTaxTotal())
                .add(order.getShippingFee()));
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            StockTransaction tx = new StockTransaction();
            tx.setVariant(item.getVariant());
            tx.setQuantity(item.getQuantity());
            tx.setType("IN");
            tx.setNote("CANCEL_ORDER");
            tx.setCreatedAt(Instant.now());
            stockTransactionRepository.save(tx);
        }
    }

    public OrderDetailResponse getOrderBySerial(String serialNumber, CustomUserDetails user) {
        ProductSerial serial = productSerialRepository
                .findBySerialNumber(serialNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy serial: " + serialNumber));

        OrderItemSerial ois = orderItemSerialRepository
                .findByProductSerial(serial)
                .orElseThrow(() -> new RuntimeException(
                        "Serial này chưa được gán với đơn hàng nào"));

        Long orderId = ois.getOrderItem().getOrder().getId();
        return getOrderDetail(orderId, user);
    }

    @Transactional
    public void assignSerialToOrderItem(Long orderId, Long itemId, String serialNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        boolean isPaidOrder = OrderStatuses.PAID.equals(order.getStatus());
        boolean isCashOrder = "CASH".equalsIgnoreCase(order.getPaymentMethod());
        boolean isOnlineOrOffline = "ONLINE".equalsIgnoreCase(order.getChannel())
                || "OFFLINE".equalsIgnoreCase(order.getChannel());

        if (!isPaidOrder && !(isCashOrder && isOnlineOrOffline)) {
            throw new RuntimeException("Chỉ được gán serial cho đơn PAID hoặc đơn CASH (ONLINE/OFFLINE)");
        }

        OrderItem orderItem = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order item"));

        if (!orderItem.getOrder().getId().equals(orderId)) {
            throw new RuntimeException("Order item không thuộc đơn hàng này");
        }

        List<OrderItemSerial> existing = orderItemSerialRepository.findByOrderItem(orderItem);
        if (existing.size() >= orderItem.getQuantity()) {
            throw new RuntimeException("Sản phẩm này đã đủ serial ("
                    + orderItem.getQuantity() + "/" + orderItem.getQuantity() + ")");
        }

        String trimmed = serialNumber.trim();
        ProductSerial serial = productSerialRepository.findBySerialNumber(trimmed)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy serial: " + trimmed));

        if (!"IN_STOCK".equals(serial.getStatus())) {
            throw new RuntimeException("Serial " + trimmed + " không ở trạng thái IN_STOCK"
                    + " (hiện tại: " + serial.getStatus() + ")");
        }

        if (!serial.getVariantId().equals(orderItem.getVariant().getId())) {
            throw new RuntimeException("Serial " + trimmed + " không thuộc biến thể sản phẩm này");
        }

        if (orderItemSerialRepository.findByProductSerial(serial).isPresent()) {
            throw new RuntimeException("Serial " + trimmed + " đã được gán cho đơn hàng khác");
        }

        boolean duplicate = existing.stream()
                .anyMatch(ois -> ois.getProductSerial().getSerialNumber().equals(trimmed));
        if (duplicate) {
            throw new RuntimeException("Serial " + trimmed + " đã được gán cho sản phẩm này rồi");
        }

        serial.setStatus("SOLD");
        productSerialRepository.save(serial);

        OrderItemSerial ois = new OrderItemSerial();
        ois.setOrderItem(orderItem);
        ois.setProductSerial(serial);
        ois.setReturned(false);
        orderItemSerialRepository.save(ois);
    }

    @Transactional
    public void removeSerialFromOrderItem(Long orderId, Long itemId, String serialNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        boolean isPaidOrder = OrderStatuses.PAID.equals(order.getStatus());
        boolean isCashOrder = "CASH".equalsIgnoreCase(order.getPaymentMethod());
        boolean isOnlineOrOffline = "ONLINE".equalsIgnoreCase(order.getChannel())
                || "OFFLINE".equalsIgnoreCase(order.getChannel());

        if (!isPaidOrder && !(isCashOrder && isOnlineOrOffline)) {
            throw new RuntimeException("Chỉ được xóa serial cho đơn PAID hoặc đơn CASH (ONLINE/OFFLINE)");
        }

        OrderItem orderItem = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order item"));

        String trimmed = serialNumber.trim();
        List<OrderItemSerial> list = orderItemSerialRepository.findByOrderItem(orderItem);
        OrderItemSerial toDelete = list.stream()
                .filter(ois -> ois.getProductSerial().getSerialNumber().equals(trimmed))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Serial " + trimmed + " chưa được gán cho sản phẩm này"));

        ProductSerial serial = toDelete.getProductSerial();
        serial.setStatus("IN_STOCK");
        productSerialRepository.save(serial);

        orderItemSerialRepository.delete(toDelete);
    }

    public List<BestVoucherSuggestion> suggestVouchersForCart(
            Integer customerId, BigDecimal subtotal) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return promotionService.suggestBestVouchers(customer, subtotal, 5);
    }

    @Transactional
    public void uploadDeliveryProof(Long orderId, MultipartFile image) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (image == null || image.isEmpty()) {
            throw new RuntimeException("image không được rỗng");
        }

        String imageUrl;
        try {
            imageUrl = cloudinaryService.uploadFile(image);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
        }

        order.setDeliveryProofUrl(imageUrl);
        order.setShipperConfirmedAt(Instant.now());
        orderRepository.save(order);

        Customer customer = order.getCustomer();
        if (customer == null) {
            return;
        }

        String message = "Đơn hàng #" + order.getOrderNumber() + " đã được giao tới. Vui lòng xác nhận!";
        notificationService.createAndSaveNotification(
                customer.getId(),
                NotificationType.ORDER_STATUS,
                "Đơn hàng đã giao",
                message);

        emailService.sendDeliveryProofEmail(order);
    }

    @Transactional
    public DeliveryInfoResponse getDeliveryInfo(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!OrderStatuses.SHIPPING.equals(order.getStatus())) {
            throw new RuntimeException("Order is not in SHIPPING status");
        }

        String address = order.getCustomer() != null ? order.getCustomer().getAddress() : null;
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;
        String customerPhone = order.getCustomer() != null ? order.getCustomer().getPhone() : null;

        return DeliveryInfoResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .customerName(customerName)
                .customerPhone(customerPhone)
                .shippingAddress(order.getShippingAddress())
                .deliveryProofUrl(order.getDeliveryProofUrl())
                .shipperConfirmedAt(order.getShipperConfirmedAt())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .build();
    }

    private boolean isStoreFault(String reason) {
        if (reason == null) return false;
        String r = reason.toUpperCase();
        return r.contains("OUT_OF_STOCK")
                || r.contains("STORE_ERROR")
                || r.contains("WRONG_ITEM")
                || r.contains("DAMAGED")
                || r.contains("SHOP_FAULT");
    }
}
