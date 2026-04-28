package com.retailmanagement.service;

import com.retailmanagement.audit.UserLoginSpecification;
import com.retailmanagement.dto.request.UserLoginFilterRequest;
import com.retailmanagement.dto.response.UserLoginResponse;
import com.retailmanagement.entity.User;
import com.retailmanagement.entity.UserLogin;
import com.retailmanagement.repository.UserLoginRepository;
import com.retailmanagement.repository.UserRepository;
import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SecurityLogService;
import com.retailmanagement.security.log.SeverityLevel;
import com.retailmanagement.util.IpUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserLoginLogService {

    private final UserLoginRepository userLoginRepository;
    private final EntityManager entityManager;
    private final SecurityLogService securityLogService;
    private final UserRepository userRepository;

    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAttemptTime = new ConcurrentHashMap<>();

    private static final long WINDOW_MILLIS = 5 * 60 * 1000;

    public void logLoginSuccess(
            Integer userId,
            String username,
            HttpServletRequest request
    ) {
        UserLogin log = new UserLogin();

        User userRef = entityManager.getReference(User.class, userId);

        log.setUser(userRef);
        log.setUsername(username);
        log.setSuccess(true);
        log.setIpAddress(IpUtil.getClientIp(request));
        log.setUserAgent(request.getHeader("User-agent"));

        userLoginRepository.save(log);

        String ip = IpUtil.getClientIp(request);
        String key = username + "|" + ip;
        failedAttempts.remove(key);
    }

    public void logLoginFail(
            String username,
            HttpServletRequest request
    ) {
        UserLogin log = new UserLogin();

        log.setUsername(username);
        log.setSuccess(false);
        log.setIpAddress(IpUtil.getClientIp(request));
        log.setUserAgent(request.getHeader("User-agent"));

        userLoginRepository.save(log);

        String ip = IpUtil.getClientIp(request);
        String key = username + "|" + ip;

        long now = System.currentTimeMillis();
        Long lastTime = lastAttemptTime.get(key);

        if (lastTime != null && now - lastTime > WINDOW_MILLIS) {
            failedAttempts.remove(key);
        }

        int attempts = failedAttempts.getOrDefault(key, 0) + 1;

        failedAttempts.put(key, attempts);
        lastAttemptTime.put(key, now);

        if (attempts == 5) {
            securityLogService.log(
                    username,
                    ActionType.ABNORMAL_OPERATION,
                    "LOGIN",
                    null,
                    IpUtil.getClientIp(request),
                    SeverityLevel.MEDIUM,
                    "SUSPICIOUS",
                    "Multiple failed login attempts: " + attempts
            );
        }

        if (attempts == 10) {
            securityLogService.log(
                    username,
                    ActionType.ABNORMAL_OPERATION,
                    "LOGIN",
                    null,
                    IpUtil.getClientIp(request),
                    SeverityLevel.HIGH,
                    "SUSPICIOUS",
                    "Multiple failed login attempts: " + attempts
            );
        }

        if (attempts == 20 || attempts == 50 || attempts == 100) {
            securityLogService.log(
                    username,
                    ActionType.INTRUSION_ATTEMPT,
                    "LOGIN",
                    null,
                    IpUtil.getClientIp(request),
                    SeverityLevel.CRITICAL,
                    "SUSPICIOUS",
                    "Multiple failed login attempts: " + attempts
            );
        }
    }

    public void logLogout (Integer userId) {
        userLoginRepository
                .findTopByUser_IdAndSuccessTrueAndUpdatedAtIsNullOrderByCreatedAtDesc(userId)
                .ifPresent(log -> {
                    log.setUpdatedAt(Instant.now());
                    userLoginRepository.save(log);
                });
    }

    public List<UserLoginResponse> getAllUserLogins() {
        return userLoginRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<UserLoginResponse> filterUserLogins(
            UserLoginFilterRequest request,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserLogin> result = userLoginRepository.findAll(
                UserLoginSpecification.filter(request),
                pageable
        );

        return result.map(this::toResponse);
    }

    public void exportCsv(UserLoginFilterRequest request,
                          PrintWriter writer) {

        writer.write('\uFEFF'); // fix Excel UTF8

        writer.println(
                "ID,User ID,Username,Status,IP Address,User Agent,Login Time"
        );

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

        int page = 0;
        int size = 1000;

        Page<UserLogin> result;

        do {
            result = userLoginRepository.findAll(
                    UserLoginSpecification.filter(request),
                    PageRequest.of(page, size)
            );

            for (UserLogin log : result.getContent()) {

                writer.printf(
                        "%s,%s,%s,%s,%s,%s,%s%n",
                        log.getId(),
                        safe(log.getUser() != null ? log.getUser().getId() : ""),
                        safeCsv(safe(log.getUser() != null ? log.getUser().getUsername() : "")),
                        log.getSuccess() ? "SUCCESS" : "FAILED",
                        safe(log.getIpAddress()),
                        escapeCsv(log.getUserAgent()),
                        formatter.format(log.getCreatedAt())
                );
            }

            page++;

        } while (result.hasNext());

        writer.flush();
    }

    public List<UserLogin> getByDateRange (LocalDate from, LocalDate to) {
        if(from == null && to == null){
            to = LocalDate.now();
            from = to.minusDays(7);
        }

        if(to == null){
            to = from;
        }

        if(from == null){
            from = to;
        }

        Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();

        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return userLoginRepository.findByCreatedAtBetween(fromInstant, toInstant);
    }

    private UserLoginResponse toResponse(UserLogin userLogin){
        Integer userId = userLogin.getUser() != null
                ? userLogin.getUser().getId()
                : null;

        return UserLoginResponse.builder()
                .id(userLogin.getId())
                .userId(userId)
                .username(userLogin.getUsername())
                .success(userLogin.getSuccess())
                .ipAddress(userLogin.getIpAddress())
                .userAgent(userLogin.getUserAgent())
                .createdAt(userLogin.getCreatedAt())
                .updatedAt(userLogin.getUpdatedAt())
                .build();
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String safeCsv(String value) {
        if (value == null) return "";

        if (value.startsWith("=") ||
                value.startsWith("+") ||
                value.startsWith("-") ||
                value.startsWith("@")) {
            return "'" + value;
        }

        return value;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";

        String result = value.replace("\"", "\"\"");

        if (result.contains(",") || result.contains("\"")) {
            result = "\"" + result + "\"";
        }

        return result;
    }
}
