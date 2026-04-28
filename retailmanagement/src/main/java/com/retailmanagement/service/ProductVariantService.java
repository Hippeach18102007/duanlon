package com.retailmanagement.service;

import com.retailmanagement.dto.request.ProductVariantRequest;
import com.retailmanagement.dto.response.ProductVariantResponse;
import com.retailmanagement.entity.Product;
import com.retailmanagement.entity.ProductVariant;
import com.retailmanagement.repository.ProductRepository;
import com.retailmanagement.repository.ProductSerialRepository;
import com.retailmanagement.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductSerialRepository productSerialRepository;
    private final ProductSerialService productSerialService;

    public List<ProductVariantResponse> getVariantsByProductId(Integer productId) {
        return productVariantRepository.findByProduct_Id(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductVariantResponse getVariantById(Integer id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể với ID: " + id));
        return mapToResponse(variant);
    }

    @Transactional
    public ProductVariantResponse createVariant(Integer productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm gốc"));

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setCurrencyCode("VND");
        variant.setCreatedAt(Instant.now());
        mapRequestToEntity(request, variant);

        ProductVariant savedVariant = productVariantRepository.save(variant);

        // ← Gen serial ngay khi tạo nếu có stock
//        int initStock = request.getStockQuantity() != null ? request.getStockQuantity() : 0;
//        if (initStock > 0) {
//            productSerialService.generateAndSave(savedVariant.getId(), initStock);
//            System.out.println("✅ Gen " + initStock + " serial cho variant mới: " + savedVariant.getId());
//        }

        return mapToResponse(savedVariant);
    }

    @Transactional
    public ProductVariantResponse updateVariant(Integer id, ProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể"));

        // ← Lấy stock hiện tại trước khi update
        int currentStock = productSerialRepository.countByVariantIdAndStatus(variant.getId(), "IN_STOCK");
        int newStock = request.getStockQuantity() != null ? request.getStockQuantity() : 0;
        int diff = newStock - currentStock;

        mapRequestToEntity(request, variant);
        variant.setUpdatedAt(Instant.now());

        ProductVariant updatedVariant = productVariantRepository.save(variant);

        // ← Nếu tăng số lượng → gen thêm serial
//        if (diff > 0) {
//            productSerialService.generateAndSave(variant.getId(), diff);
//            System.out.println("✅ Gen thêm " + diff + " serial cho variant: " + variant.getId());
//        }

        return mapToResponse(updatedVariant);
    }

    @Transactional
    public void deleteVariant(Integer id) {
        if (!productVariantRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy biến thể để xóa");
        }
        productVariantRepository.deleteById(id);
    }

    private void mapRequestToEntity(ProductVariantRequest request, ProductVariant variant) {
        variant.setVariantName(request.getVariantName());
        variant.setSku(request.getSku());
        variant.setBarcode(request.getBarcode());
        variant.setPrice(request.getPrice());
        variant.setCostPrice(request.getCostPrice());
        if (variant.getId() == null) {
            variant.setStockQuantity(0);
        }
        variant.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        variant.setReservedQty(0);
        variant.setAttributesJson(request.getAttributesJson());
        variant.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        if (variant.getUpdatedAt() == null) {
            variant.setUpdatedAt(Instant.now());
        }
    }

    private ProductVariantResponse mapToResponse(ProductVariant variant) {
        ProductVariantResponse response = new ProductVariantResponse();
        response.setId(variant.getId());
        response.setProductId(variant.getProduct().getId());
        response.setVariantName(variant.getVariantName());
        response.setSku(variant.getSku());
        response.setBarcode(variant.getBarcode());
        response.setCurrencyCode(variant.getCurrencyCode());
        response.setPrice(variant.getPrice());
        response.setCostPrice(variant.getCostPrice());
        int actualStock = productSerialRepository.countByVariantIdAndStatus(variant.getId(), "IN_STOCK");
        response.setStockQuantity(actualStock);
        if (variant.getStockQuantity() == null || variant.getStockQuantity() != actualStock) {
            variant.setStockQuantity(actualStock);
            productVariantRepository.save(variant);
        }
        response.setStockQuantity(variant.getStockQuantity());
        response.setAttributesJson(variant.getAttributesJson());
        response.setIsActive(variant.getIsActive());
        response.setCreatedAt(variant.getCreatedAt());
        return response;
    }
}