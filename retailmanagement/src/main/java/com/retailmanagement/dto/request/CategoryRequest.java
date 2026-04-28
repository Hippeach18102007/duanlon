package com.retailmanagement.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CategoryRequest {
    private String name;
    private String description;
    private Integer parentId;
    private MultipartFile imageFile;
}
