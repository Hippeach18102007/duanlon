package com.retailmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    // có thể là username hoặc email
    @NotBlank(message = "identifier không được để trống")
    private String identifier;

    @NotBlank(message = "password không được để trống")
    private String password;
}
