package com.retailmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuditSummaryResponse {

    private long totalLogs;
    private long totalActiveUsers;
    private long totalErrors;
    private long todayLogs;

    private String topModule;
    private Integer topUserId;
}
