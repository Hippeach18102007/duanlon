package com.retailmanagement.security.log;

import com.retailmanagement.repository.UserRepository;
import com.retailmanagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityLogService {

    private final SecurityLogRepository securityLogRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /** anti spam alert cache */
    private final Map<String, Long> lastAlertMap = new ConcurrentHashMap<>();

    // =====================================================
    // LOG MAIN
    // =====================================================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            String username,
            ActionType actionType,
            String targetEntity,
            String targetId,
            String ip,
            SeverityLevel severity,
            String status,
            String description
    ) {

        try {

            SecurityLog logEntity = SecurityLog.builder()
                    .username(username)
                    .actionType(actionType)
                    .targetEntity(targetEntity)
                    .targetId(targetId)
                    .ipAddress(ip)
                    .severity(severity)
                    .status(status)
                    .description(description)
                    .build();

            securityLogRepository.save(logEntity);

            // ===== ALERT LOGIC =====
            if (severity == SeverityLevel.CRITICAL) {
                handleCriticalAlert(logEntity);
            }

            if (severity == SeverityLevel.HIGH) {
                handleHighThreshold(logEntity);
            }

        } catch (Exception ex) {
            log.error("SecurityLog failed", ex); // ❗ không nuốt lỗi
        }
    }

    // =====================================================
    // ADMIN EMAIL
    // =====================================================
    private List<String> getAdminEmails() {
        return userRepository.findAdminEmails()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // =====================================================
    // CRITICAL ALERT
    // =====================================================
    private void handleCriticalAlert(SecurityLog log) {

        List<String> admins = getAdminEmails();
        if (admins.isEmpty()) return;

        emailService.sendSecurityAlertToAdmins(
                admins,
                "CRITICAL SECURITY ALERT - " + log.getActionType(),
                buildSecurityHtml(log)
        );
    }

    // =====================================================
    // HIGH ALERT THRESHOLD
    // =====================================================
    private void handleHighThreshold(SecurityLog log) {

        // Truyền chuỗi .name() và bỏ tham số Instant để khớp với cấu trúc Repository mới
        long count = securityLogRepository.countRecentHigh(log.getActionType().name());

        if (count < 10) return;

        String key =
                log.getActionType() + "_" +
                        Optional.ofNullable(log.getIpAddress()).orElse("unknown");

        if (!shouldSendHighAlert(key)) return;

        List<String> admins = getAdminEmails();
        if (admins.isEmpty()) return;

        emailService.sendSecurityAlertToAdmins(
                admins,
                "HIGH ALERT - " + log.getActionType(),
                buildHighAlertHtml(log, count)
        );
    }

    // =====================================================
    // ANTI SPAM ALERT (5 phút)
    // =====================================================
    private boolean shouldSendHighAlert(String key) {

        long now = System.currentTimeMillis();

        Long lastSent = lastAlertMap.get(key);

        if (lastSent != null && now - lastSent < 300_000) {
            return false;
        }

        lastAlertMap.put(key, now);
        return true;
    }

    // =====================================================
    // EMAIL TEMPLATE
    // =====================================================
    private String buildSecurityHtml(SecurityLog log) {

        String time = formatTime(log.getCreatedAt());

        return """
            <div style="font-family:Arial;padding:20px">
                <h2 style="color:red">🚨 SECURITY ALERT</h2>

                <p><b>User:</b> %s</p>
                <p><b>Action:</b> %s</p>
                <p><b>Target:</b> %s (%s)</p>
                <p><b>IP:</b> %s</p>
                <p><b>Status:</b> %s</p>
                <p><b>Description:</b> %s</p>
                <p><b>Time:</b> %s</p>
            </div>
        """.formatted(
                log.getUsername(),
                log.getActionType(),
                log.getTargetEntity(),
                log.getTargetId(),
                log.getIpAddress(),
                log.getStatus(),
                log.getDescription(),
                time
        );
    }

    private String buildHighAlertHtml(SecurityLog log, long count) {

        String time = formatTime(log.getCreatedAt());

        return """
            <div style="font-family:Arial;padding:20px">
                <h2 style="color:orange">⚠️ HIGH ALERT (Threshold Reached)</h2>

                <p><b>User:</b> %s</p>
                <p><b>Action:</b> %s</p>
                <p><b>Số lần:</b> %d lần trong 5 phút</p>
                <p><b>IP:</b> %s</p>
                <p><b>Time:</b> %s</p>

                <hr/>
                <p style="font-size:12px;color:gray">
                    Có dấu hiệu tấn công hoặc hành vi bất thường
                </p>
            </div>
        """.formatted(
                log.getUsername(),
                log.getActionType(),
                count,
                log.getIpAddress(),
                time
        );
    }

    private String formatTime(Instant instant) {
        if (instant == null) return "N/A";

        return instant.atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    // =====================================================
    // FILTER LOGS
    // =====================================================
    public Page<SecurityLog> filterLogs(
            SecurityLogFilterRequest request,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        size = Math.min(size, 100);

        List<String> whitelist = List.of(
                "createdAt",
                "username",
                "actionType",
                "severity",
                "status",
                "ipAddress"
        );

        if (!whitelist.contains(sortBy)) {
            sortBy = "createdAt";
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (Exception e) {
            direction = Sort.Direction.DESC;
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size,
                Sort.by(direction, sortBy)
        );

        Specification<SecurityLog> spec =
                Specification.where(SecurityLogSpecification.filter(request));

        return securityLogRepository.findAll(spec, pageable);
    }
}