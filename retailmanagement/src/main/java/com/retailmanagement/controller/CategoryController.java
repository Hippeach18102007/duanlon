package com.retailmanagement.controller;

import com.retailmanagement.dto.request.CategoryRequest;
import com.retailmanagement.dto.response.ApiResponse;
import com.retailmanagement.dto.response.CategoryResponse;
import com.retailmanagement.entity.Category;
import com.retailmanagement.repository.CategoryRepository;
import com.retailmanagement.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục thành công", saved));
    }

    // ✅ Added: category list for product filter UI
    // activeOnly=true by default to avoid showing disabled categories
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<Category> categories = activeOnly
                ? categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()
                : categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();

        List<CategoryResponse> result = categories.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục thành công", result));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .displayOrder(c.getDisplayOrder())
                .isActive(c.getIsActive())
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> createCategoryWithImage(@ModelAttribute CategoryRequest request) throws IOException {
        categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục (có ảnh) thành công", null));
    }

    // 2. Cập nhật (Sửa tên, mô tả, ảnh, cha con...)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> updateCategory(
            @PathVariable Integer id,
            @ModelAttribute CategoryRequest request
    ) throws IOException {
        categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", null));
    }

    // 3. Xóa mềm (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Integer id) {
        categoryService.softDeleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Đã ẩn danh mục thành công", null));
    }

    // 4. Lấy danh sách có Phân trang
    @GetMapping("/paging")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> listCategoriesPaged(Pageable pageable) {
        Page<CategoryResponse> page = categoryService.getAllCategories(pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phân trang thành công", page));
    }
}
