package com.retailmanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailmanagement.audit.Audit;
import com.retailmanagement.audit.AuditAction;
import com.retailmanagement.audit.AuditModule;
import com.retailmanagement.audit.TargetType;
import com.retailmanagement.dto.request.AttributeRequest;
import com.retailmanagement.dto.request.ProductRequest;
import com.retailmanagement.dto.response.ImageResponse;
import com.retailmanagement.dto.response.ProductImportResult;
import com.retailmanagement.dto.response.ProductResponse;
import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SensitiveOperation;
import com.retailmanagement.security.log.SeverityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PromotionService promotionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TagRepository tagRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductSerialRepository productSerialRepository;
    private final CloudinaryService cloudinaryService;

    private final String UPLOAD_DIR = "uploads/";

    @SensitiveOperation(
            action = ActionType.CREATE_OPERATION,
            entity = "PRODUCT",
            description = "Create new product",
            severity = SeverityLevel.MEDIUM
    )
    @Audit(
            module = AuditModule.PRODUCT,
            action = AuditAction.CREATE,
            targetType = TargetType.PRODUCT
    )
    @Transactional
    public void createProduct(ProductRequest request) throws IOException {
        Product product = new Product();

        product.setName(request.getName().trim());
        product.setSku(request.getSku().trim().toUpperCase());
        product.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setSoldCount(0);
        product.setIsNew(request.getIsNew() != null ? request.getIsNew() : true);
        product.setIsFaulty(false);

        if (request.getBrand() != null && !request.getBrand().trim().isEmpty()) {
            product.setBrand(request.getBrand().trim());
        }

        String finalDescription = request.getDescription();

        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
        }
        product.setDescription(finalDescription);

        product.setAttributesJson(request.getAttributes());

        Product savedProduct = productRepository.save(product);

        if (request.getCategoryIds() != null) {
            boolean first = true;
            for (Integer catId : request.getCategoryIds()) {
                ProductCategory pc = new ProductCategory();
                ProductCategoryId pcId = new ProductCategoryId(savedProduct.getId(), catId);
                pc.setId(pcId);
                pc.setCategory(categoryRepository.getReferenceById(catId));
                pc.setIsPrimary(first);
                pc.setCreatedAt(Instant.now());
                productCategoryRepository.save(pc);
                first = false;
            }
        }

        if (request.getGalleryImages() != null) {
            int sortOrder = 0;
            for (MultipartFile file : request.getGalleryImages()) {
                if (!file.isEmpty()) {
                    String url = cloudinaryService.uploadFile(file);
                    Image image = new Image();
                    image.setProduct(savedProduct);
                    image.setUrl(url);
                    image.setIsPrimary(sortOrder == 0);
                    image.setSortOrder(sortOrder++);
                    image.setCreatedAt(Instant.now());
                    imageRepository.save(image);
                }
            }
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            for (Integer tagId : request.getTagIds()) {
                ProductTag pt = new ProductTag();
                ProductTagId ptId = new ProductTagId();
                ptId.setProductId(savedProduct.getId());
                ptId.setTagId(tagId);

                pt.setId(ptId);
                pt.setProduct(savedProduct);
                pt.setTag(tagRepository.getReferenceById(tagId));
                pt.setCreatedAt(Instant.now());

                productTagRepository.save(pt);
            }
        }
    }

    @SensitiveOperation(
            action = ActionType.UPDATE_OPERATION,
            entity = "PRODUCT",
            description = "Update product info",
            severity = SeverityLevel.MEDIUM
    )
    @Audit(
            module = AuditModule.PRODUCT,
            action = AuditAction.UPDATE,
            targetType = TargetType.PRODUCT
    )
    @Transactional
    public void updateProduct(Integer id, ProductRequest request) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setUpdatedAt(LocalDateTime.now());

        if (request.getIsNew() != null) product.setIsNew(request.getIsNew());
        if (request.getIsFaulty() != null) product.setIsFaulty(request.getIsFaulty());

        if (request.getBrand() != null) product.setBrand(request.getBrand().trim());

        String finalDescription = request.getDescription();
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
        }
        product.setDescription(finalDescription);
        product.setAttributesJson(request.getAttributes());

        if (request.getIdsToDelete() != null && !request.getIdsToDelete().isEmpty()) {
            imageRepository.deleteAllById(request.getIdsToDelete());
        }

        if (request.getGalleryImages() != null) {
            for (MultipartFile file : request.getGalleryImages()) {
                if (!file.isEmpty()) {
                    String url = cloudinaryService.uploadFile(file);
                    Image image = new Image();
                    image.setProduct(product);
                    image.setUrl(url);
                    image.setIsPrimary(false);
                    image.setSortOrder(1);
                    image.setCreatedAt(Instant.now());
                    imageRepository.save(image);
                }
            }
        }

        productTagRepository.deleteByProduct_Id(product.getId());
        productTagRepository.flush();
        if (request.getCategoryIds() != null) {
            boolean first = true;
            for (Integer catId : request.getCategoryIds()) {
                ProductCategory pc = new ProductCategory();
                ProductCategoryId pcId = new ProductCategoryId(product.getId(), catId);
                pc.setId(pcId);
                pc.setCategory(categoryRepository.getReferenceById(catId));
                pc.setIsPrimary(first);
                pc.setCreatedAt(Instant.now());
                productCategoryRepository.save(pc);
                first = false;
            }
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            for (Integer tagId : request.getTagIds()) {
                ProductTag pt = new ProductTag();
                ProductTagId ptId = new ProductTagId();
                ptId.setProductId(product.getId());
                ptId.setTagId(tagId);

                pt.setId(ptId);
                pt.setProduct(product);
                pt.setTag(tagRepository.getReferenceById(tagId));
                pt.setCreatedAt(Instant.now());

                productTagRepository.save(pt);
            }
        }

        productRepository.save(product);
    }

    @Transactional
    public Page<ProductResponse> getProducts(int page, List<Integer> categoryIds, String keyword, String sortBy, boolean inStockOnly, Integer tagId, LocalDateTime startDate, LocalDateTime endDate, Boolean isNew, Boolean isFaulty, String brand, BigDecimal minPrice, BigDecimal maxPrice) {
        Sort sortObj = Sort.by(Sort.Direction.DESC, "id");
        if (sortBy != null) {
            switch (sortBy) {
                case "recently_updated":
                    sortObj = Sort.by(Sort.Direction.DESC, "updated_at");
                    break;
                case "oldest":
                    sortObj = Sort.by(Sort.Direction.ASC, "id");
                    break;
                case "name_asc":
                    sortObj = Sort.by(Sort.Direction.ASC, "name");
                    break;
                case "name_desc":
                    sortObj = Sort.by(Sort.Direction.DESC, "name");
                    break;
                case "best_selling":
                    sortObj = JpaSort.unsafe(Sort.Direction.DESC, "sold_count");
                    break;
                case "newest_arrival":
                    sortObj = Sort.by(Sort.Direction.DESC, "created_at");
                    break;
            }
        }

        Pageable pageable = PageRequest.of(page, 20, sortObj);

        String searchKey = (keyword != null && !keyword.trim().isEmpty()) ? "%" + keyword.trim() + "%" : null;
        boolean hasCategory = (categoryIds != null && !categoryIds.isEmpty());
        List<Integer> filterIds = hasCategory ? categoryIds : List.of(-1);
        Integer finalTagId = (tagId != null) ? tagId : -1;
        Page<Product> productPage = productRepository.searchProducts(searchKey, filterIds, hasCategory, true, startDate, endDate, isNew, isFaulty, inStockOnly, tagId != null ? tagId : -1, brand, minPrice, maxPrice,pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toCollection(ArrayList::new));

        if ("price_asc".equals(sortBy)) {
            content.sort(Comparator.comparing(p -> p.getMinPrice() != null ? p.getMinPrice() : BigDecimal.valueOf(Double.MAX_VALUE)));
        } else if ("price_desc".equals(sortBy)) {
            content.sort(Comparator.comparing((ProductResponse p) -> p.getMinPrice() != null ? p.getMinPrice() : BigDecimal.ZERO).reversed());
        }

        return new PageImpl<>(content, pageable, productPage.getTotalElements());
    }

    @Transactional
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        return mapToResponse(product);
    }

    @SensitiveOperation(
            action = ActionType.DELETE_OPERATION,
            entity = "PRODUCT",
            description = "Soft delete product",
            severity = SeverityLevel.HIGH
    )
    @Audit(
            module = AuditModule.PRODUCT,
            action = AuditAction.DELETE,
            targetType = TargetType.PRODUCT
    )
    @Transactional
    public void softDeleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        product.setIsVisible(false);
        productRepository.save(product);
    }

    public Page<ProductResponse> getTrashProducts(int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by("updatedAt").descending());
        return productRepository.findByIsVisibleFalse(pageable).map(this::mapToResponse);
    }

    @Transactional
    public void restoreProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        product.setIsVisible(true);
        productRepository.save(product);
    }

    @Transactional
    public void hardDeleteProduct(Integer id) {
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse dto = new ProductResponse();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setDescription(product.getDescription());
        dto.setIsVisible(product.getIsVisible());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setAttributes(product.getAttributesJson());
        dto.setIsNew(product.getIsNew());
        dto.setIsFaulty(product.getIsFaulty());
        dto.setBrand(product.getBrand());

        List<Image> images = imageRepository.findByProductId(product.getId());
        if (images != null) {
            dto.setImages(images.stream().map(img -> {
                ImageResponse imgDto = new ImageResponse();
                imgDto.setId(img.getId());
                imgDto.setUrl(img.getUrl());
                imgDto.setIsPrimary(img.getIsPrimary());
                return imgDto;
            }).toList());
            images.stream().filter(img -> Boolean.TRUE.equals(img.getIsPrimary())).findFirst()
                    .ifPresent(img -> dto.setImageUrl(img.getUrl()));
        }

        if (dto.getImageUrl() == null) {
            imageRepository.findFirstByProductIdAndIsPrimaryTrue(product.getId())
                    .ifPresent(img -> dto.setImageUrl(img.getUrl()));
        }

        LocalDateTime now = LocalDateTime.now();
        productVariantRepository.findByProduct_Id(product.getId()).stream()
                .filter(v -> v.getPrice() != null)
                .min((v1, v2) -> {
                    BigDecimal final1 = calculateFinalPrice(v1, now);
                    BigDecimal final2 = calculateFinalPrice(v2, now);
                    return final1.compareTo(final2);
                })
                .ifPresent(v -> {
                    dto.setMinPrice(v.getPrice());
                    dto.setCurrencyCode(v.getCurrencyCode());
                    dto.setVariantId(v.getId());

                    BigDecimal finalPrice = calculateFinalPrice(v, now);
                    if (finalPrice.compareTo(v.getPrice()) < 0) {
                        dto.setPromotionPrice(finalPrice);
                        Promotion promo = promotionService.findBestPromotionForVariant(v, now);
                        if (promo != null) {
                            dto.setPromotionName(promo.getName());
                            dto.setPromotionCode(promo.getCode());
                        }
                    }
                });

        productCategoryRepository
                .findFirstById_ProductIdAndIsPrimaryTrue(product.getId())
                .ifPresent(pc -> dto.setCategoryId(pc.getCategory().getId()));
        int totalStock = productVariantRepository.findByProduct_Id(product.getId()).stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .mapToInt(v -> {
                    int actualStock = productSerialRepository.countByVariantIdAndStatus(v.getId(), "IN_STOCK");
                    if (v.getStockQuantity() == null || v.getStockQuantity() != actualStock) {
                        v.setStockQuantity(actualStock);
                        productVariantRepository.save(v);
                    }
                    return actualStock;
                })
                .sum();
        dto.setTotalStock(totalStock);
        dto.setInStock(totalStock > 0);

        List<String> tagNames = productTagRepository.findByProduct_Id(product.getId())
                .stream()
                .map(pt -> pt.getTag().getName())
                .toList();
        dto.setTags(tagNames);

        return dto;
    }

    private BigDecimal calculateFinalPrice(ProductVariant variant, LocalDateTime at) {
        BigDecimal basePrice = variant.getPrice();
        if (basePrice == null) return BigDecimal.ZERO;

        Promotion best = promotionService.findBestPromotionForVariant(variant, at);
        if (best != null) {
            return promotionService.computeEffectiveUnitPrice(basePrice, best);
        }
        return basePrice;
    }

    private String saveFileToDisk(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    @Transactional
    public void setPrimaryImage(Integer productId, Long imageId) {
        List<Image> images = imageRepository.findByProductId(productId);
        boolean exists = images.stream().anyMatch(img -> img.getId().equals(imageId));
        if (!exists) throw new RuntimeException("Hình ảnh không thuộc sản phẩm này!");

        for (Image img : images) {
            if (img.getId().equals(imageId)) {
                img.setIsPrimary(true);
                img.setSortOrder(0);
            } else {
                img.setIsPrimary(false);
                img.setSortOrder(1);
            }
        }
        imageRepository.saveAll(images);
    }

    @SensitiveOperation(
            action = ActionType.UPDATE_OPERATION,
            entity = "PRODUCT",
            description = "Batch update products",
            severity = SeverityLevel.MEDIUM
    )
    @Audit(
            module = AuditModule.PRODUCT,
            action = AuditAction.UPDATE,
            targetType = TargetType.PRODUCT
    )
    @Transactional
    public void batchUpdateProducts(List<Integer> ids, Boolean isVisible, Boolean isNew, Boolean isFaulty, List<Integer> tagIds) {
        List<Product> products = productRepository.findAllById(ids);

        for (Product product : products) {
            if (isVisible != null) product.setIsVisible(isVisible);
            if (isNew != null) product.setIsNew(isNew);
            if (isFaulty != null) product.setIsFaulty(isFaulty);

            product.setUpdatedAt(LocalDateTime.now());

            if (tagIds != null) {
                productTagRepository.deleteByProduct_Id(product.getId());
                productTagRepository.flush();

                for (Integer tagId : tagIds) {
                    ProductTag pt = new ProductTag();
                    ProductTagId ptId = new ProductTagId();
                    ptId.setProductId(product.getId());
                    ptId.setTagId(tagId);

                    pt.setId(ptId);
                    pt.setProduct(product);
                    pt.setTag(tagRepository.getReferenceById(tagId));
                    pt.setCreatedAt(Instant.now());

                    productTagRepository.save(pt);
                }
            }
        }
        productRepository.saveAll(products);
    }
}