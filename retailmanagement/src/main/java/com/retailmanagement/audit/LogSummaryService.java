package com.retailmanagement.audit;

import com.retailmanagement.dto.response.LogSummaryResponse;
import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogSummaryService {

    private final SecurityLogRepository securityLogRepository;

    public LogSummaryResponse getSummary(String type) {

        Instant end = Instant.now();
        Instant start;

        LocalDate today = LocalDate.now();

        if ("week".equalsIgnoreCase(type)) {
            start = today.minusDays(7)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
        } else if ("month".equalsIgnoreCase(type)) {
            start = today.minusDays(30)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
        } else {
            throw new RuntimeException("Type must be week or month");
        }

        long totalLogs = securityLogRepository.countByCreatedAtBetween(start, end);

        long securityViolations =
                securityLogRepository.countByActionTypeInAndCreatedAtBetween(
                        List.of(
                                ActionType.UNAUTHORIZED_ACCESS,
                                ActionType.FORBIDDEN_ACCESS,
                                ActionType.INTRUSION_ATTEMPT,
                                ActionType.UNAUTHORIZED_OPERATION
                        ),
                        start, end
                );

        long suspiciousActivities =
                securityLogRepository.countByActionTypeInAndCreatedAtBetween(
                        List.of(
                                ActionType.ABNORMAL_ACCESS,
                                ActionType.ABNORMAL_OPERATION
                        ),
                        start, end
                );

        long sensitiveOperations =
                securityLogRepository.countByActionTypeInAndCreatedAtBetween(
                        List.of(
                                ActionType.ROLE_CHANGE,
                                ActionType.CHANGE_PASSWORD,
                                ActionType.DELETE_OPERATION,
                                ActionType.UPDATE_OPERATION,
                                ActionType.CREATE_OPERATION,
                                ActionType.SENSITIVE_OPERATION
                        ),
                        start, end
                );

        // severity stats
        Map<String, Long> severityMap = new HashMap<>();

        for (Object[] row : securityLogRepository.countBySeverity(start, end)) {
            severityMap.put(row[0].toString(), (Long) row[1]);
        }

        return LogSummaryResponse.builder()
                .totalLogs(totalLogs)
                .securityViolations(securityViolations)
                .suspiciousActivities(suspiciousActivities)
                .sensitiveOperations(sensitiveOperations)
                .severityStats(severityMap)
                .build();
    }
}
