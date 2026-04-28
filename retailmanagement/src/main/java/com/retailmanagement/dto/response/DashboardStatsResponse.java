package com.retailmanagement.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class DashboardStatsResponse {
    private long totalActiveProducts;
    private long totalSerialsInStock;
    private long totalSerialsFaulty;
    private long outOfStockVariants;
    private List<BestSellingProductDTO> topSellingProducts;
}