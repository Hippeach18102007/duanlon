package com.retailmanagement.dto.request;

import lombok.Data;

@Data
public class ProcessReturnRequest {
    private String reason;
    private String note;
    private String refundType;
}