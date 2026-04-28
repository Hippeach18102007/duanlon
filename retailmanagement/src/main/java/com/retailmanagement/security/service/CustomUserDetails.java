package com.retailmanagement.security.service;

import com.retailmanagement.entity.User;
import com.retailmanagement.security.permission.PermissionEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class CustomUserDetails implements UserDetails {

    private final Integer userId;
    private final String username;
    private Integer customerId;
    private final String password;
    private final String role;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Set<PermissionEnum> permissions;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.role = user.getRole().getName();
        this.enabled = Boolean.TRUE.equals(user.getIsActive());

        this.permissions = user.getRole() != null
                ? user.getRole().getPermissions()
                .stream()
                .map(p -> {
                    try {
                        return PermissionEnum.valueOf(
                                (p.getModule() + "_" + p.getAction()).toUpperCase()
                        );
                    } catch (Exception e) {
                        throw new IllegalStateException("Permission không hợp lệ trong DB");
                    }
                })
                .collect(Collectors.toSet())
                : Set.of();

        this.authorities = buildAuthorities();
    }

    public Set<PermissionEnum> getPermissions() {
        return permissions;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private Collection<? extends GrantedAuthority> buildAuthorities() {
        List<GrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("ROLE_" + role));
        permissions.forEach(p -> auths.add(new SimpleGrantedAuthority(p.name())));

        System.out.println("=== buildAuthorities ===");
        System.out.println("role: " + role);
        System.out.println("permissions: " + permissions);
        System.out.println("auths: " + auths);
        System.out.println("=======================");

        return auths;
    }
}
