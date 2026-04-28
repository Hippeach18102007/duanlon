package com.retailmanagement.dto.response;

public class UserActionReportResponse {
    private Integer userId;
    private String username;
    private Long totalActions;

    public UserActionReportResponse(Integer userId, String username, Long totalActions) {
        this.userId = userId;
        this.username = username;
        this.totalActions = totalActions;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getTotalActions() {
        return totalActions;
    }
}
