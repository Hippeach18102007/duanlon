package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String reply;
    private String intent;     // LOYALTY_QUERY | ORDER_SUPPORT | PRODUCT_ADVICE | GENERAL
    private boolean escalate;  // true = nên chuyển sang nhân viên thật
}