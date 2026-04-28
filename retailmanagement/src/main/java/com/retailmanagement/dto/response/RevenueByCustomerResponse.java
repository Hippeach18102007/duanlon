package com.retailmanagement.dto.response;

import java.math.BigDecimal;

public record RevenueByCustomerResponse(
        Integer customerId,
        String customerName,
        BigDecimal totalRevenue
) {}

