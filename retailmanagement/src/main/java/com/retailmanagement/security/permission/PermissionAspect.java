package com.retailmanagement.security.permission;

import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SecurityLogService;
import com.retailmanagement.security.log.SeverityLevel;
import com.retailmanagement.security.service.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;
    private final SecurityLogService securityLogService;

    @Before("@annotation(checkPermission)")
    public void checkPermission(CheckPermission checkPermission) {

        String permission = checkPermission.value().name();

        if (!permissionService.hasPermission(permission)) {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = "UNKNOWN";

            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
                username = user.getUsername();
            }

            // 👉 Lấy request info
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            String ip = "UNKNOWN";
            String endpoint = "UNKNOWN";

            if (attr != null) {
                HttpServletRequest request = attr.getRequest();
                ip = request.getRemoteAddr();
                endpoint = request.getRequestURI();
            }

            // 🔥 GHI SECURITY LOG
            securityLogService.log(
                    username,
                    ActionType.FORBIDDEN_ACCESS,
                    "API",
                    null,
                    ip,
                    SeverityLevel.HIGH,
                    "FAILED",
                    "Truy cập trái phép vào [" + endpoint + "] với permission [" + permission + "]"
            );

            throw new AccessDeniedException("Forbidden");
        }
    }
}
