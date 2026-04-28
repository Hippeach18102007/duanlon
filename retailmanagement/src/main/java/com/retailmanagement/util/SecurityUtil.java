package com.retailmanagement.util;

import com.retailmanagement.security.service.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    // Lấy user khi thao tác (ví dụ: khi create, update, delete)
    public static Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem người dùng login chưa
        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return  null;
        }

        // Lấy danh tính user
        Object principal = authentication.getPrincipal();

        // Kiểm tra kiểu dữ liệu principal có phải là CustomUserDetails
        // và tự động ép kiểu dữ liệu sang CustomUserDetails
        if(principal instanceof CustomUserDetails userDetails) {
            return  userDetails.getUserId();
        }

        return null;
    }

}
