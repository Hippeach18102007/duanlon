package com.retailmanagement.dto.response;


import com.retailmanagement.entity.CustomerType;
import com.retailmanagement.entity.VipTier;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerBirthdayResponse {
    private Integer customerId;
    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String birthdayDisplay; // Format: dd/MM/yyyy
    private Integer birthMonth; // 1-12
    private Integer birthDay; // 1-31
    private Integer age; // Tuổi hiện tại hoặc tuổi sẽ đạt được
    private Long daysUntilBirthday; // Số ngày đến sinh nhật tiếp theo
    private Boolean isBirthdayToday;
    private CustomerType customerType;
    private VipTier vipTier;
    private Integer loyaltyPoints;
    private BigDecimal totalSpent;
}