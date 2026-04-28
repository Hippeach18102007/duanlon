package com.retailmanagement.security.handler;

import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SecurityLogService;
import com.retailmanagement.security.log.SeverityLevel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityLogService securityLogService;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        String username = "UNKNOWN";

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        securityLogService.log(
                username,
                ActionType.FORBIDDEN_ACCESS,
                null,
                null,
                request.getRemoteAddr(),
                SeverityLevel.HIGH,
                "403",
                "Access denied"
        );

        response.sendError(403, "Forbidden");
    }
}
