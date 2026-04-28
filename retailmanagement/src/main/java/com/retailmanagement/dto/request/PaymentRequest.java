package com.retailmanagement.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order ID không được để trống")
    private Long orderId;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String method; // CASH, CARD, BANK_TRANSFER, E_WALLET

    private String transactionRef; // Mã giao dịch (tùy chọn)

    private String note; // Ghi chú (tùy chọn)
}