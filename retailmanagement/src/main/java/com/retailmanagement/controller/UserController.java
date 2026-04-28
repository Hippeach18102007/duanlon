package com.retailmanagement.controller;

import com.retailmanagement.dto.request.CreateUserRequest;
import com.retailmanagement.dto.request.UpdateUserPasswordRequest;
import com.retailmanagement.dto.request.UpdateUserRequest;
import com.retailmanagement.dto.request.UpdateUserRoleRequest;
import com.retailmanagement.dto.response.UserResponse;
import com.retailmanagement.security.permission.CheckPermission;
import com.retailmanagement.security.permission.PermissionEnum;
import com.retailmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @CheckPermission(PermissionEnum.USER_VIEW)
    @GetMapping("")
    public List<UserResponse> getAllUsers(){
        return userService.findAll();
    }

    @CheckPermission(PermissionEnum.USER_CREATE)
    @PostMapping("add")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request){
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @CheckPermission(PermissionEnum.USER_UPDATE)
    @PutMapping("update")
    public ResponseEntity<UserResponse> UpdateUser(@Valid @RequestBody UpdateUserRequest request, @RequestParam("id") Integer id){
        UserResponse response = userService.updateUser(request,id);
        return ResponseEntity.ok(response);
    }

    @CheckPermission(PermissionEnum.USER_UPDATE)
    @PutMapping("update/password")
    public ResponseEntity<UserResponse> UpdateUserPassword(@Valid @RequestBody UpdateUserPasswordRequest request, @RequestParam("id") Integer id){
        UserResponse response = userService.updateUserPassword(request,id);
        return ResponseEntity.ok(response);
    }

    @CheckPermission(PermissionEnum.USER_UPDATE)
    @PutMapping("update/role")
    public ResponseEntity<UserResponse> UpdateUserRole(@Valid @RequestBody UpdateUserRoleRequest request, @RequestParam("id") Integer id){
        UserResponse response = userService.updateUserRole(request,id);
        return ResponseEntity.ok(response);
    }

    @CheckPermission(PermissionEnum.USER_DELETE)
    @DeleteMapping("delete")
    public ResponseEntity<UserResponse> DeleteUser(@RequestParam("id") Integer id){
        UserResponse response = userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
