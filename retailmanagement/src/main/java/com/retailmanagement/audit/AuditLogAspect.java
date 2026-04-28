package com.retailmanagement.audit;

import com.retailmanagement.dto.response.CreateOrderResponse;
import com.retailmanagement.dto.response.CustomerResponse;
import com.retailmanagement.dto.response.UserResponse;
import com.retailmanagement.util.IpUtil;
import com.retailmanagement.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;
    private final AuditArgsBuilder auditArgsBuilder;

    //Chỉ chạy khi Method chạy thành công (Ví dụ method: method Create, method Update)
    @AfterReturning(
            // Chỉ chạy khi có annotation audit (@Audit)
            pointcut = "@annotation(audit)",
            // Lấy dữ liệu của method (Ví dụ method: method Create, method Update)
            returning = "result"
    )
    public void logAfterSuccess(
            JoinPoint joinPoint,
            Audit audit,
            Object result
    ) {
        try {
            // Lấy id của user thao tác
            Integer userId = SecurityUtil.getCurrentUserId();

            // Lấy ip của user thao tác
            String ip = IpUtil.getClientIp(request);

            // Lấy id của đối tượng tác động (Ví dụ đối tượng: User, Customer, Product,...)
            Long targetId = extractTargetId(joinPoint, result, audit);

            //
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("method", joinPoint.getSignature().getName());
            details.put("args", auditArgsBuilder.build(audit.module(),audit.action(), joinPoint.getArgs()));

            String detailsJson = objectMapper.writeValueAsString(details);


            // Lưu
            auditLogService.save(
                    userId,
                    audit.module(),
                    audit.action(),
                    audit.targetType(),
                    targetId,
                    detailsJson,
                    ip
            );
        } catch (Exception e) {
            System.err.println("AUDIT LOG FAILED: " + e.getMessage());
        }
    }

    //Hàm lấy id của đối tượng bị thao tác
    private Long extractTargetId(JoinPoint joinPoint, Object result, Audit audit) {

        // DELETE → lấy id từ method param
        if(audit.action() == AuditAction.DELETE){
            for(Object args: joinPoint.getArgs()){
                if(args instanceof Integer i){
                    return i.longValue();
                }
                if(args instanceof Long l){
                    return l;
                }
            }
        }

        // CREATE → lấy từ result
        if(audit.action() == AuditAction.CREATE){
            if(result instanceof UserResponse user){
                return Long.valueOf(user.getId());
            }
            if(result instanceof CustomerResponse customer){
                return Long.valueOf(customer.getId());
            }
            if(result instanceof CreateOrderResponse orderResponse){
                return Long.valueOf(orderResponse.getOrderId());
            }
        }

        // UPDATE (void or not) -> lấy từ param
        if(audit.action() == AuditAction.UPDATE) {
            Long idFromArgs = extractIdFromArgs(joinPoint.getArgs());
            if(idFromArgs != null){
                return idFromArgs;
            }
        }

        return null;
    }

    private Long extractIdFromArgs(Object[] args) {
        for(Object arg: args) {
            if(arg instanceof Long l) return l;
            if(arg instanceof Integer i) return i.longValue();
        }
        return null;
    }
}
