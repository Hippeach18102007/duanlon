package com.retailmanagement.dto.response;

import lombok.Data;

@Data
public class ImageResponse {
    private Long id;
    private String url;
    private Boolean isPrimary;
}
