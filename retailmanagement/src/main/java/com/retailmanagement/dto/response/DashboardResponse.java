package com.retailmanagement.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardResponse  {

    private long totalLogs;
    private long securityViolations;
    private long suspiciousActivities;
    private long sensitiveOperations;
    private Map<String, Long> severityStats;
    private long todayLogs;
    private long thisWeekLogs;

    private long auditActionsToday;
    private long auditActionsThisWeek;
    private Map<String, Long> auditStatsByType;
}
