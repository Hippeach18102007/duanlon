package com.retailmanagement.security.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SecurityLogRepository
        extends JpaRepository<SecurityLog, Long>,
                JpaSpecificationExecutor<SecurityLog> {

    @Query(value = """
    SELECT COUNT(*) 
    FROM security_log
    WHERE action_type = :action
      AND severity = 'HIGH'
      AND created_at >= DATEADD(MINUTE, -5, GETUTCDATE())
    """, nativeQuery = true)
    long countRecentHigh(@Param("action") String action);

    long countByCreatedAtBetween(Instant start, Instant end);

    long countByActionTypeInAndCreatedAtBetween(
            List<ActionType> actionTypes,
            Instant start,
            Instant end
    );

    @Query("""
        SELECT l.severity, COUNT(l)
        FROM SecurityLog l
        WHERE l.createdAt BETWEEN :start AND :end
        GROUP BY l.severity
    """)
    List<Object[]> countBySeverity(Instant start, Instant end);
}
