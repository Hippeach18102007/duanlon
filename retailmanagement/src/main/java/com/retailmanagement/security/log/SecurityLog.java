package com.retailmanagement.security.log;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "security_log")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SecurityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 100)
    @Nationalized
    @Column(name = "username", length = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 100)
    private ActionType actionType;

    @Size(max = 100)
    @Nationalized
    @Column(name = "target_entity", length = 100)
    private String targetEntity;

    @Size(max = 100)
    @Nationalized
    @Column(name = "target_id", length = 100)
    private String targetId;

    @Size(max = 50)
    @Nationalized
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private SeverityLevel severity;

    @Size(max = 50)
    @Nationalized
    @Column(name = "status", length = 50)
    private String status;

    @Nationalized
    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

}