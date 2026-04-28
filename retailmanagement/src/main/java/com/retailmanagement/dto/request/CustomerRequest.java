package com.retailmanagement.dto.request;


import com.retailmanagement.entity.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {
    private Integer userId;
    @NotBlank(message = "Không được để trống tên khách hàng")
    @Size(max = 200, message = "Tên không được vượt quá 200 ký tự")
    private String fullName;
    @Email(message = "Email không hợp lệ")
    @Size(max = 200, message = "Email không được vượt quá 200 ký tự")
    @NotBlank(message = "Không được để trống email khách hàng")
    private String email;
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    @NotBlank(message = "Không được để trống số điện thoại khách hàng")
    private String phone;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate birthDate;

    private CustomerType customerType;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;
    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address; // Thêm field address

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự")
    private String notes; // MATCH với entity: notes (không phải note)
    private String vipNote;
}
