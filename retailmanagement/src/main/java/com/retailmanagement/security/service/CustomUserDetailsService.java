package com.retailmanagement.security.service;

import com.retailmanagement.entity.Customer;
import com.retailmanagement.entity.User;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // ===== CODE CŨ =====
    private final UserRepository userRepository;

    // ===== THÊM (KHÔNG ĐỤNG CODE CŨ) =====
    private final CustomRes customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // ===== CODE CŨ =====
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User không tồn tại"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new UsernameNotFoundException("Tài khoản đã bị khóa");
        }

        // ===== GIỮ NGUYÊN CÁCH TẠO PRINCIPAL =====
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // ===== THÊM DUY NHẤT: RESOLVE customerId =====
        Customer customer = customerRepository.findByEmail(user.getEmail())
                .orElse(null);

        if (customer != null) {
            userDetails.setCustomerId(customer.getId());
        }
        // ===== HẾT PHẦN THÊM =====

        return userDetails;
    }
}
