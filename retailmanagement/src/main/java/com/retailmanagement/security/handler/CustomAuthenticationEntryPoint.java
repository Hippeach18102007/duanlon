package com.retailmanagement.security.handler;

import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SecurityLogService;
import com.retailmanagement.security.log.SeverityLevel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityLogService securityLogService;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        securityLogService.log(
                "UNKNOWN",
                ActionType.UNAUTHORIZED_ACCESS,
                null,
                null,
                request.getRemoteAddr(),
                SeverityLevel.HIGH,
                "401",
                "Unauthorized access attempt"
        );

        response.sendError(401, "Unauthorized");
    }
}
