package com.retailmanagement.audit;

import com.retailmanagement.dto.response.AuditLogResponse;
import com.retailmanagement.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public void save(
            Integer userId,
            AuditModule module,
            AuditAction action,
            TargetType target,
            Long targetId,
            String detailsJson,
            String ipAddress
    ) {
        AuditLog log = new AuditLog();

        // Dùng entityMananger.getReference để tạo proxy User để gán userId
        // (proxy đại diện cho User thật)
       if(userId != null) {
            User userRef = entityManager.getReference(User.class,userId);
            log.setUser(userRef);
       }
        log.setModule(module.name());
        log.setAction(action.name());
        log.setTargetType(target.name());
        log.setTargetId(targetId);
        log.setDetailsJson(detailsJson);
        log.setIpAddress(ipAddress);

        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> findAll(){
        return auditLogRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsByDate(LocalDate date) {
        ZoneId zone = ZoneId.of("UTC");

        Instant from = date.atStartOfDay(zone).toInstant();

        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();

        return auditLogRepository.findByCreatedAtBetween(from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AuditLog> getByDateRange(LocalDate from, LocalDate to){
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

        return auditLogRepository.findByCreatedAtBetween(fromInstant,toInstant);
    }

    public List<AuditLogResponse> getAuditLogsByActionChange_Role(){
        return auditLogRepository.findByActionOrderByCreatedAtDesc(AuditAction.CHANGE_ROLE.name())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsByUser_Id(Integer id){
        return auditLogRepository.findByUserId(id)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsByModuleCustomer(){
        return auditLogRepository.findByModuleOrderByCreatedAtDesc(AuditModule.CUSTOMER.name())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsByThisWeek(){
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDate today = LocalDate.now();

        Instant start = today
                .with(DayOfWeek.MONDAY)
                .atStartOfDay(zoneId)
                .toInstant();

        Instant end = today
                .with(DayOfWeek.SUNDAY)
                .atTime(LocalTime.MAX)
                .atZone(zoneId)
                .toInstant();

        return auditLogRepository.findLogsThisWeek(start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<AuditLogResponse> getAuditLogByModule(String module, int page, int size) {
        Pageable pageable = PageRequest.of(page,size, Sort.by(Sort.Direction.DESC,"createdAt"));

        return auditLogRepository.findByModule(module,pageable)
                .map(this::toResponse);
    }

    public Page<AuditLog> filterLogs(
            AuditLogFilterRequest request,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        size = Math.min(size, 100);

        List<String> allowedSortFields =
                List.of("createdAt", "module", "action", "ipAddress");

        if (!allowedSortFields.contains(sortBy)) {
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

        return auditLogRepository.findAll(
                AuditLogSpecification.filter(request),
                pageable
        );
    }

    public List<AuditLog> searchForExport(AuditLogFilterRequest req) {
        return auditLogRepository.findAll(
                AuditLogSpecification.filter(req)
        );
    }

    private AuditLogResponse toResponse(AuditLog auditLog){

        Integer userId = auditLog.getUser() != null
                ? auditLog.getUser().getId()
                : null;

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(userId)
                .module(auditLog.getModule())
                .action(auditLog.getAction())
                .targetType(auditLog.getTargetType())
                .targetId(auditLog.getTargetId())
                .detailsJson(auditLog.getDetailsJson())
                .ipAddress(auditLog.getIpAddress())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private String safe(Object value) {
        if (value == null) return "";

        String s = value.toString().replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
