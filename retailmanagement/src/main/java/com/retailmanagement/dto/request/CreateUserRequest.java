package com.retailmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    //Dùng để tạo User mới
    @NotBlank(message = "Username không được để trống")
    @Size(max = 50, message = "Username tối đa 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tụ")
    private String password; // plaintext từ client

    private String role; //Chỉ được nhập ADMIN,SALES,INVENTORY,CUSTOMER

    /** Optional — mapped to Customer.dateOfBirth (LocalDate) */
    private LocalDate dateOfBirth;

    /** Optional — mapped to Customer.address */
    private String address;
}
