package com.retailmanagement.service;

import com.retailmanagement.dto.request.UpsertPriceRequest;
import com.retailmanagement.dto.response.PriceHistoryResponse;
import com.retailmanagement.dto.response.VariantPriceResponse;
import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PricingService {

    private final ProductVariantRepository variantRepo;
    private final PriceHistoryRepository priceHistoryRepo;
    private final SettingService settingService;
    private final PromotionService promotionService;
    private final UserRepository userRepository;
    private final CustomRes customerRepository;

    public PricingService(ProductVariantRepository variantRepo,
            PriceHistoryRepository priceHistoryRepo,
            SettingService settingService,
            PromotionService promotionService,
            UserRepository userRepository,
            CustomRes customerRepository) {
        this.variantRepo = variantRepo;
        this.priceHistoryRepo = priceHistoryRepo;
        this.settingService = settingService;
        this.promotionService = promotionService;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    // ─────────────────────────────────────────────
    // SET PRICE
    // ─────────────────────────────────────────────

    @Transactional
    public PriceHistory setVariantPrice(Integer variantId, UpsertPriceRequest req, Integer userId) {
        ProductVariant v = variantRepo.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("Variant not found"));

        if (req == null) throw new IllegalArgumentException("Body rỗng");
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("price phải >= 0");
        if (req.getCostPrice() != null && req.getCostPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("costPrice phải >= 0");

        User user = resolveUser(userId);

        String currency = resolveCurrency(req.getCurrencyCode());
        Instant now = Instant.now();

        // Đóng bản ghi giá đang active
        priceHistoryRepo
                .findFirstByVariant_IdAndEffectiveToIsNullOrderByEffectiveFromDesc(variantId)
                .ifPresent(cur -> {
                    cur.setEffectiveTo(now);
                    priceHistoryRepo.save(cur);
                });

        // Tạo bản ghi mới
        PriceHistory ph = new PriceHistory();
        ph.setVariant(v);
        ph.setCurrencyCode(currency);
        ph.setPrice(scaleMoney(req.getPrice()));
        ph.setCostPrice(req.getCostPrice() == null ? null : scaleMoney(req.getCostPrice()));
        ph.setReason(resolveReason(req.getReason()));
        ph.setEffectiveFrom(now);
        ph.setEffectiveTo(null);
        ph.setCreatedBy(user);
        ph.setCreatedAt(now);
        priceHistoryRepo.save(ph);

        // Sync giá lên variant
        syncVariantPrice(v, currency, req.getPrice(), req.getCostPrice(), now);

        return ph;
    }

    // ─────────────────────────────────────────────
    // UPDATE HISTORY — tạo bản ghi mới thay vì overwrite
    // ─────────────────────────────────────────────

    @Transactional
    public PriceHistoryResponse updateLatestHistory(Long priceHistoryId, UpsertPriceRequest req,
            Integer userId) {
        PriceHistory current = priceHistoryRepo.findById(priceHistoryId)
                .orElseThrow(() -> new NoSuchElementException("Price history not found"));

        if (current.getEffectiveTo() != null)
            throw new IllegalArgumentException("Chỉ được sửa bản ghi giá hiện tại (effective_to = null)");
        if (req == null) throw new IllegalArgumentException("Body rỗng");
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("price phải >= 0");
        if (req.getCostPrice() != null && req.getCostPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("costPrice phải >= 0");

        Instant now = Instant.now();
        User user = resolveUser(userId);

        // Giữ lại costPrice cũ nếu request không truyền
        BigDecimal newCostPrice = req.getCostPrice() != null
                ? scaleMoney(req.getCostPrice())
                : current.getCostPrice();

        String newCurrency = (req.getCurrencyCode() != null && !req.getCurrencyCode().isBlank())
                ? req.getCurrencyCode().trim().toUpperCase()
                : current.getCurrencyCode();

        String newReason = (req.getReason() != null && !req.getReason().isBlank())
                ? req.getReason().trim()
                : current.getReason();

        // Đóng bản ghi cũ — giữ dấu vết audit
        current.setEffectiveTo(now);
        priceHistoryRepo.save(current);

        // Tạo bản ghi mới
        PriceHistory ph = new PriceHistory();
        ph.setVariant(current.getVariant());
        ph.setCurrencyCode(newCurrency);
        ph.setPrice(scaleMoney(req.getPrice()));
        ph.setCostPrice(newCostPrice);
        ph.setReason(newReason);
        ph.setEffectiveFrom(now);
        ph.setEffectiveTo(null);
        ph.setCreatedBy(user);
        ph.setCreatedAt(now);
        priceHistoryRepo.save(ph);

        // Sync giá lên variant
        syncVariantPrice(current.getVariant(), newCurrency, req.getPrice(),
                req.getCostPrice() != null ? req.getCostPrice() : current.getCostPrice(), now);

        return toPriceHistoryResponse(ph);
    }

    // ─────────────────────────────────────────────
    // DELETE & ROLLBACK
    // ─────────────────────────────────────────────

    @Transactional
    public void deleteLatestAndRollback(Long priceHistoryId) {
        PriceHistory current = priceHistoryRepo.findById(priceHistoryId)
                .orElseThrow(() -> new NoSuchElementException("Price history not found"));

        if (current.getEffectiveTo() != null)
            throw new IllegalArgumentException("Chỉ được xóa bản ghi giá hiện tại (effective_to = null)");

        Integer variantId = current.getVariant().getId();
        priceHistoryRepo.delete(current);

        List<PriceHistory> histories = priceHistoryRepo
                .findByVariant_IdOrderByEffectiveFromDesc(variantId);
        ProductVariant v = variantRepo.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("Variant not found"));

        if (histories.isEmpty()) {
            v.setCurrencyCode(settingService.getDefaultCurrency());
            v.setPrice(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            v.setCostPrice(null);
            v.setUpdatedAt(Instant.now());
            variantRepo.save(v);
            return;
        }

        PriceHistory latest = histories.get(0);
        latest.setEffectiveTo(null);
        priceHistoryRepo.save(latest);

        syncVariantPrice(v, latest.getCurrencyCode(), latest.getPrice(), latest.getCostPrice(),
                Instant.now());
    }

    // ─────────────────────────────────────────────
    // EFFECTIVE PRICE
    // ─────────────────────────────────────────────

    public VariantPriceResponse getEffectivePrice(Integer variantId) {
        return getEffectivePriceForCustomer(variantId, null);
    }

    public VariantPriceResponse getEffectivePriceForCustomer(Integer variantId, Integer customerId) {
        ProductVariant v = variantRepo.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("Variant not found"));
        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;
        LocalDateTime now = LocalDateTime.now();
        BigDecimal basePrice = getBasePriceForCustomer(v, customer);
        Promotion best = promotionService.findBestPromotionForVariantAndCustomer(v, customer, now);
        return buildVariantPriceResponse(v, basePrice, best);
    }

    // ─────────────────────────────────────────────
    // LIST BY PRODUCT
    // ─────────────────────────────────────────────

    public List<VariantPriceResponse> listCurrentPricesByProduct(Integer productId) {
        return listCurrentPricesByProductForCustomer(productId, null);
    }

    public List<VariantPriceResponse> listCurrentPricesByProductForCustomer(Integer productId,
            Integer customerId) {
        List<ProductVariant> variants = variantRepo.findByProduct_Id(productId);
        LocalDateTime now = LocalDateTime.now();
        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;
        return variants.stream().map(v -> {
            BigDecimal basePrice = getBasePriceForCustomer(v, customer);
            Promotion best = promotionService.findBestPromotionForVariantAndCustomer(v, customer, now);
            return buildVariantPriceResponse(v, basePrice, best);
        }).toList();
    }

    // ─────────────────────────────────────────────
    // PRICE HISTORY — phân trang
    // ─────────────────────────────────────────────

    public Page<PriceHistoryResponse> getPriceHistory(Integer variantId, Pageable pageable) {
        return priceHistoryRepo
                .findByVariant_IdOrderByEffectiveFromDesc(variantId, pageable)
                .map(this::toPriceHistoryResponse);
    }

    // ─────────────────────────────────────────────
    // COST WARNING
    // ─────────────────────────────────────────────

    public Map<String, Object> checkPriceBelowCost(Integer variantId) {
        ProductVariant v = variantRepo.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("Variant not found"));
        boolean warning = isBelowCost(v);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("variantId", variantId);
        result.put("variantName", v.getVariantName());
        result.put("sku", v.getSku());
        result.put("price", v.getPrice());
        result.put("costPrice", v.getCostPrice());
        result.put("belowCost", warning);
        if (warning) result.put("message", "⚠️ Giá bán thấp hơn giá nhập!");
        return result;
    }

    // Dùng query tối ưu thay vì findAll()
    public List<Map<String, Object>> getAllConflictsBelowCost() {
        return variantRepo.findVariantsBelowCost().stream()
                .map(v -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("variantId", v.getId());
                    item.put("variantName", v.getVariantName());
                    item.put("sku", v.getSku());
                    item.put("productId", v.getProduct().getId());
                    item.put("price", v.getPrice());
                    item.put("costPrice", v.getCostPrice());
                    item.put("currencyCode", v.getCurrencyCode());
                    item.put("diff", v.getCostPrice().subtract(v.getPrice()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // CALCULATE ORDER
    // ─────────────────────────────────────────────

    public Map<String, Object> calculateOrderPrice(List<Map<String, Object>> items,
            Integer customerId) {
        Customer customer = customerId != null
                ? customerRepository.findById(customerId).orElse(null)
                : null;
        LocalDateTime now = LocalDateTime.now();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        int totalFreeQty = 0;
        List<Map<String, Object>> lineItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Integer variantId = (Integer) item.get("variantId");
            Integer qty = (Integer) item.get("quantity");
            if (variantId == null || qty == null || qty <= 0) continue;

            ProductVariant v = variantRepo.findById(variantId).orElse(null);
            if (v == null) continue;

            BigDecimal basePrice = getBasePriceForCustomer(v, customer);
            Promotion best = promotionService.findBestPromotionForVariantAndCustomer(v, customer, now);
            BigDecimal finalUnitPrice = best != null
                    ? promotionService.computeEffectiveUnitPrice(basePrice, best)
                    : basePrice;

            int freeQty = 0;
            PromotionService.Rules rules = best != null
                    ? promotionService.parseRulesPublic(best.getRulesJson())
                    : null;
            if (rules != null && rules.combo != null
                    && rules.combo.buy_qty != null && rules.combo.get_qty != null) {
                freeQty = (qty / rules.combo.buy_qty) * rules.combo.get_qty;
            }
            totalFreeQty += freeQty;

            BigDecimal lineSubtotal = basePrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal lineFinal = finalUnitPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal lineDiscount = lineSubtotal.subtract(lineFinal);

            subtotal = subtotal.add(lineSubtotal);
            discountTotal = discountTotal.add(lineDiscount);

            Map<String, Object> line = new LinkedHashMap<>();
            line.put("variantId", variantId);
            line.put("variantName", v.getVariantName());
            line.put("quantity", qty);
            line.put("unitPrice", basePrice);
            line.put("finalUnitPrice", finalUnitPrice);
            line.put("freeQty", freeQty);
            line.put("lineSubtotal", lineSubtotal);
            line.put("lineDiscount", lineDiscount);
            line.put("lineTotal", lineFinal);
            if (best != null) {
                line.put("promotionCode", best.getCode());
                line.put("promotionName", best.getName());
            }
            lineItems.add(line);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subtotal", subtotal);
        result.put("discountTotal", discountTotal);
        result.put("finalTotal", subtotal.subtract(discountTotal));
        result.put("freeQty", totalFreeQty);
        result.put("items", lineItems);
        return result;
    }

    // ─────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────

    public Map<String, Object> getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> activePromos = promotionService.list(true);

        // Dùng query tối ưu thay vì findAll()
        List<ProductVariant> belowCostVariants = variantRepo.findVariantsBelowCost();
        List<Map<String, Object>> conflicts = promotionService.detectConflicts();

        long expiringIn3Days = activePromos.stream()
                .filter(p -> p.getEndDate().isBefore(now.plusDays(3)))
                .count();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("activePromotions", activePromos.size());
        dashboard.put("variantsBelowCost", belowCostVariants.size());   // fix: dùng list đã query
        dashboard.put("expiringIn3Days", expiringIn3Days);
        dashboard.put("conflictCount", conflicts.size());               // fix: dùng list đã query
        dashboard.put("promotionConflicts", conflicts);                 // fix: trả list thay vì .size()
        return dashboard;
    }

    // ─────────────────────────────────────────────
    // TO RESPONSE
    // ─────────────────────────────────────────────

    public PriceHistoryResponse toPriceHistoryResponse(PriceHistory ph) {
        PriceHistoryResponse dto = new PriceHistoryResponse();
        dto.setId(ph.getId());
        dto.setVariantId(ph.getVariant().getId());
        dto.setCurrencyCode(ph.getCurrencyCode());
        dto.setPrice(ph.getPrice());
        dto.setCostPrice(ph.getCostPrice());
        dto.setReason(ph.getReason());
        dto.setEffectiveFrom(ph.getEffectiveFrom());
        dto.setEffectiveTo(ph.getEffectiveTo());
        dto.setCreatedByUsername(ph.getCreatedBy() != null ? ph.getCreatedBy().getUsername() : null);
        dto.setCreatedAt(ph.getCreatedAt());

        // Cảnh báo giá bán < giá nhập
        boolean belowCost = ph.getCostPrice() != null
                && ph.getPrice() != null
                && ph.getPrice().compareTo(ph.getCostPrice()) < 0;
        dto.setBelowCost(belowCost);
        if (belowCost) dto.setWarningMessage("⚠️ Giá bán thấp hơn giá nhập!");

        return dto;
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private User resolveUser(Integer userId) {
        if (userId == null || userId <= 0) return null;
        return userRepository.findById(userId).orElse(null);
    }

    private String resolveCurrency(String code) {
        return (code == null || code.isBlank())
                ? settingService.getDefaultCurrency()
                : code.trim().toUpperCase();
    }

    private String resolveReason(String reason) {
        return (reason == null || reason.isBlank()) ? "MANUAL" : reason.trim();
    }

    private boolean isBelowCost(ProductVariant v) {
        return v.getCostPrice() != null
                && v.getPrice() != null
                && v.getPrice().compareTo(v.getCostPrice()) < 0;
    }

    private void syncVariantPrice(ProductVariant v, String currency,
            BigDecimal price, BigDecimal costPrice, Instant now) {
        v.setCurrencyCode(currency);
        v.setPrice(scaleMoney(price));
        v.setCostPrice(costPrice == null ? v.getCostPrice() : scaleMoney(costPrice));
        v.setUpdatedAt(now);
        variantRepo.save(v);
    }

    private BigDecimal getBasePriceForCustomer(ProductVariant variant, Customer customer) {
        BigDecimal basePrice = variant.getPrice();
        if (customer == null) return basePrice;
        VipTier tier = customer.getVipTier();
        if (tier != null && tier.getDiscountRate() != null && tier.getDiscountRate() > 0) {
            BigDecimal multiplier = BigDecimal.ONE
                    .subtract(BigDecimal.valueOf(tier.getDiscountRate()));
            basePrice = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice;
    }

    private VariantPriceResponse buildVariantPriceResponse(ProductVariant v,
            BigDecimal basePrice, Promotion best) {
        VariantPriceResponse r = new VariantPriceResponse();
        r.setVariantId(v.getId());
        r.setProductId(v.getProduct().getId());
        r.setVariantName(v.getVariantName());
        r.setSku(v.getSku());
        r.setCurrencyCode(v.getCurrencyCode());
        r.setPrice(basePrice);
        r.setCostPrice(v.getCostPrice());
        if (best != null) {
            r.setPromotionCode(best.getCode());
            r.setPromotionName(best.getName());
            r.setFinalPrice(promotionService.computeEffectiveUnitPrice(basePrice, best));
            PromotionService.Rules rules = promotionService.parseRulesPublic(best.getRulesJson());
            if (rules != null && rules.combo != null
                    && rules.combo.buy_qty != null && rules.combo.get_qty != null) {
                r.setComboInfo("Mua " + rules.combo.buy_qty + " tặng " + rules.combo.get_qty);
            }
        } else {
            r.setFinalPrice(basePrice);
        }
        return r;
    }

    private static BigDecimal scaleMoney(BigDecimal v) {
        if (v == null) return null;
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}