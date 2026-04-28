package com.retailmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "username không được để trống")
    @Size(min = 3, max = 50, message = "username 3-50 ký tự")
    private String username;

    @NotBlank(message = "email không được để trống")
    @Email(message = "email không hợp lệ")
    @Size(max = 100, message = "email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "password không được để trống")
    @Size(min = 6, max = 100, message = "password 6-100 ký tự")
    private String password;
    /** Optional — mapped to Customer.dateOfBirth (LocalDate) */
    private LocalDate dateOfBirth;

    /** Optional — mapped to Customer.address */
    private String address;
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9+\\s\\-().]{7,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
