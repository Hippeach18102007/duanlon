package com.retailmanagement.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class LogSummaryResponse {
    private long totalLogs;

    private long securityViolations;
    private long suspiciousActivities;
    private long sensitiveOperations;

    private Map<String, Long> severityStats;
}
