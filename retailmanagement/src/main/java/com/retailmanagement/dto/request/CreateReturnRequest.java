package com.retailmanagement.dto.request;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReturnRequest {
    private Long orderId;
    private Long orderItemId;
    private Integer quantity;
    private String reason;
    private BigDecimal refundAmount;
}