package com.retailmanagement.controller;

import com.retailmanagement.audit.*;
import com.retailmanagement.dto.response.*;
import com.retailmanagement.service.DashboardService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/auth/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditReportService reportService;
    private final LogSummaryService logSummaryService;
    private final DashboardService dashboardService;

    // Get all
    @GetMapping("")
    public List<AuditLogResponse> getAllAuditLogs(){
        return auditLogService.findAll();
    }

    // filters
    @GetMapping("filter/day")
    public List<AuditLogResponse> filterByDay(@RequestParam("day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date){
        return auditLogService.getAuditLogsByDate(date);
    }

    @GetMapping("filter/action/change-role")
    public List<AuditLogResponse> filterByActionChange_Role(){
        return  auditLogService.getAuditLogsByActionChange_Role();
    }

    @GetMapping("filter/user_id")
    public List<AuditLogResponse> filterByUser_Id(@RequestParam("id")Integer id){
        return auditLogService.getAuditLogsByUser_Id(id);
    }

    @GetMapping("filter/module/customer")
    public List<AuditLogResponse> filterByModuleCustomer(){
        return auditLogService.getAuditLogsByModuleCustomer();
    }

    @GetMapping("filter/this-week")
    public List<AuditLogResponse> filerByThisWeek(){
        return auditLogService.getAuditLogsByThisWeek();
    }

    @GetMapping("filter/module")
    public Page<AuditLogResponse> filterByModule(
            @RequestParam String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
            ) {
        return auditLogService.getAuditLogByModule(module,page,size);
    }

    // Reports
    @GetMapping("report/module")
    public List<ModuleLogReportResponse> reportByModule(){
        return reportService.reportByModule();
    }

    @GetMapping("report/user")
    public List<UserActionReportResponse> reportByUser() {return reportService.getUserActionReport();}

    @GetMapping("report/summary")
    public AuditSummaryResponse getSummaryReport(){
        return reportService.getSummaryReport();
    }

    @GetMapping("log/summary")
    public LogSummaryResponse summary(@RequestParam String type) {
        return logSummaryService.getSummary(type);
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.getDashboard();
    }

    // Advanced filter
    @PostMapping("search")
    public Page<AuditLog> search(
            @RequestBody AuditLogFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        return  auditLogService.filterLogs(request,page,size,sortBy,sortDir);
    }

    // Exports
    @GetMapping("export")
    public void exportAuditLogs(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=audit_logs.csv");

        List<AuditLog> logs = auditLogService.getByDateRange(from,to);

        PrintWriter writer = response.getWriter();

        writer.println(
                "id,user_id,module,action,target_type,target_id,details_json,ip_address,created_at"
        );

        for(AuditLog log: logs) {
            writer.printf(
                    "%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    log.getId(),
                    safe(log.getUser() != null ? log.getUser().getId() : ""),
                    log.getModule(),
                    log.getAction(),
                    log.getTargetType(),
                    log.getTargetId(),
                    safe(log.getDetailsJson()),
                    log.getIpAddress(),
                    log.getCreatedAt()
            );
        }
        writer.flush();
    }

    @PostMapping("export/advanced")
    public void exportAdvanced(
            @RequestBody AuditLogFilterRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=audit_logs.csv");

        List<AuditLog> logs = auditLogService.searchForExport(request);

        try (PrintWriter writer = response.getWriter()) {

            // ⭐ Excel UTF8 fix (rất quan trọng)
            writer.write('\uFEFF');

            writer.println(
                    "id,user_id,module,action,target_type,target_id,details_json,ip_address,created_at"
            );

            for (AuditLog log : logs) {
                writer.printf(
                        "%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        log.getId(),
                        safe(log.getUser() != null ? log.getUser().getId() : ""),
                        safe(log.getModule()),
                        safe(log.getAction()),
                        safe(log.getTargetType()),
                        safe(log.getTargetId()),
                        safe(log.getDetailsJson()),
                        safe(log.getIpAddress()),
                        safe(log.getCreatedAt())
                );
            }
        }
    }

    private String safe(Object value) {
        if (value == null) return "";
        String s = value.toString().replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
