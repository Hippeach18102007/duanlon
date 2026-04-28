package com.retailmanagement.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    //Lấy ip của user
    public static String getClientIp(HttpServletRequest request) {
        // X-Forwared-For để lấy ip của client
        // Nếu ko có nó chỉ lấy ip trung gian
        String ip = request.getHeader("X-Forwarded-For");

        // Kiểm tra xem ip có bị null hay rỗng
        if (ip != null && !ip.isEmpty()) {
            // split để lấy ip thật của client
            // Các ip sau "," là các ip trung gian
            return ip.split(",")[0];
        }

        // Fallback nếu không có ip
        return request.getRemoteAddr();
    }
}
