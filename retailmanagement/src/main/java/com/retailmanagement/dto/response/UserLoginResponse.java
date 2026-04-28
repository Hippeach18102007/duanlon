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
public class UserLoginResponse {

    private Long id;
    private Integer userId;
    private String username;
    private Boolean success;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
    private Instant updatedAt;
}
