package com.retailmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserPasswordRequest {
    @NotBlank(message = "Old password không được để trống")
    String oldPassword;

    @NotBlank(message = "New password không được để trống")
    String newPassword;
}
