package com.retailmanagement.service;

import com.retailmanagement.audit.AuditLogRepository;
import com.retailmanagement.audit.LogSummaryService;
import com.retailmanagement.dto.response.DashboardResponse;
import com.retailmanagement.security.log.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SecurityLogRepository securityLogRepository;
    private final LogSummaryService logSummaryService;
    private final AuditLogRepository auditLogRepository;

    public DashboardResponse getDashboard() {

        var summary = logSummaryService.getSummary("month");

        Instant now = Instant.now();

        Instant startToday =
                LocalDate.now()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant();

        Instant startWeek =
                LocalDate.now()
                        .minusDays(7)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant();

        long todayLogs =
                securityLogRepository.countByCreatedAtBetween(startToday, now);
        long weekLogs =
                securityLogRepository.countByCreatedAtBetween(startWeek, now);

        long auditToday = auditLogRepository.countByCreatedAtBetween(startToday, now);
        long auditWeek = auditLogRepository.countByCreatedAtBetween(startWeek, now);
        List<Object[]> grouped = auditLogRepository.countAuditActionsGrouped();
        Map<String, Long> auditStats = grouped.stream()
                .collect(Collectors.toMap(
                        r -> (String) r[0],
                        r -> (Long) r[1]
                ));

        return DashboardResponse.builder()
                .totalLogs(summary.getTotalLogs())
                .securityViolations(summary.getSecurityViolations())
                .suspiciousActivities(summary.getSuspiciousActivities())
                .sensitiveOperations(summary.getSensitiveOperations())
                .severityStats(summary.getSeverityStats())
                .todayLogs(todayLogs)
                .thisWeekLogs(weekLogs)
                .auditActionsToday(auditToday)
                .auditActionsThisWeek(auditWeek)
                .auditStatsByType(auditStats)
                .build();
    }
}
