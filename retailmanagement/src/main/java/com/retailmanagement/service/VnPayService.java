package com.retailmanagement.service;

import com.retailmanagement.config.VnPayConfig;
import com.retailmanagement.entity.Order;
import com.retailmanagement.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VnPayService {
    private final OrderRepository orderRepository;

    public String createPaymentUrl(Long orderId, HttpServletRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        String safeOrderNumber = order.getOrderNumber();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", VnPayConfig.vnp_TmnCode);

        // VNPAY yêu cầu số tiền nhân với 100
        vnp_Params.put("vnp_Amount", String.valueOf(order.getTotalAmount().longValue() * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", safeOrderNumber);
        vnp_Params.put("vnp_OrderInfo", "TT_" + safeOrderNumber);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // TẤT CẢ LOGIC SẮP XẾP, REPLACE "%20", VÀ ÉP IN HOA ĐÃ NẰM HẾT TRONG NÀY
        return VnPayConfig.buildPaymentUrl(vnp_Params);
    }
}