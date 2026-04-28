package com.retailmanagement.service;

import com.retailmanagement.dto.request.ChangePasswordRequest;
import com.retailmanagement.dto.request.LoginRequest;
import com.retailmanagement.dto.request.RegisterRequest;
import com.retailmanagement.dto.response.AuthResponse;
import com.retailmanagement.dto.response.UserResponse;
import com.retailmanagement.entity.Customer;
import com.retailmanagement.entity.CustomerType;
import com.retailmanagement.entity.Role;
import com.retailmanagement.entity.User;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.repository.RoleRepository;
import com.retailmanagement.repository.UserRepository;
import com.retailmanagement.security.service.JwtService;
import com.retailmanagement.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomRes customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;
    private final UserLoginLogService userLoginLogService;

    // ========================= REGISTER =========================
    @Transactional
    public AuthResponse register(RegisterRequest req) {

        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        Role role = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        // 1️⃣ Create USER
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // 2️⃣ Create or Update CUSTOMER
        Optional<Customer> existingCus = customerRepository.findByEmail(req.getEmail());

        if (existingCus.isEmpty()) {

            Customer customer = Customer.builder()
                    .name(req.getUsername())
                    .email(req.getEmail())
                    .dateOfBirth(req.getDateOfBirth())   // ← add
                    .address(req.getAddress())
                    .phone(req.getPhone())
                    .userId(user.getId())
                    .customerType(CustomerType.REGULAR)
                    .loyaltyPoints(0)
                    .totalSpent(BigDecimal.ZERO)
                    .isActive(true)
                    .lastLoginAt(LocalDateTime.now())
                    .build();

            customerRepository.save(customer);

        } else {

            Customer customer = existingCus.get();
            customer.setIsActive(true);
            customer.setLastLoginAt(LocalDateTime.now());
            customer.setUserId(user.getId());
            if (customer.getName() == null || customer.getName().isBlank()) {
                customer.setName(req.getUsername());
            }
            if (customer.getPhone() == null || customer.getPhone().isBlank()) {
                customer.setPhone(req.getPhone());  // ← thêm
            }

            customerRepository.save(customer);
        }

        String token = jwtService.generateToken(user);
        UserResponse userRes = modelMapper.map(user, UserResponse.class);

        if (user.getRole() != null) {
            userRes.setRole(user.getRole().getName());
        }

        return AuthResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpirationMillis())
                .user(userRes)
                .build();
    }

    // ========================= LOGIN =========================
    public AuthResponse login(LoginRequest req, HttpServletRequest request) {

        User user = userRepository
                .findByUsernameOrEmail(req.getIdentifier(), req.getIdentifier())
                .orElseThrow(() -> {
                    userLoginLogService.logLoginFail(req.getIdentifier(), request);
                    return new RuntimeException("Sai tài khoản hoặc mật khẩu");
                });

        if (Boolean.FALSE.equals(user.getIsActive())) {
            userLoginLogService.logLoginFail(user.getUsername(), request);
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            req.getPassword()
                    )
            );

        } catch (BadCredentialsException ex) {

            userLoginLogService.logLoginFail(user.getUsername(), request);
            throw new RuntimeException("Sai tài khoản hoặc mật khẩu");
        }

        // ✅ LOGIN SUCCESS
        userLoginLogService.logLoginSuccess(
                user.getId(),
                user.getUsername(),
                request
        );

        // ✅ Update last_login_at cho customer
        customerRepository.findByEmail(user.getEmail())
                .ifPresent(customer -> {
                    customer.setLastLoginAt(LocalDateTime.now());
                    customerRepository.save(customer);
                });

        String token = jwtService.generateToken(user);
        UserResponse userRes = modelMapper.map(user, UserResponse.class);

        if (user.getRole() != null) {
            userRes.setRole(user.getRole().getName());
        }

        return AuthResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpirationMillis())
                .user(userRes)
                .build();
    }

    // ========================= LOGOUT =========================
    public void logout() {

        Integer userId = SecurityUtil.getCurrentUserId();

        if (userId != null) {
            userLoginLogService.logLogout(userId);
        }
    }
    // ========================= CHANGE PASSWORD =========================
// Thêm method này vào AuthService.java

    @Transactional
    public void changePassword(ChangePasswordRequest req) {

        // 1️⃣ Lấy user hiện tại từ SecurityContext
        Integer userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("Bạn chưa đăng nhập");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        // 2️⃣ Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        // 3️⃣ Kiểm tra mật khẩu mới ≠ mật khẩu cũ
        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        // 4️⃣ Cập nhật
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }
}