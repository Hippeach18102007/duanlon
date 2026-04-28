package com.retailmanagement.security.log;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class SecurityLogSpecification {

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean hasList(List<?> list) {
        return list != null && !list.isEmpty();
    }

    public static Specification<SecurityLog> filter(SecurityLogFilterRequest request) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ===== USERNAME =====
            if (hasText(request.getUsername())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("username")),
                                "%" + request.getUsername().trim().toLowerCase() + "%"
                        )
                );
            }

            // ===== ACTION TYPE =====
            if (hasList(request.getActionTypes())) {
                predicates.add(root.get("actionType").in(request.getActionTypes()));
            }

            // ===== TARGET ENTITY =====
            if (hasList(request.getTargetEntities())) {
                predicates.add(root.get("targetEntity").in(request.getTargetEntities()));
            }

            // ===== SEVERITY =====
            if (hasList(request.getSeverities())) {
                predicates.add(root.get("severity").in(request.getSeverities()));
            }

            // ===== STATUS =====
            if (hasList(request.getStatuses())) {
                predicates.add(root.get("status").in(request.getStatuses()));
            }

            // ===== IP ADDRESS =====
            if (hasText(request.getIpAddress())) {
                predicates.add(
                        cb.like(
                                root.get("ipAddress"),
                                "%" + request.getIpAddress().trim() + "%"
                        )
                );
            }

            // ===== DESCRIPTION KEYWORD =====
            if (hasText(request.getKeyword())) {
                predicates.add(
                        cb.like(
                                root.get("description"),
                                "%" + request.getKeyword().trim() + "%"
                        )
                );
            }

            // ===== DATE RANGE =====
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

            if (request.getStartDate() != null) {
                Instant start = request.getStartDate()
                        .atStartOfDay(zone)
                        .toInstant();

                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("createdAt"), start)
                );
            }

            if (request.getEndDate() != null) {
                Instant end = request.getEndDate()
                        .atTime(LocalTime.MAX)
                        .atZone(zone)
                        .toInstant();

                predicates.add(
                        cb.lessThanOrEqualTo(root.get("createdAt"), end)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}