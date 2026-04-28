package com.retailmanagement.security.log;

import com.retailmanagement.security.service.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;

@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {

    private final SecurityLogService securityLogService;

    @Around("@annotation(sensitive)")
    public Object logSensitive(ProceedingJoinPoint joinPoint,
                               SensitiveOperation sensitive) throws Throwable {
        String username = SecurityContextHolder.getContext()
                .getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "UNKNOWN";

        String ip = "UNKNOWN";
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs != null) {
            ip = attrs.getRequest().getRemoteAddr();
        }

        String targetId = extractTargetId(joinPoint);

        SeverityLevel severity = sensitive.severity();
        String status = "SUCCESS";
        String description = sensitive.description();

        try {

            // CHANGE_ROLE thì kiểm tra quyền
            if (sensitive.action() == ActionType.ROLE_CHANGE) {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                    throw new AccessDeniedException("Unauthenticated access");
                }

                boolean isAdmin = authentication.getAuthorities()
                        .stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                //  Không phải admin mà đổi quyền
                if (!isAdmin) {
                    securityLogService.log(
                            username,
                            sensitive.action(),
                            sensitive.entity(),
                            targetId,
                            ip,
                            SeverityLevel.CRITICAL,
                            "BLOCKED",
                            "Unauthorized role change attempt"
                    );

                    throw new AccessDeniedException("Không có quyền đổi role");
                }

                //  Tự nâng quyền
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                Integer currentUserId = userDetails.getUserId();
                if (String.valueOf(currentUserId).equals(targetId)) {
                    securityLogService.log(
                            username,
                            sensitive.action(),
                            sensitive.entity(),
                            targetId,
                            ip,
                            SeverityLevel.CRITICAL,
                            "BLOCKED",
                            "User attempted to change own role"
                    );

                    throw new AccessDeniedException("Không được tự đổi role của chính mình");
                }
            }

            Object result = joinPoint.proceed();

            securityLogService.log(
                    username,
                    sensitive.action(),
                    sensitive.entity(),
                    targetId,
                    ip,
                    severity,
                    status,
                    description
            );

            return result;

        } catch (Exception ex) {

            securityLogService.log(
                    username,
                    sensitive.action(),
                    sensitive.entity(),
                    targetId,
                    ip,
                    SeverityLevel.CRITICAL,
                    "FAILED",
                    ex.getMessage()
            );

            throw ex;
        }
    }

    private String extractTargetId(ProceedingJoinPoint joinPoint) {

        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) continue;

            if (arg instanceof Integer || arg instanceof Long) {
                return String.valueOf(arg);
            }

            try {
                Field field = arg.getClass().getDeclaredField("id");
                field.setAccessible(true);
                Object value = field.get(arg);
                if (value != null) {
                    return String.valueOf(value);
                }
            } catch (Exception ignored) {}
        }

        return "UNKNOWN";
    }
}
