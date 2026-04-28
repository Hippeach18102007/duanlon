package com.retailmanagement.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_log")
@Data
public class ProductHistoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;

    @Column(name = "action_type")
    private String actionType;

    private String description;

    @Column(name = "ip_address")
    private String ipAddress;

    private String severity;
    private String status;

    @Column(name = "target_entity")
    private String targetEntity;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}