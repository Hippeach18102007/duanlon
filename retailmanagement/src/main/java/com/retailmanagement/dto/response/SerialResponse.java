package com.retailmanagement.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SerialResponse {
    private Integer id;
    private String serialNumber;
    private String status;
    private Integer variantId;
    private String variantName;
    private String productName;
    private String sku;
}