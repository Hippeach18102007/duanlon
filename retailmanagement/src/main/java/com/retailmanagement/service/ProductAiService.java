package com.retailmanagement.service;

import com.retailmanagement.dto.response.ProductSummaryDto;
import com.retailmanagement.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductAiService {

    private final ProductVariantRepository variantRepository;

    public List<ProductSummaryDto> getTopProducts(int limit) {
        try {
            return variantRepository.searchForAI(null, PageRequest.of(0, limit));
        } catch (Exception e) {
            log.warn("Could not load top products: {}", e.getMessage());
            return List.of();
        }
    }

    public List<ProductSummaryDto> searchProducts(String keyword) {
        try {
            // Tách từ khóa ngắn để query hiệu quả hơn
            String kw = extractMainKeyword(keyword);
            return variantRepository.searchForAI(kw, PageRequest.of(0, 10));
        } catch (Exception e) {
            log.warn("Could not search products: {}", e.getMessage());
            return List.of();
        }
    }

    public String formatProductContext(List<ProductSummaryDto> products) {
        if (products == null || products.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("--- DANH SÁCH SẢN PHẨM CỬA HÀNG ---\n");
        for (ProductSummaryDto p : products) {
            sb.append("• ").append(p.getName());
            if (p.getBrand() != null) sb.append(" | Hãng: ").append(p.getBrand());
            if (p.getCategoryName() != null) sb.append(" | Danh mục: ").append(p.getCategoryName());
            if (p.getMinPrice() != null) {
                if (p.getMinPrice().compareTo(p.getMaxPrice()) == 0)
                    sb.append(" | Giá: ").append(formatVnd(p.getMinPrice()));
                else
                    sb.append(" | Giá: ").append(formatVnd(p.getMinPrice()))
                            .append(" – ").append(formatVnd(p.getMaxPrice()));
            }
            long stock = p.getTotalStock() != null ? p.getTotalStock() : 0;
            sb.append(" | Tồn: ").append(stock > 0 ? stock + " sp" : "Hết hàng");
            if (Boolean.TRUE.equals(p.getIsNew())) sb.append(" | 🆕");
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                String desc = p.getDescription().trim();
                sb.append("\n  ↳ ").append(desc.length() > 100 ? desc.substring(0, 100) + "..." : desc);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String extractMainKeyword(String message) {
        if (message == null) return null;
        // Bỏ các stop words tiếng Việt, giữ từ khóa chính
        String cleaned = message
                .replaceAll("(?i)(cho tôi|bạn có|có không|tìm|xem|muốn mua|tư vấn|gợi ý|giúp tôi)", "")
                .trim();
        return cleaned.length() > 3 ? cleaned : message;
    }

    private String formatVnd(java.math.BigDecimal amount) {
        if (amount == null) return "0 VNĐ";
        return String.format("%,d VNĐ", amount.longValue());
    }
}