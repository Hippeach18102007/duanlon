package com.retailmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class ProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;
    private String sku;
    private String description;
    private Boolean isVisible;

    private Integer categoryId;

    private MultipartFile imageFile;

    private List<Integer> categoryIds;

    private String attributes;

    private List<MultipartFile> galleryImages;

    private List<Long> idsToDelete;

    private List<Integer> tagIds;

    private Boolean isNew;
    private Boolean isFaulty;

    private String brand;
}