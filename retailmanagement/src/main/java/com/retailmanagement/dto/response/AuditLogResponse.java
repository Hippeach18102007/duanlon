package com.retailmanagement.dto.response;

import com.retailmanagement.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private Integer userId;
    private String module;
    private String action;
    private String targetType;
    private Long targetId;
    private String detailsJson;
    private String ipAddress;
    private Instant createdAt;
}
