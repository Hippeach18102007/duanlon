package com.retailmanagement.audit;

import com.retailmanagement.dto.request.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AuditArgsBuilder {

    public Object build(
            AuditModule module,
            AuditAction action,
            Object[] args
    ) {

        return switch (module) {
            case USER -> buildUserArgs(action, args);
            case CUSTOMER -> buildCustomerArgs(action, args);
            case ORDER -> buildOrderArgs(action, args);
            default -> "N/A";
        };
    }

    private Object buildUserArgs(AuditAction action, Object[] args) {
        if(action == AuditAction.CHANGE_PASSWORD){
            return "Password has been changed";
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (Object arg: args) {
            if(arg instanceof Integer id) {
                map.put("id",id);
            }

            if(arg instanceof CreateUserRequest createUserRequest){
                putIfNotNull(map,"name",createUserRequest.getUsername());
                putIfNotNull(map,"email",createUserRequest.getEmail());
                putIfNotNull(map,"password","Private");
                putIfNotNull(map,"role",createUserRequest.getRole());
            }

            if(arg instanceof UpdateUserRequest updateUserRequest){
                putIfNotNull(map,"name",updateUserRequest.getUsername());
                putIfNotNull(map,"email",updateUserRequest.getEmail());
            }

            if(arg instanceof UpdateUserRoleRequest updateUserRoleRequest) {
                putIfNotNull(map,"role",updateUserRoleRequest.getRole());
            }
        }
        return map.isEmpty() ? "No changes" : map;
    }

    private Object buildCustomerArgs(AuditAction action, Object[] args) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (Object arg: args) {
            if(arg instanceof Integer id){
                map.put("id",id);
            }

            if(arg instanceof CustomerRequest cr) {
                putIfNotNull(map, "fullName", cr.getFullName());
                putIfNotNull(map, "email", cr.getEmail());
                putIfNotNull(map, "phone", cr.getPhone());
                putIfNotNull(map,"birthDate",cr.getBirthDate());
                putIfNotNull(map, "customerType", cr.getCustomerType());
                putIfNotNull(map, "note",cr.getNote());
                putIfNotNull(map, "address", cr.getAddress());
                putIfNotNull(map, "notes",cr.getNotes());
            }
        }
        return map.isEmpty() ? "No changes" : map;
    }

    private Object buildOrderArgs(AuditAction action, Object[] args){
        Map<String, Object> map =  new LinkedHashMap<>();

        for (Object arg: args) {
            if(arg instanceof Long id) {
                map.put("id",id);
            }

            if(arg instanceof CreateOrderRequest createOrderRequest){
                putIfNotNull(map, "CustomerId", createOrderRequest.getCustomerId());
                putIfNotNull(map, "paymentMethod", createOrderRequest.getPaymentMethod());
                putIfNotNull(map, "channel", createOrderRequest.getChannel());
                putIfNotNull(map, "notes", createOrderRequest.getNotes());
                putIfNotNull(map, "items", createOrderRequest.getItems());
            }

            if(arg instanceof UpdateOrderRequest updateOrderRequest) {
                putIfNotNull(map, "paymentMethod", updateOrderRequest.getPaymentMethod());
                putIfNotNull(map, "notes", updateOrderRequest.getNotes());
            }
        }

        return map.isEmpty() ? "No changes" : map;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
