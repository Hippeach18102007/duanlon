package com.retailmanagement.audit;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    //Helper để ignore rỗng
    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean hasList(List<?> list) {
        return list != null && !list.isEmpty();
    }

    // Filter logic
    public static Specification<AuditLog> filter(AuditLogFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates =  new ArrayList<>();

            // ===== USER =====
            if (request.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), request.getUserId()));
            }

            // ===== MODULE =====

            if (hasList(request.getModules())) {
                List<String> modules = request.getModules().stream()
                        .filter(m -> m != null && !m.isBlank())
                        .toList();

                if (!modules.isEmpty()) {
                    predicates.add(root.get("module").in(modules));
                }
            }

            // ===== ACTION =====

            if (hasList(request.getActions())) {
                List<String> actions = request.getActions().stream()
                        .filter(a -> a != null && !a.isBlank())
                        .toList();

                if (!actions.isEmpty()) {
                    predicates.add(root.get("action").in(actions));
                }
            }

            // ===== TARGET =====
            if (hasText(request.getTargetType())) {
                predicates.add(
                        criteriaBuilder.equal(root.get("targetType"), request.getTargetType().trim())
                );
            }

            if (request.getTargetId() != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("targetId"), request.getTargetId())
                );
            }

            // ===== IP =====
            if (hasText(request.getIpAddress())) {
                predicates.add(
                        criteriaBuilder.equal(root.get("ipAddress"), request.getIpAddress().trim())
                );
            }


            // ===== KEYWORD =====
            if (hasText(request.getKeyword())) {
                predicates.add(
                        criteriaBuilder.like(
                                root.get("detailsJson"),
                                "%" + request.getKeyword().trim() + "%"
                        )
                );
            }

            // ===== DATE =====
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

            if (request.getStartDate() != null) {
                Instant start = request.getStartDate()
                        .atStartOfDay(zone)
                        .toInstant();

                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), start
                ));
            }

            if (request.getEndDate() != null) {
                Instant end = request.getEndDate()
                        .atTime(LocalTime.MAX)
                        .atZone(zone)
                        .toInstant();

                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), end
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
