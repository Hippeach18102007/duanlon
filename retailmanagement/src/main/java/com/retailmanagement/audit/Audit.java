package com.retailmanagement.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    AuditModule module();
    AuditAction action();
    TargetType targetType();
}
