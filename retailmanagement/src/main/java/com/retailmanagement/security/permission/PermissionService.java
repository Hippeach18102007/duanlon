package com.retailmanagement.security.permission;

import com.retailmanagement.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("permissionService")
@RequiredArgsConstructor
public class PermissionService {

    public boolean hasPermission(String permission) {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Unauthorized");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails user)) {
            return false;
        }

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }

        return user.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
