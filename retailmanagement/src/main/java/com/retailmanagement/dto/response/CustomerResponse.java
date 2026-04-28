package com.retailmanagement.dto.response;


import com.retailmanagement.entity.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private Integer id; // MATCH với entity: Integer id (không phải Long customerId)

    private String name; // MATCH với entity: name (không phải fullName)

    private String email;

    private String phone;

    private LocalDate dateOfBirth; // MATCH với entity: dateOfBirth (không phải birthDate)

    private CustomerType customerType;
    private String customerTypeDisplay;

    private VipTier vipTier; // Thêm vipTier
    private String vipTierDisplay;
    private LocalDateTime lastLoginAt;
    private Integer loyaltyPoints; // Thêm loyaltyPoints
    private Integer pointsToNextTier; // Điểm cần để lên hạng
    private Double discountRate; // % giảm giá
    private String vipNote;
    private BigDecimal totalSpent; // Thêm totalSpent
    private LocalDateTime lastOrderAt; // Thêm lastOrderAt
    private String address; // Thêm address
    private String notes; // MATCH với entity: notes (không phải note)
    private String segmentsJson; // Thêm segmentsJson
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String birthdayDisplay; // Format: dd/MM/yyyy
    private Integer birthMonth; // 1-12
    private Integer birthDay; // 1-31
    private Integer age; // Tuổi hiện tại hoặc tuổi sẽ đạt được
    private Long daysUntilBirthday;
    @Builder.Default
    private Integer orderCount = 0;
}