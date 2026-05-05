package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailResponse {

    private Long orderId;
    private String orderNumber;
    private String channel;
    private String paymentMethod;
    private String status;
    private String paymentStatus;

    private Integer customerId;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private String customerEmail;

    private Integer staffId;
    private String staffUsername;

    private String notes;

    private BigDecimal vipDiscountRate  = BigDecimal.ZERO;
    private BigDecimal vipDiscount      = BigDecimal.ZERO;
    private BigDecimal spinDiscountRate = BigDecimal.ZERO;
    private BigDecimal spinDiscount     = BigDecimal.ZERO;

    private String     promoCode        = null;
    private BigDecimal promoDiscount    = BigDecimal.ZERO;

    private String     comboInfo        = null;

    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal taxTotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;

    private String deliveryProofUrl;
    private Instant shipperConfirmedAt;

    private Instant createdAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private Instant paidAt;

    private Instant shippedAt;
    private Instant updatedAt;
    private List<CreateOrderResponse.Item> items;

    public OrderDetailResponse(
            Long orderId,
            String orderNumber,
            String channel,
            String paymentMethod,
            String status,
            String paymentStatus,
            Integer customerId,
            String customerName,
            String customerEmail,
            String customerPhone,
            String customerAddress,
            Integer staffId,
            String staffUsername,
            String notes,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            String deliveryProofUrl,
            Instant shipperConfirmedAt,
            Instant createdAt,
            Instant deliveredAt,
            Instant cancelledAt,
            Instant paidAt,
            Instant shippedAt,
            Instant updatedAt,
            List<CreateOrderResponse.Item> items
    ) {
        this.orderId       = orderId;
        this.orderNumber   = orderNumber;
        this.channel       = channel;
        this.paymentMethod = paymentMethod;
        this.status        = status;
        this.paymentStatus = paymentStatus;
        this.customerId    = customerId;
        this.customerName  = customerName;
        this.customerAddress = customerAddress;
        this.customerPhone   = customerPhone;
        this.customerEmail   = customerEmail;
        this.staffId       = staffId;
        this.staffUsername = staffUsername;
        this.notes         = notes;
        this.subtotal      = subtotal;
        this.discountTotal = discountTotal;
        this.taxTotal      = taxTotal;
        this.shippingFee   = shippingFee;
        this.totalAmount   = totalAmount;
        this.deliveryProofUrl = deliveryProofUrl;
        this.shipperConfirmedAt = shipperConfirmedAt;
        this.createdAt     = createdAt;
        this.deliveredAt    = deliveredAt;
        this.cancelledAt  = cancelledAt;
        this.paidAt      = paidAt;
        this.shippedAt    = shippedAt;
        this.updatedAt     = updatedAt;
        this.items         = items;

        parseDiscountFromNotes();
    }

    public OrderDetailResponse(
            Long orderId,
            String orderNumber,
            String channel,
            String paymentMethod,
            String status,
            String paymentStatus,
            Integer customerId,
            String customerName,
            String customerAddress,
            String customerPhone,
            String customerEmail,
            Integer staffId,
            String staffUsername,
            String notes,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            String deliveryProofUrl,
            Instant shipperConfirmedAt,
            Instant createdAt,
            Instant deliveredAt,
            Instant cancelledAt,
            Instant paidAt,
            Instant shippedAt,
            Instant updatedAt,
            List<CreateOrderResponse.Item> items,
            String promoCode,
            String appliedPromotionJson
    ) {
        this(orderId, orderNumber, channel, paymentMethod, status, paymentStatus,
                customerId, customerName, customerAddress, customerPhone, customerEmail, staffId, staffUsername, notes,
                subtotal, discountTotal, taxTotal, shippingFee, totalAmount, deliveryProofUrl, shipperConfirmedAt, createdAt, deliveredAt, cancelledAt, paidAt, shippedAt, updatedAt, items);
        this.promoCode = promoCode;
        this.comboInfo = parseComboInfo(appliedPromotionJson);
    }

    private String parseComboInfo(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            if (json.contains("\"buy_qty\"") || json.contains("buyQty")) {
                Integer buy = extractJsonInt(json, "buy_qty");
                if (buy == null) buy = extractJsonInt(json, "buyQty");
                Integer get = extractJsonInt(json, "get_qty");
                if (get == null) get = extractJsonInt(json, "getQty");
                if (buy != null && get != null) {
                    return "Mua " + buy + " tặng " + get;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Integer extractJsonInt(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == start) return null;
        try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return null; }
    }

    private void parseDiscountFromNotes() {
        this.vipDiscountRate  = BigDecimal.ZERO;
        this.vipDiscount      = BigDecimal.ZERO;
        this.spinDiscountRate = BigDecimal.ZERO;
        this.spinDiscount     = BigDecimal.ZERO;
        this.promoDiscount    = BigDecimal.ZERO;

        if (notes == null || notes.isBlank()) return;

        String[] segments = notes.split("\\|");

        for (String seg : segments) {
            String s = seg.trim();

            if (s.startsWith("VIP ") && !s.contains("không áp dụng")) {
                if (s.contains("%")) {
                    int pctIdx = s.indexOf('%');
                    int dashIdx = s.lastIndexOf('-', pctIdx);
                    if (dashIdx >= 0) {
                        String rateStr = s.substring(dashIdx + 1, pctIdx).trim();
                        this.vipDiscountRate = parseSafe(rateStr);
                    }
                    this.vipDiscount = extractParenAmount(s);
                } else if (s.contains("-")) {
                    int dashIdx = s.lastIndexOf('-');
                    int endIdx = s.indexOf('(', dashIdx);
                    String amtStr = endIdx > dashIdx
                            ? s.substring(dashIdx + 1, endIdx).trim()
                            : s.substring(dashIdx + 1).trim();
                    amtStr = amtStr.replace(",", "").replace(".", "");
                    this.vipDiscount = parseSafe(amtStr);
                }
            }

            if (s.startsWith("Spin:")) {
                if (s.contains("%")) {
                    int pctIdx = s.indexOf('%');
                    int dashIdx = s.lastIndexOf('-', pctIdx);
                    if (dashIdx >= 0) {
                        String rateStr = s.substring(dashIdx + 1, pctIdx).trim();
                        this.spinDiscountRate = parseSafe(rateStr);
                    }
                    this.spinDiscount = extractParenAmount(s);
                }
            }

            if (s.startsWith("Mã:")) {
                int lastDash = s.lastIndexOf('-');
                if (lastDash >= 0) {
                    String amtStr = s.substring(lastDash + 1).trim()
                            .replace(",", "").replace(".", "");
                    this.promoDiscount = parseSafe(amtStr);
                }
                String after = s.substring(3).trim();
                int colonIdx = after.indexOf(':');
                if (colonIdx > 0) {
                    this.promoCode = after.substring(0, colonIdx).trim();
                }
            }
        }
    }

    private BigDecimal extractParenAmount(String s) {
        int openParen  = s.lastIndexOf("(-");
        int closeParen = s.lastIndexOf(')');
        if (openParen >= 0 && closeParen > openParen) {
            String raw = s.substring(openParen + 2, closeParen)
                    .replace(".", "")
                    .replace(",", "")
                    .trim();
            return parseSafe(raw);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal parseSafe(String s) {
        try {
            String cleaned = s.trim().replaceAll("[^0-9]", "");
            if (cleaned.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}