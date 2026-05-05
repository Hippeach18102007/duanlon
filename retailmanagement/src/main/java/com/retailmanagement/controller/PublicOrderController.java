package com.retailmanagement.controller;

import com.retailmanagement.dto.response.ApiResponse;
import com.retailmanagement.dto.response.DeliveryInfoResponse;
import com.retailmanagement.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/orders")
@RequiredArgsConstructor
public class PublicOrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}/delivery-info")
    public ApiResponse<DeliveryInfoResponse> getDeliveryInfo(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getDeliveryInfo(orderId));
    }
}

