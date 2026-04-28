package com.retailmanagement.service;

import com.retailmanagement.repository.OrderRepository;
import com.retailmanagement.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final ReturnRepository returnRepository;

    public List<Map<String, Object>> revenueByChannel() {
        return orderRepository.revenueByChannel()
                .stream()
                .map(r -> Map.of(
                        "channel", r[0],
                        "revenue", r[1]
                ))
                .toList();
    }

    public List<Map<String, Object>> revenueByDate() {
        return orderRepository.revenueByDate()
                .stream()
                .map(r -> Map.of(
                        "date",    r[0],
                        "revenue", r[1]
                ))
                .toList();
    }

    public List<Map<String, Object>> ordersByStaff() {
        return orderRepository.ordersByStaff()
                .stream()
                .map(r -> Map.of(
                        "staff",  r[0],
                        "orders", r[1]
                ))
                .toList();
    }

    /**
     * Tổng hợp doanh thu theo trạng thái đơn hàng.
     * Mỗi phần tử: { status, orderCount, totalRevenue }
     */
    public List<Map<String, Object>> revenueByStatus() {
        return orderRepository.revenueByStatus()
                .stream()
                .map(r -> Map.of(
                        "status",       r[0],
                        "orderCount",   r[1],
                        "totalRevenue", r[2] != null ? r[2] : 0
                ))
                .toList();
    }

    /**
     * Thống kê trả hàng theo sản phẩm (loại trừ REJECTED).
     * Mỗi phần tử: { productName, sku, variantName, returnCount, totalQty, totalRefund }
     */
    public List<Map<String, Object>> returnStatsByProduct() {
        return returnRepository.returnStatsByProduct()
                .stream()
                .map(r -> Map.of(
                        "productName",  r[0],
                        "sku",          r[1] != null ? r[1] : "",
                        "variantName",  r[2] != null ? r[2] : "",
                        "returnCount",  r[3],
                        "totalQty",     r[4],
                        "totalRefund",  r[5] != null ? r[5] : 0
                ))
                .toList();
    }

    /** Doanh thu theo tuần. Mỗi phần tử: { weekStart, revenue } */
    public List<Map<String, Object>> revenueByWeek() {
        return orderRepository.revenueByWeek()
                .stream()
                .map(r -> Map.of(
                        "weekStart", r[0],
                        "revenue",   r[1] != null ? r[1] : 0
                ))
                .toList();
    }

    /** Doanh thu theo tháng. Mỗi phần tử: { monthStart, revenue } */
    public List<Map<String, Object>> revenueByMonth() {
        return orderRepository.revenueByMonth()
                .stream()
                .map(r -> Map.of(
                        "monthStart", r[0],
                        "revenue",    r[1] != null ? r[1] : 0
                ))
                .toList();
    }

    /**
     * Top sản phẩm bán chạy.
     * Mỗi phần tử: { productName, sku, variantName, totalQty, totalRevenue }
     */
    public List<Map<String, Object>> topSellingProducts() {
        return orderRepository.topSellingProducts()
                .stream()
                .map(r -> Map.of(
                        "productName",  r[0],
                        "sku",          r[1] != null ? r[1] : "",
                        "variantName",  r[2] != null ? r[2] : "",
                        "totalQty",     r[3],
                        "totalRevenue", r[4] != null ? r[4] : 0
                ))
                .toList();
    }
}