package com.retailmanagement.service;

import com.retailmanagement.entity.User;
import com.retailmanagement.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // email -> {otp, expiresAt}
    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    static class OtpEntry {
        String otp;
        Instant expiresAt;
    }

    public void sendOtp(String email) {
        // Tìm theo username (email) hoặc theo email field
        boolean exists = userRepository.existsByUsername(email)
                || userRepository.existsByEmail(email);

        if (!exists) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }

        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        otpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(300)));
        emailService.sendOtpEmail(email, otp);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null) throw new RuntimeException("OTP không tồn tại");
        if (Instant.now().isAfter(entry.getExpiresAt())) {
            otpStore.remove(email);
            throw new RuntimeException("OTP đã hết hạn");
        }
        if (!entry.getOtp().equals(otp)) throw new RuntimeException("OTP không đúng");

        // Tìm theo username trước, fallback sang email field
        User user = userRepository.findByUsername(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpStore.remove(email);
    }
}
