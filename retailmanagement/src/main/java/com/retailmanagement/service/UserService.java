package com.retailmanagement.service;

import com.retailmanagement.audit.Audit;
import com.retailmanagement.audit.AuditAction;
import com.retailmanagement.audit.AuditModule;
import com.retailmanagement.audit.TargetType;
import com.retailmanagement.dto.request.CreateUserRequest;
import com.retailmanagement.dto.request.UpdateUserPasswordRequest;
import com.retailmanagement.dto.request.UpdateUserRequest;
import com.retailmanagement.dto.request.UpdateUserRoleRequest;
import com.retailmanagement.dto.response.UserResponse;
import com.retailmanagement.entity.Role;
import com.retailmanagement.entity.User;
import com.retailmanagement.repository.RoleRepository;
import com.retailmanagement.repository.UserRepository;
import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SensitiveOperation;
import com.retailmanagement.security.log.SeverityLevel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Role hợp lệ cho staff (không cho phép tạo CUSTOMER qua API này)
    private static final List<String> ALLOWED_ROLES = List.of("ADMIN", "SALES", "INVENTORY", "CUSTOMER");

    @SensitiveOperation(
            action = ActionType.CREATE_OPERATION,
            entity = "USER",
            description = "Create new user",
            severity = SeverityLevel.MEDIUM
    )
    @Audit(
            module = AuditModule.USER,
            action = AuditAction.CREATE,
            targetType = TargetType.USER
    )
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        // ✅ FIX: đọc role từ request thay vì hardcode "CUSTOMER"
        String roleName = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole().toUpperCase()
                : "ADMIN"; // fallback mặc định cho trang admin

        if (!ALLOWED_ROLES.contains(roleName)) {
            throw new RuntimeException("Role không hợp lệ: " + roleName + ". Chỉ chấp nhận: " + ALLOWED_ROLES);
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại trong hệ thống: " + roleName));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        return toResponse(userRepository.save(user));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @SensitiveOperation(
            action = ActionType.UPDATE_OPERATION,
            entity = "USER",
            description = "Update user info",
            severity = SeverityLevel.MEDIUM
    )
    @Audit(
            module = AuditModule.USER,
            action = AuditAction.UPDATE,
            targetType = TargetType.USER
    )
    @Transactional
    public UserResponse updateUser(UpdateUserRequest request, Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
            throw new RuntimeException("Username đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        return toResponse(userRepository.save(user));
    }

    @SensitiveOperation(
            action = ActionType.CHANGE_PASSWORD,
            entity = "USER",
            description = "User changed their passsword",
            severity = SeverityLevel.HIGH
    )
    @Audit(
            module = AuditModule.USER,
            action = AuditAction.CHANGE_PASSWORD,
            targetType = TargetType.USER
    )
    @Transactional
    public UserResponse updateUserPassword(UpdateUserPasswordRequest request, Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        return toResponse(userRepository.save(user));
    }

    @Audit(
            module = AuditModule.USER,
            action = AuditAction.CHANGE_ROLE,
            targetType = TargetType.USER
    )
    @SensitiveOperation(
            action = ActionType.ROLE_CHANGE,
            entity = "USER",
            description = "Change user role",
            severity = SeverityLevel.HIGH
    )
    @Transactional
    public UserResponse updateUserRole(UpdateUserRoleRequest request, Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        Role newRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        Role oldRole = user.getRole();

        if (oldRole.getName().equals(newRole.getName())) {
            throw new RuntimeException("Role không thay đổi");
        }

        user.setRole(newRole);

        return toResponse(userRepository.save(user));
    }

    @Audit(
            module = AuditModule.USER,
            action = AuditAction.DELETE,
            targetType = TargetType.USER
    )
    @SensitiveOperation(
            action = ActionType.DELETE_OPERATION,
            entity = "USER",
            description = "Delete user",
            severity = SeverityLevel.HIGH
    )
    @Transactional
    public UserResponse deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setIsActive(false);
        userRepository.save(user);

        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}