package com.retailmanagement.service;

import com.retailmanagement.dto.request.CategoryRequest;
import com.retailmanagement.dto.response.CategoryResponse;
import com.retailmanagement.entity.Category;
import com.retailmanagement.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final String UPLOAD_DIR = "uploads/categories/";

    // 1. Lấy danh sách (Có phân trang)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findByIsActiveTrue(pageable).map(this::mapToResponse);
    }

    // 2. Tạo mới
    @Transactional
    public void createCategory(CategoryRequest request) throws IOException {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIsActive(true);
        category.setDisplayOrder(0);
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId()).orElse(null);
            category.setParent(parent);
        }
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            String url = cloudinaryService.uploadFile(request.getImageFile()); // ← đổi
            category.setImageUrl(url);
        }

        categoryRepository.save(category);
    }

    // 3. Cập nhật
    @Transactional
    public void updateCategory(Integer id, CategoryRequest request) throws IOException {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setUpdatedAt(Instant.now());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId()).orElse(null);
            category.setParent(parent);
        }

        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            String url = cloudinaryService.uploadFile(request.getImageFile());
            category.setImageUrl(url);
        }

        categoryRepository.save(category);
    }

    // 4. Xóa mềm (Soft Delete)
    @Transactional
    public void softDeleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setIsActive(false); // Chỉ ẩn đi
        categoryRepository.save(category);
    }

    // --- Helper: Map Entity -> DTO (Dùng Builder của bạn) ---
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .build();
    }

    private String saveFileToDisk(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }
}