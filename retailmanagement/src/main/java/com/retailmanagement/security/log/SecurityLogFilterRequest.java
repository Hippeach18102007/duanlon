package com.retailmanagement.security.log;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
public class SecurityLogFilterRequest {

    private String username;

    private List<String> actionTypes;
    private List<String> targetEntities;
    private List<String> severities;
    private List<String> statuses;

    private String ipAddress;
    private String keyword;

    private LocalDate startDate;
    private LocalDate endDate;
}