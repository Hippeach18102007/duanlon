package com.retailmanagement.audit;

import com.retailmanagement.dto.request.UserLoginFilterRequest;
import com.retailmanagement.entity.UserLogin;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class UserLoginSpecification {

    public static Specification<UserLogin> filter(UserLoginFilterRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            /* ================= USERNAME ================= */

            if (request.getUsername() != null && !request.getUsername().isBlank()) {

                predicates.add(
                        cb.like(
                                cb.lower(root.get("user").get("username")),
                                "%" + request.getUsername().toLowerCase().trim() + "%"
                        )
                );
            }

            /* ================= SUCCESS ================= */

            if (request.getSuccess() != null) {
                predicates.add(
                        cb.equal(root.get("success"), request.getSuccess())
                );
            }

            /* ================= IP ADDRESS ================= */

            if (request.getIpAddress() != null && !request.getIpAddress().isBlank()) {

                predicates.add(
                        cb.like(
                                cb.lower(root.get("ipAddress")),
                                "%" + request.getIpAddress().toLowerCase().trim() + "%"
                        )
                );
            }

            /* ================= DATE RANGE ================= */

            ZoneId zone = ZoneId.systemDefault();

            // FROM DATE (00:00:00)
            if (request.getFromDate() != null) {

                Instant fromInstant =
                        request.getFromDate()
                                .atStartOfDay(zone)
                                .toInstant();

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                fromInstant
                        )
                );
            }

            // TO DATE (23:59:59.999)
            if (request.getToDate() != null) {

                Instant toInstant =
                        request.getToDate()
                                .plusDays(1)
                                .atStartOfDay(zone)
                                .minusNanos(1)
                                .toInstant();

                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("createdAt"),
                                toInstant
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}