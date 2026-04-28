package com.retailmanagement.security.permission;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@permissionService.hasPermission(this.value.name())")
public @interface CheckPermission {
    PermissionEnum value();
}
