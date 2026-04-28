package com.retailmanagement.service;

import com.retailmanagement.entity.Customer;
import com.retailmanagement.entity.CustomerType;
import com.retailmanagement.entity.SpinWheelHistory;
import com.retailmanagement.repository.CustomRes;
import com.retailmanagement.repository.SpinWheelHistoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpinWheelService {

    private final SpinWheelHistoryRepository spinWheelHistoryRepository;
    private final CustomRes customerRepository;

    private static final Map<BigDecimal, Integer> DISCOUNT_WEIGHTS = new LinkedHashMap<>();
    static {
        DISCOUNT_WEIGHTS.put(BigDecimal.valueOf(1.00), 30);  // 1%  - 30% chance
        DISCOUNT_WEIGHTS.put(BigDecimal.valueOf(2.00), 25);  // 2%  - 25% chance
        DISCOUNT_WEIGHTS.put(BigDecimal.valueOf(3.00), 20);  // 3%  - 20% chance
        DISCOUNT_WEIGHTS.put(BigDecimal.valueOf(5.00), 15);  // 5%  - 15% chance
        DISCOUNT_WEIGHTS.put(BigDecimal.valueOf(7.00), 10);  // 7%  - 10% chance
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private LocalDate getWeekStartDate() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private BigDecimal generateRandomDiscount() {
        int totalWeight = DISCOUNT_WEIGHTS.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = new Random().nextInt(totalWeight);
        int cumulative  = 0;

        for (Map.Entry<BigDecimal, Integer> entry : DISCOUNT_WEIGHTS.entrySet()) {
            cumulative += entry.getValue();
            if (randomValue < cumulative) return entry.getKey();
        }

        return BigDecimal.valueOf(1.00); // fallback
    }

    // ================================================================
    // GET SPIN STATUS
    // ================================================================

    public Map<String, Object> getSpinStatus(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Map<String, Object> status = new HashMap<>();
        boolean isVip = customer.getCustomerType() == CustomerType.VIP;
        status.put("isVip", isVip);

        if (!isVip) {
            status.put("canSpin", false);
            status.put("message", "Chỉ khách hàng VIP mới được quay thưởng");
            return status;
        }

        LocalDate weekStart = getWeekStartDate();
        Optional<SpinWheelHistory> thisWeekSpin =
                spinWheelHistoryRepository.findByCustomerIdAndWeekStartDate(customerId, weekStart);

        if (thisWeekSpin.isPresent()) {
            SpinWheelHistory spin = thisWeekSpin.get();
            status.put("canSpin", false);
            status.put("message", "Bạn đã quay thưởng tuần này");
            status.put("lastSpin", Map.of(
                    "spunAt",        spin.getSpunAt(),
                    "discountBonus", spin.getDiscountBonus(),
                    "expiresAt",     spin.getExpiresAt()
            ));

            if (!spin.getIsUsed() && spin.getExpiresAt().isAfter(LocalDateTime.now())) {
                status.put("activeBonus", spin.getDiscountBonus());
            }
        } else {
            status.put("canSpin", true);
            status.put("message", "Bạn có thể quay thưởng tuần này!");
        }

        Optional<SpinWheelHistory> activeBonus =
                spinWheelHistoryRepository.findMostRecentActiveBonus(customerId, LocalDateTime.now());

        if (activeBonus.isPresent()) {
            status.put("currentBonus",   activeBonus.get().getDiscountBonus());
            status.put("bonusExpiresAt", activeBonus.get().getExpiresAt());
        } else {
            status.put("currentBonus", BigDecimal.ZERO);
        }

        return status;
    }

    // ================================================================
    // SPIN
    // ================================================================

    @Transactional
    public Map<String, Object> spin(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getCustomerType() != CustomerType.VIP) {
            throw new RuntimeException("Only VIP customers can spin the wheel");
        }

        LocalDate weekStart = getWeekStartDate();
        Optional<SpinWheelHistory> existingSpin =
                spinWheelHistoryRepository.findByCustomerIdAndWeekStartDate(customerId, weekStart);

        if (existingSpin.isPresent()) {
            throw new RuntimeException("You have already spun this week");
        }

        BigDecimal discountBonus = generateRandomDiscount();
        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime expiresAt  = now.plusDays(7);

        SpinWheelHistory spinHistory = SpinWheelHistory.builder()
                .customer(customer)
                .discountBonus(discountBonus)
                .weekStartDate(weekStart)
                .spunAt(now)
                .expiresAt(expiresAt)
                .isUsed(false)
                .build();

        spinWheelHistoryRepository.save(spinHistory);

        customerRepository.updateSpinBonus(customerId, discountBonus);

        Map<String, Object> result = new HashMap<>();
        result.put("success",       true);
        result.put("discountBonus", discountBonus);
        result.put("expiresAt",     expiresAt);
        result.put("message",       String.format("Chúc mừng! Bạn nhận được %.0f%% giảm giá bổ sung", discountBonus));
        return result;
    }

    // ================================================================
    // GET SPIN HISTORY
    // ================================================================

    public List<Map<String, Object>> getSpinHistory(Integer customerId) {
        List<SpinWheelHistory> history =
                spinWheelHistoryRepository.findByCustomerIdOrderBySpunAtDesc(customerId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SpinWheelHistory spin : history) {
            Map<String, Object> item = new HashMap<>();
            item.put("id",            spin.getId());
            item.put("discountBonus", spin.getDiscountBonus());
            item.put("spunAt",        spin.getSpunAt());
            item.put("expiresAt",     spin.getExpiresAt());
            item.put("isUsed",        spin.getIsUsed());
            item.put("usedOrderId",   spin.getUsedOrderId());
            item.put("weekStartDate", spin.getWeekStartDate());

            if (spin.getIsUsed()) {
                item.put("status", "Đã sử dụng");
            } else if (spin.getExpiresAt().isBefore(LocalDateTime.now())) {
                item.put("status", "Đã hết hạn");
            } else {
                item.put("status", "Đang hoạt động");
            }

            result.add(item);
        }

        return result;
    }

    // ================================================================
    // USE BONUS — gọi từ OrderService khi tạo đơn
    // ================================================================

    @Transactional
    public void useBonus(Integer customerId, Long orderId, Long spinHistoryId) {
        Customer customer = customerRepository.findByIdWithLock(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getSpinDiscountBonus() == null ||
                customer.getSpinDiscountBonus().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Optional<SpinWheelHistory> activeBonus;

        if (spinHistoryId != null) {
            // Khách tự chọn bonus cụ thể
            activeBonus = spinWheelHistoryRepository.findById(spinHistoryId)
                    .filter(s -> !s.getIsUsed()
                            && s.getExpiresAt().isAfter(LocalDateTime.now())
                            && s.getCustomer().getId().equals(customerId));
        } else {
            // Chỉ có 1 bonus → hệ thống tự lấy
            activeBonus = spinWheelHistoryRepository
                    .findMostRecentActiveBonus(customerId, LocalDateTime.now());
        }

        if (activeBonus.isPresent()) {
            SpinWheelHistory bonus = activeBonus.get();
            bonus.setIsUsed(true);
            bonus.setUsedOrderId(orderId);
            spinWheelHistoryRepository.save(bonus);
            customerRepository.updateSpinBonus(customerId, BigDecimal.ZERO);
        }
    }

    // ================================================================
    // RESTORE BONUS — gọi từ OrderService khi hủy đơn
    // ================================================================

    @Transactional
    public void restoreBonus(Long orderId) {
        Optional<SpinWheelHistory> spinOpt =
                spinWheelHistoryRepository.findByUsedOrderId(orderId);

        if (spinOpt.isEmpty()) return;

        SpinWheelHistory spin = spinOpt.get();

        // Chỉ restore nếu đang isUsed = true và chưa hết hạn
        if (!spin.getIsUsed() || spin.getExpiresAt().isBefore(LocalDateTime.now())) return;

        // Lock customer trước khi ghi
        Customer customer = customerRepository.findByIdWithLock(spin.getCustomer().getId())
                .orElseThrow();

        spin.setIsUsed(false);
        spin.setUsedOrderId(null);
        spinWheelHistoryRepository.save(spin);

        customerRepository.updateSpinBonus(spin.getCustomer().getId(), spin.getDiscountBonus());
    }

    // ================================================================
    // ✅ GET EXPIRING BONUSES — gọi từ NotificationService
    // Trả về danh sách bonus chưa dùng sẽ hết hạn trong withinHours giờ tới
    // ================================================================

    public List<Map<String, Object>> getExpiringBonuses(int withinHours) {
        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(withinHours);

        List<SpinWheelHistory> expiring =
                spinWheelHistoryRepository.findExpiringUnusedBonuses(now, threshold);

        return expiring.stream().map(spin -> {
            Map<String, Object> item = new HashMap<>();
            item.put("customerId",    spin.getCustomer().getId());
            item.put("customerName",  spin.getCustomer().getName());
            item.put("customerEmail", spin.getCustomer().getEmail());
            item.put("discountBonus", spin.getDiscountBonus());
            item.put("expiresAt",     spin.getExpiresAt());
            item.put("hoursLeft",     Duration.between(now, spin.getExpiresAt()).toHours());
            return item;
        }).collect(Collectors.toList());
    }

    // ================================================================
    // GET PRIZES — hiển thị trên vòng quay
    // ================================================================

    public List<Map<String, Object>> getPrizeOptions() {
        List<Map<String, Object>> prizes = new ArrayList<>();
        for (Map.Entry<BigDecimal, Integer> entry : DISCOUNT_WEIGHTS.entrySet()) {
            Map<String, Object> prize = new HashMap<>();
            prize.put("discount", entry.getKey());
            prize.put("weight",   entry.getValue());
            prize.put("label",    String.format("%.0f%%", entry.getKey()));
            prizes.add(prize);
        }
        return prizes;
    }

    @Transactional
    public void restoreBonusByOrder(Long orderId) {

        Optional<SpinWheelHistory> spinOpt =
                spinWheelHistoryRepository.findByUsedOrderId(orderId);

        if (spinOpt.isEmpty()) return;

        SpinWheelHistory spinHistory = spinOpt.get();

        // ✅ ĐÃ restore rồi → STOP
        if (!spinHistory.getIsUsed()) {
            return;
        }

        // ✅ Bonus hết hạn → không restore
        if (spinHistory.getExpiresAt().isBefore(LocalDateTime.now())) {
            return;
        }

        // lock customer tránh race condition
        Customer customer = customerRepository
                .findByIdWithLock(spinHistory.getCustomer().getId())
                .orElseThrow();

        spinHistory.setIsUsed(false);
        spinHistory.setUsedOrderId(null);

        spinWheelHistoryRepository.save(spinHistory);

        customerRepository.updateSpinBonus(
                customer.getId(),
                spinHistory.getDiscountBonus()
        );
    }
}