package com.retailmanagement.controller;

import com.retailmanagement.dto.response.CustomerBirthdayResponse;
import com.retailmanagement.dto.response.NotificationResponse;
import com.retailmanagement.entity.Customer;
import com.retailmanagement.entity.Notification;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth/admin/birthdays")
@RequiredArgsConstructor
public class BirthdayController {

    private final CustomRes customerRepository;
    private final NotificationService notificationService;

    /**
     * Lấy khách hàng có sinh nhật hôm nay
     */
    @GetMapping("/today")
    public ResponseEntity<List<CustomerBirthdayResponse>> getTodayBirthdays() {
        List<Customer> customers = customerRepository.findCustomersWithBirthdayToday();
        List<CustomerBirthdayResponse> response = customers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy khách hàng có sinh nhật trong tháng
     */
    @GetMapping("/month/{month}")
    public ResponseEntity<List<CustomerBirthdayResponse>> getBirthdaysByMonth(@PathVariable int month) {
        List<Customer> customers = customerRepository.findCustomersWithBirthdayInMonth(month);
        List<CustomerBirthdayResponse> response = customers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thống kê sinh nhật theo tháng
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<Integer, Long> monthlyCount = new HashMap<>();

        for (int month = 1; month <= 12; month++) {
            long count = customerRepository.countCustomersWithBirthdayInMonth(month);
            monthlyCount.put(month, count);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("monthlyCount", monthlyCount);
        stats.put("totalCustomers", customerRepository.count());

        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy sinh nhật sắp tới trong N ngày
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<CustomerBirthdayResponse>> getUpcomingBirthdays(
            @RequestParam(defaultValue = "7") int days) {

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        List<Customer> customers = customerRepository.findCustomersWithBirthdayBetween(
                today.getMonthValue(),
                today.getDayOfMonth(),
                endDate.getMonthValue(),
                endDate.getDayOfMonth()
        );

        List<CustomerBirthdayResponse> response = customers.stream()
                .map(this::mapToResponse)
                .filter(c -> c.getDaysUntilBirthday() <= days)
                .sorted(Comparator.comparing(CustomerBirthdayResponse::getDaysUntilBirthday))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gửi lời chúc sinh nhật từ admin
     */
    @PostMapping("/send-greeting/{customerId}")
    public ResponseEntity<Map<String, String>> sendGreeting(
            @PathVariable Integer customerId,
            @RequestBody Map<String, String> payload) {

        String message = payload.get("message");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Nội dung lời chúc không được để trống"));
        }

        try {
            notificationService.sendCustomBirthdayGreeting(customerId, message);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Đã gửi lời chúc sinh nhật thành công!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Lấy lịch sử thông báo sinh nhật đã gửi
     */
    @GetMapping("/notification-history")
    public ResponseEntity<List<NotificationResponse>> getNotificationHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<Notification> notifications;

        if (from != null && to != null) {
            notifications = notificationService.getBirthdayNotificationHistory(from, to);
        } else {
            notifications = notificationService.getBirthdayNotificationHistory();
        }

        List<NotificationResponse> response = notifications.stream()
                .map(this::mapNotificationToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ==================== Helper Methods ====================

    /**
     * Map Customer entity sang CustomerBirthdayResponse DTO
     */
    private CustomerBirthdayResponse mapToResponse(Customer customer) {
        LocalDate birthDate = customer.getDateOfBirth();
        LocalDate today = LocalDate.now();

        int age = Period.between(birthDate, today).getYears();

        LocalDate nextBirthday = LocalDate.of(today.getYear(), birthDate.getMonth(), birthDate.getDayOfMonth());
        if (nextBirthday.isBefore(today)) {
            nextBirthday = nextBirthday.plusYears(1);
        }

        long daysUntil = ChronoUnit.DAYS.between(today, nextBirthday);
        boolean isToday = daysUntil == 0;

        return CustomerBirthdayResponse.builder()
                .id(Long.valueOf(customer.getId()))
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .dateOfBirth(birthDate)
                .birthdayDisplay(String.format("%02d/%02d/%d",
                        birthDate.getDayOfMonth(),
                        birthDate.getMonthValue(),
                        birthDate.getYear()))
                .birthMonth(birthDate.getMonthValue())
                .birthDay(birthDate.getDayOfMonth())
                .age(age)
                .daysUntilBirthday(daysUntil)
                .isBirthdayToday(isToday)
                .customerType(customer.getCustomerType())
                .vipTier(customer.getVipTier())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .totalSpent(customer.getTotalSpent())
                .build();
    }

    /**
     * Map Notification entity sang NotificationResponse DTO
     */
    private NotificationResponse mapNotificationToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .customerId(notification.getCustomer().getId())
                .customerName(notification.getCustomer().getName())
                .customerEmail(notification.getCustomer().getEmail())
                .type(notification.getType())
                .typeDisplay(notification.getType().getDisplayName())
                .icon(notification.getType().getIcon())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}