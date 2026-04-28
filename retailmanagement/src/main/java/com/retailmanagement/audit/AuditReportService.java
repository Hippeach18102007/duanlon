package com.retailmanagement.audit;

import com.retailmanagement.dto.response.AuditSummaryResponse;
import com.retailmanagement.dto.response.ModuleLogReportResponse;
import com.retailmanagement.dto.response.UserActionReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditReportService {

    private final AuditLogRepository auditLogRepository;

    public List<ModuleLogReportResponse> reportByModule(){
        return auditLogRepository.reportByModule()
                .stream()
                .map(r -> new ModuleLogReportResponse(
                        (String) r[0],
                        ((Number) r[1]).longValue()
                ))
                .toList();
    }

    public List<UserActionReportResponse> getUserActionReport() {
        return auditLogRepository.countTotalActionByUser();
    }

    public AuditSummaryResponse getSummaryReport() {

        long totalLogs = auditLogRepository.count();
        long totalUsers = auditLogRepository.countDistinctUsers();
        long totalErrors = auditLogRepository.countErrors();

        // log hôm nay
        Instant todayStart = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant todayEnd = LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        long todayLogs = auditLogRepository.countByCreatedAtBetween(todayStart, todayEnd);

        // top module
        String topModule = null;
        List<Object[]> modules = auditLogRepository.findTopModule();
        if (!modules.isEmpty()) {
            topModule = (String) modules.get(0)[0];
        }

        // top user
        Integer topUser = null;
        List<Object[]> users = auditLogRepository.findTopUser();
        if (!users.isEmpty()) {
            topUser = (Integer) users.get(0)[0];
        }

        return AuditSummaryResponse.builder()
                .totalLogs(totalLogs)
                .totalActiveUsers(totalUsers)
                .totalErrors(totalErrors)
                .todayLogs(todayLogs)
                .topModule(topModule)
                .topUserId(topUser)
                .build();
    }
}
