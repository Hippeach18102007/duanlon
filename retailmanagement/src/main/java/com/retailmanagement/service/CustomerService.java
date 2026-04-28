package com.retailmanagement.service;

import com.retailmanagement.audit.Audit;
import com.retailmanagement.audit.AuditAction;
import com.retailmanagement.audit.AuditModule;
import com.retailmanagement.audit.TargetType;
import com.retailmanagement.dto.request.CustomerRequest;
import com.retailmanagement.dto.response.CustomerResponse;
import com.retailmanagement.entity.*;
import com.retailmanagement.repository.*;
import com.retailmanagement.security.log.ActionType;
import com.retailmanagement.security.log.SensitiveOperation;
import com.retailmanagement.security.log.SeverityLevel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
private final EmailService emailService;
    private final CustomRes customRes;
    private final LoyaltyLedgerRepository loyaltyLedgerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    @Lazy
    private final CustomerEventNotificationService eventNotificationService; // ✅ THÊM

    @SensitiveOperation(
            action = ActionType.UPDATE_OPERATION,
            entity = "CUSTOMER",
            description = "Save loyalty ledger",
            severity = SeverityLevel.LOW
    )
    @Audit(
            module = AuditModule.CUSTOMER,
            action = AuditAction.UPDATE,
            targetType = TargetType.CUSTOMER
    )
    private void saveLoyaltyLedger(
            Customer customer,
            int pointsDelta,
            String transactionType,
            VipTier tierBefore,
            VipTier tierAfter,
            String reason,
            String note,
            String referenceType,
            Long referenceId,
            Integer createdBy) {

        User createdByUser = null;
        if (createdBy != null) {
            createdByUser = userRepository.findById(createdBy).orElse(null);
        }

        LoyaltyLedger ledger = LoyaltyLedger.builder()
                .customer(customer)
                .pointsDelta(pointsDelta)
                .transactionType(transactionType)
                .tierBefore(tierBefore != null ? tierBefore.name() : null)
                .tierAfter(tierAfter != null ? tierAfter.name() : null)
                .reason(reason)
                .note(note)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .createdBy(createdByUser)
                .createdAt(Instant.now())
                .build();

        loyaltyLedgerRepository.save(ledger);

        System.out.println("✅ Saved loyalty ledger: " + transactionType + " | Points: " + pointsDelta +
                " | Tier: " + (tierBefore != null ? tierBefore.name() : "NULL") +
                " → " + (tierAfter != null ? tierAfter.name() : "NULL") +
                " | Created by: " + (createdByUser != null ? createdByUser.getUsername() : "SYSTEM"));
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,d VNĐ", amount.longValue());
    }

    private String getDeductNote(String reason, BigDecimal amount) {
        return switch (reason) {
            case "CANCEL_ORDER" -> "Trừ điểm do hủy đơn: " + formatMoney(amount);
            case "RETURN" -> "Trừ điểm do trả hàng: " + formatMoney(amount);
            default -> "Trừ điểm: " + formatMoney(amount);
        };
    }

    @SensitiveOperation(
            action = ActionType.CREATE_OPERATION,
            entity = "CUSTOMER",
            description = "Create new customer",
            severity = SeverityLevel.MEDIUM
    )
    @Audit(module = AuditModule.CUSTOMER, action = AuditAction.CREATE, targetType = TargetType.CUSTOMER)
    @Transactional
    public CustomerResponse create(CustomerRequest customerRequest) {

        // Validate trùng email/phone
        if (customRes.findByEmail(customerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        if (customRes.findByPhone(customerRequest.getPhone()).isPresent()) {
            throw new RuntimeException("Số điện thoại đã tồn tại trong hệ thống");
        }


        // ✅ TỰ ĐỘNG TẠO USER với username = email, password = SĐT
        User user = null;
        Role role = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role not found"));
        if (userRepository.findByUsername(customerRequest.getEmail()).isEmpty()) {
            user = User.builder()
                    .username(customerRequest.getEmail())
                    .passwordHash(passwordEncoder.encode(customerRequest.getPhone())) // ✅ đúng tên field
                    .email(customerRequest.getEmail())
                    .role(role)  // ✅ khớp với @PrePersist default
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
        } else {
            user = userRepository.findByUsername(customerRequest.getEmail()).get();
        }

        CustomerType type = (customerRequest.getCustomerType() != null)
                ? customerRequest.getCustomerType()
                : CustomerType.REGULAR;

        Customer customer = Customer.builder()
                .userId(user.getId())                        // ✅ Link tới User vừa tạo
                .name(customerRequest.getFullName())
                .email(customerRequest.getEmail())
                .phone(customerRequest.getPhone())
                .dateOfBirth(customerRequest.getBirthDate())
                .address(customerRequest.getAddress())
                .notes(customerRequest.getNotes())
                .customerType(type)
                .vipTier(null)
                .totalSpent(BigDecimal.ZERO)
                .loyaltyPoints(0)
                .isActive(true)
                .vipNote(customerRequest.getVipNote())
                .build();

        Customer saved = customRes.save(customer);
        eventNotificationService.onCustomerRegistered(saved);
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            emailService.sendWelcomePasswordEmail(
                    saved.getEmail(),
                    saved.getName(),
                    saved.getEmail(),
                    customerRequest.getPhone()
            );
        }
        return mapToResponse(saved);
    }


    private CustomerResponse mapToResponse(Customer customer) {
        VipTier currentTier = customer.getVipTier();
        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;

        Integer pointsToNext = 0;
        int orderCount = (int) orderRepository.countByCustomerId(customer.getId());

        if (currentTier == null) {
            pointsToNext = VipTier.BRONZE.getMinPoints() - currentPoints;
        } else {
            Integer nextThreshold = currentTier.getNextTierThreshold();
            if (nextThreshold != null) {
                pointsToNext = nextThreshold - currentPoints;
            } else {
                pointsToNext = 0;
            }
        }
        if (pointsToNext < 0) pointsToNext = 0;

        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .dateOfBirth(customer.getDateOfBirth())
                .customerType(customer.getCustomerType())
                .customerTypeDisplay(customer.getCustomerType().getDisplayname())
                .vipTier(currentTier)
                .vipTierDisplay(currentTier != null ? currentTier.getDisplayName() : "Member")
                .loyaltyPoints(currentPoints)
                .pointsToNextTier(pointsToNext)
                .discountRate(currentTier != null ? currentTier.getDiscountRate() : 0.0)
                .lastLoginAt(customer.getLastLoginAt())
                .totalSpent(customer.getTotalSpent())
                .lastOrderAt(customer.getLastOrderAt())
                .address(customer.getAddress())
                .notes(customer.getNotes())
                .segmentsJson(customer.getSegmentsJson())
                .isActive(customer.getIsActive())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .vipNote(customer.getVipNote())
                .orderCount(orderCount)
                .build();
    }

    // ================================================================
    // OVERLOAD: gọi từ chỗ không có orderId (backward compatible)
    // ================================================================
    @Transactional
    public void addLoyaltyPoints(Integer customerId, BigDecimal orderTotal) {
        addLoyaltyPoints(customerId, orderTotal, null);
    }

    // ================================================================
    // MAIN: gọi từ PaymentService khi thanh toán thành công
    // ================================================================
    // ===== HELPER =====


    // ===== MAIN METHOD =====
    @Transactional
    public void addLoyaltyPoints(Integer customerId, BigDecimal orderTotal, Long orderId) {
        Customer customer = customRes.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        boolean alreadyEarned = loyaltyLedgerRepository
                .existsByReferenceTypeAndReferenceIdAndTransactionType("PURCHASE", orderId, "EARN");
        if (alreadyEarned) {
            log.warn("addLoyaltyPoints skipped — orderId {} đã được cộng điểm", orderId);
            return;
        }

        int pointsBefore = customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints();
        VipTier tierBefore = customer.getVipTier();

        // ✅ WEEKEND BONUS x1.5
        int basePoints = orderTotal
                .divide(BigDecimal.valueOf(10000), 0, RoundingMode.DOWN)
                .intValue();
        boolean weekendBonus = isWeekend();
        int pointsEarned = weekendBonus ? (int) Math.round(basePoints * 1.5) : basePoints;

        if (pointsEarned <= 0) return;

        int newTotalPoints = pointsBefore + pointsEarned;
        customer.setLoyaltyPoints(newTotalPoints);

        BigDecimal currentSpent = customer.getTotalSpent() == null ? BigDecimal.ZERO : customer.getTotalSpent();
        customer.setTotalSpent(currentSpent.add(orderTotal));

        VipTier newTier = VipTier.fromPoints(newTotalPoints);
        customer.setVipTier(newTier);

        if (newTier == null || newTier == VipTier.BRONZE || newTier == VipTier.SILVER) {
            customer.setCustomerType(CustomerType.REGULAR);
        } else {
            customer.setCustomerType(CustomerType.VIP);
        }

        customRes.save(customer);

        eventNotificationService.onPointsEarned(customer, pointsEarned, newTotalPoints, orderTotal);

        String earnNote = weekendBonus
                ? "🎉 [Weekend x1.5] Cộng điểm từ thanh toán: " + formatMoney(orderTotal)
                + " (gốc: " + basePoints + " → x1.5 = " + pointsEarned + " điểm)"
                : "Cộng điểm từ thanh toán: " + formatMoney(orderTotal);

        saveLoyaltyLedger(
                customer,
                pointsEarned,
                "EARN",
                tierBefore,
                newTier,
                "PURCHASE",
                earnNote,
                "orders",
                orderId,
                null
        );

        if (tierBefore != newTier) {
            boolean isUpgrade = newTier != null && (tierBefore == null ||
                    newTier.ordinal() > tierBefore.ordinal());

            if (!isUpgrade) {
                log.warn("Unexpected tier downgrade during EARN for customerId={}", customerId);
                return; // hoặc throw, tùy business rule
            }

            String transactionType = isUpgrade ? "TIER_UPGRADE" : "TIER_DOWNGRADE";

            String note = String.format("🎉 Thăng hạng từ %s → %s (Điểm: %d)",
                    tierBefore != null ? tierBefore.getDisplayName() : "Member",
                    newTier != null ? newTier.getDisplayName() : "Member",
                    newTotalPoints);

            saveLoyaltyLedger(
                    customer, 0, transactionType,
                    tierBefore, newTier, "TIER_CHANGE", note,
                    null, null, null
            );

            if (isUpgrade) {
                eventNotificationService.onTierUpgrade(customer, tierBefore, newTier, newTotalPoints);
            } else {
                eventNotificationService.onTierDowngrade(customer, tierBefore, newTier, newTotalPoints, "PURCHASE");
            }
        }
    }

    public List<CustomerResponse> findAll() {
        return customRes.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<CustomerResponse> findbyCustomerType(CustomerType type) {
        return customRes.findByCustomerType(type).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @SensitiveOperation(
            action = ActionType.DELETE_OPERATION,
            entity = "CUSTOMER",
            description = "Delete Customer",
            severity = SeverityLevel.MEDIUM
    )
    @Audit(
            module = AuditModule.CUSTOMER,
            action = AuditAction.DELETE,
            targetType = TargetType.CUSTOMER
    )
    public void deleteById(int id) {
        Customer customer = customRes.findById(id).orElseThrow();
        customer.setIsActive(false);
        customRes.save(customer);
    }

    @SensitiveOperation(
            action = ActionType.UPDATE_OPERATION,
            entity = "CUSTOMER",
            description = "Update Customer",
            severity = SeverityLevel.LOW
    )
    @Audit(
            module = AuditModule.CUSTOMER,
            action = AuditAction.UPDATE,
            targetType = TargetType.CUSTOMER
    )
    public CustomerResponse updateById(Integer id, CustomerRequest customerRequest) {
        Customer customer = customRes.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Khách hàng với id: " + id));

        if (customerRequest.getFullName() != null) {
            customer.setName(customerRequest.getFullName());
        }

        String oldEmail = customer.getEmail();
        if (customerRequest.getEmail() != null && !customerRequest.getEmail().equals(oldEmail)) {
            if (customRes.findByEmail(customerRequest.getEmail()).isPresent()) {
                throw new RuntimeException("Email này đã tồn tại trong hệ thống");
            }
            customer.setEmail(customerRequest.getEmail());
        }
        if (customerRequest.getVipNote() != null) {
            customer.setVipNote(customerRequest.getVipNote());
        }

        String oldPhone = customer.getPhone();
        if (customerRequest.getPhone() != null && !customerRequest.getPhone().equals(oldPhone)) {
            if (customRes.findByPhone(customerRequest.getPhone()).isPresent()) {
                throw new RuntimeException("Số điện thoại này đã tồn tại trong hệ thống");
            }
            customer.setPhone(customerRequest.getPhone());
        }

        if (customerRequest.getBirthDate() != null) {
            customer.setDateOfBirth(customerRequest.getBirthDate());
        }
        if (customerRequest.getAddress() != null) {
            customer.setAddress(customerRequest.getAddress());
        }
        if (customerRequest.getNotes() != null) {
            customer.setNotes(customerRequest.getNotes());
        }

        Customer updated = customRes.save(customer);
        return mapToResponse(updated);
    }

    public CustomerResponse findByEmail(String email) {
        // 1. Tìm theo email trong bảng customers
        Optional<Customer> customerOpt = customRes.findByEmail(email.trim());
        if (customerOpt.isPresent()) return mapToResponse(customerOpt.get());

        // 2. Tìm qua bảng users (username = email)
        Optional<User> userOpt = userRepository.findByUsername(email.trim());
        if (userOpt.isPresent()) {
            customerOpt = customRes.findByUserId(userOpt.get().getId());
            if (customerOpt.isPresent()) return mapToResponse(customerOpt.get());
        }

        return null;
    }

    public List<CustomerResponse> findActiveInLast30Days() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Customer> activeCustomers = customRes.findActiveCustomersSince(thirtyDaysAgo);
        return activeCustomers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deductLoyaltyPoints(Integer customerId, BigDecimal orderTotal,
                                    String reason, String referenceType, Long referenceId) {
        Customer customer = customRes.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        int pointsBefore = customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints();
        VipTier tierBefore = customer.getVipTier();

        int pointsToDeduct = orderTotal.divide(BigDecimal.valueOf(10000)).intValue();
        if (pointsToDeduct <= 0) return;

        int newTotalPoints = Math.max(0, pointsBefore - pointsToDeduct);
        customer.setLoyaltyPoints(newTotalPoints);

        BigDecimal currentSpent = customer.getTotalSpent() == null ? BigDecimal.ZERO : customer.getTotalSpent();
        BigDecimal newSpent = currentSpent.subtract(orderTotal);
        customer.setTotalSpent(newSpent.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newSpent);

        VipTier newTier = VipTier.fromPoints(newTotalPoints);
        customer.setVipTier(newTier);

        if (newTier == null || newTier == VipTier.BRONZE || newTier == VipTier.SILVER) {
            customer.setCustomerType(CustomerType.REGULAR);
        } else {
            customer.setCustomerType(CustomerType.VIP);
        }

        customRes.save(customer);

        // ✅ THÊM: thông báo trừ điểm
        eventNotificationService.onPointsDeducted(customer, pointsToDeduct, newTotalPoints, reason);

        String note = getDeductNote(reason, orderTotal);
        saveLoyaltyLedger(
                customer, -pointsToDeduct, "DEDUCT",
                tierBefore, newTier, reason, note,
                referenceType, referenceId, null
        );

        if (tierBefore != newTier) {
            String note2 = String.format("⚠️ Hạ hạng từ %s → %s do trừ điểm (Điểm còn: %d)",
                    tierBefore != null ? tierBefore.getDisplayName() : "Member",
                    newTier != null ? newTier.getDisplayName() : "Member",
                    newTotalPoints);

            saveLoyaltyLedger(
                    customer, 0, "TIER_DOWNGRADE",
                    tierBefore, newTier, "TIER_CHANGE", note2,
                    null, null, null
            );

            // ✅ THÊM: thông báo hạ hạng
            eventNotificationService.onTierDowngrade(customer, tierBefore, newTier, newTotalPoints, reason);
        }
    }

    public List<CustomerResponse> findByPointsRange(int minPoints, int maxPoints) {
        List<Customer> customers = customRes.findByLoyaltyPointsBetween(minPoints, maxPoints);
        return customers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CustomerResponse> findByVipTier(String tierName) {
        try {
            VipTier tier = VipTier.valueOf(tierName.toUpperCase());
            List<Customer> customers = customRes.findByVipTier(tier);
            return customers.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid VIP tier: " + tierName);
        }
    }

    public List<CustomerResponse> findByVipTierAndPointsRange(
            String tierName, int minPoints, int maxPoints) {
        try {
            VipTier tier = VipTier.valueOf(tierName.toUpperCase());
            List<Customer> customers = customRes.findByVipTierAndLoyaltyPointsBetween(
                    tier, minPoints, maxPoints);
            return customers.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid VIP tier: " + tierName);
        }
    }

    public List<CustomerResponse> findBySpendingRange(BigDecimal minSpent, BigDecimal maxSpent) {
        List<Customer> customers = customRes.findByTotalSpentBetween(minSpent, maxSpent);
        return customers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CustomerResponse> findTopSpenders(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Customer> customers = customRes.findTopByTotalSpent(pageable);
        return customers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CustomerResponse> findTopSpendersByVipTier(String tierName, int limit) {
        try {
            VipTier tier = VipTier.valueOf(tierName.toUpperCase());
            Pageable pageable = PageRequest.of(0, limit);
            List<Customer> customers = customRes.findTopByVipTierAndTotalSpent(tier, pageable);
            return customers.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid VIP tier: " + tierName);
        }
    }

    public Map<String, Object> getSpendingStatistics() {
        Map<String, Object> stats = new HashMap<>();

        BigDecimal totalSpent = customRes.getTotalSpentAllCustomers();
        stats.put("totalSpent", totalSpent != null ? totalSpent : BigDecimal.ZERO);

        long totalCustomers = customRes.findAll().stream()
                .filter(Customer::getIsActive)
                .count();
        stats.put("totalCustomers", totalCustomers);

        BigDecimal avgSpent = BigDecimal.ZERO;
        if (totalCustomers > 0 && totalSpent != null) {
            avgSpent = totalSpent.divide(
                    BigDecimal.valueOf(totalCustomers), 2, BigDecimal.ROUND_HALF_UP);
        }
        stats.put("averageSpent", avgSpent);

        Map<String, Object> tierStats = new HashMap<>();
        for (VipTier tier : VipTier.values()) {
            BigDecimal tierTotal = customRes.getTotalSpentByVipTier(tier);
            long tierCount = customRes.findByVipTier(tier).size();

            Map<String, Object> tierData = new HashMap<>();
            tierData.put("totalSpent", tierTotal != null ? tierTotal : BigDecimal.ZERO);
            tierData.put("customerCount", tierCount);
            tierData.put("averageSpent", tierCount > 0 && tierTotal != null
                    ? tierTotal.divide(BigDecimal.valueOf(tierCount), 2, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO);
            tierStats.put(tier.name(), tierData);
        }
        stats.put("byTier", tierStats);
        Map<String, Long> spendingRanges = new HashMap<>();
        spendingRanges.put("under1M", customRes.countByTotalSpentBetween(
                BigDecimal.ZERO, BigDecimal.valueOf(1000000)));
        spendingRanges.put("1M-5M", customRes.countByTotalSpentBetween(
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(5000000)));
        spendingRanges.put("5M-10M", customRes.countByTotalSpentBetween(
                BigDecimal.valueOf(5000000), BigDecimal.valueOf(10000000)));
        spendingRanges.put("10M-50M", customRes.countByTotalSpentBetween(
                BigDecimal.valueOf(10000000), BigDecimal.valueOf(50000000)));
        spendingRanges.put("over50M", customRes.countByTotalSpentBetween(
                BigDecimal.valueOf(50000000), BigDecimal.valueOf(999999999)));
        stats.put("spendingRanges", spendingRanges);

        return stats;
    }

    public List<CustomerResponse> findInactiveTransactionDays(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return customRes.findCustomersInactiveTransaction(cutoff)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerResponse updateVipNote(Integer id, String vipNote) {
        Customer customer = customRes.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + id));
        customer.setVipNote(vipNote);
        return mapToResponse(customRes.save(customer));
    }

    public List<CustomerResponse> findZeroOrderCustomers(int minDaysSinceRegistered) {
        LocalDateTime registeredBefore = LocalDateTime.now().minusDays(minDaysSinceRegistered);
        return customRes.findZeroOrderCustomers(registeredBefore)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getZeroOrderStats() {
        Map<String, Object> stats = new HashMap<>();
        long total3Days  = customRes.countZeroOrderCustomers(LocalDateTime.now().minusDays(3));
        long total7Days  = customRes.countZeroOrderCustomers(LocalDateTime.now().minusDays(7));
        long total30Days = customRes.countZeroOrderCustomers(LocalDateTime.now().minusDays(30));
        stats.put("registeredOver3Days",  total3Days);
        stats.put("registeredOver7Days",  total7Days);
        stats.put("registeredOver30Days", total30Days);
        stats.put("generatedAt", LocalDateTime.now());
        return stats;
    }
    private boolean isWeekend() {
        java.time.DayOfWeek day = LocalDateTime.now().getDayOfWeek();
        return day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY;
    }
}