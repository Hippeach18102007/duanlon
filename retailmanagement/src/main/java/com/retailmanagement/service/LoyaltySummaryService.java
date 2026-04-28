package com.retailmanagement.service;

import com.retailmanagement.dto.response.LoyaltySummaryResponse;
import com.retailmanagement.dto.response.LoyaltySummaryResponse.CustomerPointSummary;
import com.retailmanagement.repository.LoyaltyLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoyaltySummaryService {
private final CustomerService customerService;
    private final LoyaltyLedgerRepository ledgerRepository;

    // ═══════════════════════════════════════════════════════════════════
    // CUSTOMER: Tổng hợp điểm của 1 khách hàng
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Khách hàng xem tổng hợp điểm của bản thân theo TUẦN
     *
     * @param customerId ID khách hàng
     * @param weeksBack  Lấy bao nhiêu tuần gần đây (mặc định 12)
     */
    public List<LoyaltySummaryResponse> getWeeklySummaryForCustomer(
            Integer customerId, int weeksBack) {

        Instant to   = Instant.now();
        Instant from = LocalDate.now()
                .minusWeeks(weeksBack)
                .with(java.time.DayOfWeek.MONDAY)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<Object[]> rows = ledgerRepository.findWeeklySummaryByCustomer(customerId, from, to);
        return rows.stream()
                .map(r -> buildWeeklyResponse(r, false))
                .collect(Collectors.toList());
    }

    /**
     * Khách hàng xem tổng hợp điểm của bản thân theo THÁNG
     *
     * @param customerId   ID khách hàng
     * @param monthsBack   Lấy bao nhiêu tháng gần đây (mặc định 12)
     */
    public List<LoyaltySummaryResponse> getMonthlySummaryForCustomer(
            Integer customerId, int monthsBack) {

        Instant to   = Instant.now();
        Instant from = LocalDate.now()
                .minusMonths(monthsBack)
                .withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<Object[]> rows = ledgerRepository.findMonthlySummaryByCustomer(customerId, from, to);
        return rows.stream()
                .map(r -> buildMonthlyResponse(r, false))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // ADMIN: Tổng hợp điểm toàn hệ thống
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Admin xem tổng hợp điểm TẤT CẢ khách hàng theo TUẦN
     * Kết quả: mỗi phần tử là 1 tuần, có breakdown từng khách hàng
     */
    public List<LoyaltySummaryResponse> getWeeklySummaryForAdmin(
            int weeksBack) {

        Instant to   = Instant.now();
        Instant from = LocalDate.now()
                .minusWeeks(weeksBack)
                .with(java.time.DayOfWeek.MONDAY)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<Object[]> rows = ledgerRepository.findWeeklySummaryAllCustomers(from, to);
        return groupAndBuildWeekly(rows);
    }

    /**
     * Admin xem tổng hợp điểm TẤT CẢ khách hàng theo THÁNG
     */
    public List<LoyaltySummaryResponse> getMonthlySummaryForAdmin(int monthsBack) {

        Instant to   = Instant.now();
        Instant from = LocalDate.now()
                .minusMonths(monthsBack)
                .withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        List<Object[]> rows = ledgerRepository.findMonthlySummaryAllCustomers(from, to);
        return groupAndBuildMonthly(rows);
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER: Build response từ row (customer-level, không có breakdown)
    // ═══════════════════════════════════════════════════════════════════

    private LoyaltySummaryResponse buildWeeklyResponse(Object[] r, boolean includeBreakdown) {
        int yearWeek   = toInt(r[0]);          // e.g. 202503
        int year       = yearWeek / 100;
        int week       = yearWeek % 100;

        int earnSum    = toInt(r[includeBreakdown ? 5 : 1]);
        int deductSum  = toInt(r[includeBreakdown ? 6 : 2]);
        long txCount   = toLong(r[includeBreakdown ? 7 : 3]);

        LocalDate weekStart = LocalDate.ofYearDay(year, 1)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        return LoyaltySummaryResponse.builder()
                .period(year + "-W" + String.format("%02d", week))
                .periodLabel("Tuần " + week + ", " + year)
                .periodStart(weekStart)
                .periodEnd(weekEnd)
                .totalPointsEarned(earnSum)
                .totalPointsDeducted(deductSum)
                .netPoints(earnSum - deductSum)
                .totalTransactions(txCount)
                .build();
    }

    private LoyaltySummaryResponse buildMonthlyResponse(Object[] r, boolean includeBreakdown) {
        int yearMonth  = toInt(r[0]);          // e.g. 202501
        int year       = yearMonth / 100;
        int month      = yearMonth % 100;

        int earnSum    = toInt(r[includeBreakdown ? 5 : 1]);
        int deductSum  = toInt(r[includeBreakdown ? 6 : 2]);
        long txCount   = toLong(r[includeBreakdown ? 7 : 3]);

        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd   = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        return LoyaltySummaryResponse.builder()
                .period(year + "-" + String.format("%02d", month))
                .periodLabel("Tháng " + month + ", " + year)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .totalPointsEarned(earnSum)
                .totalPointsDeducted(deductSum)
                .netPoints(earnSum - deductSum)
                .totalTransactions(txCount)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER: Group rows theo period và gắn customerBreakdown (Admin)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * rows[]: [yearWeek, customerId, customerName, customerEmail, vipTier,
     *          earnSum, deductSum, txCount]
     */
    private List<LoyaltySummaryResponse> groupAndBuildWeekly(List<Object[]> rows) {
        // Group by yearWeek
        Map<Integer, List<Object[]>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> toInt(r[0])));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    int yearWeek = entry.getKey();
                    List<Object[]> periodRows = entry.getValue();

                    // Build header từ row đầu tiên (chỉ lấy period info)
                    LoyaltySummaryResponse header = buildWeeklyResponse(periodRows.get(0), true);

                    // Tổng hợp aggregate
                    int totalEarned   = periodRows.stream().mapToInt(r -> toInt(r[5])).sum();
                    int totalDeducted = periodRows.stream().mapToInt(r -> toInt(r[6])).sum();
                    long totalTx      = periodRows.stream().mapToLong(r -> toLong(r[7])).sum();

                    // Breakdown từng khách hàng
                    List<CustomerPointSummary> breakdown = periodRows.stream()
                            .map(r -> CustomerPointSummary.builder()
                                    .customerId(toInt(r[1]))
                                    .customerName((String) r[2])
                                    .customerEmail((String) r[3])
                                    .vipTier(r[4] != null ? r[4].toString() : "Member")
                                    .pointsEarned(toInt(r[5]))
                                    .pointsDeducted(toInt(r[6]))
                                    .netPoints(toInt(r[5]) - toInt(r[6]))
                                    .transactionCount(toLong(r[7]))
                                    .build())
                            .collect(Collectors.toList());

                    return header.toBuilder()
                            .totalPointsEarned(totalEarned)
                            .totalPointsDeducted(totalDeducted)
                            .netPoints(totalEarned - totalDeducted)
                            .totalTransactions(totalTx)
                            .customerBreakdown(breakdown)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<LoyaltySummaryResponse> groupAndBuildMonthly(List<Object[]> rows) {
        Map<Integer, List<Object[]>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> toInt(r[0])));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Object[]> periodRows = entry.getValue();

                    LoyaltySummaryResponse header = buildMonthlyResponse(periodRows.get(0), true);

                    int totalEarned   = periodRows.stream().mapToInt(r -> toInt(r[5])).sum();
                    int totalDeducted = periodRows.stream().mapToInt(r -> toInt(r[6])).sum();
                    long totalTx      = periodRows.stream().mapToLong(r -> toLong(r[7])).sum();

                    List<CustomerPointSummary> breakdown = periodRows.stream()
                            .map(r -> CustomerPointSummary.builder()
                                    .customerId(toInt(r[1]))
                                    .customerName((String) r[2])
                                    .customerEmail((String) r[3])
                                    .vipTier(r[4] != null ? r[4].toString() : "Member")
                                    .pointsEarned(toInt(r[5]))
                                    .pointsDeducted(toInt(r[6]))
                                    .netPoints(toInt(r[5]) - toInt(r[6]))
                                    .transactionCount(toLong(r[7]))
                                    .build())
                            .collect(Collectors.toList());

                    return header.toBuilder()
                            .totalPointsEarned(totalEarned)
                            .totalPointsDeducted(totalDeducted)
                            .netPoints(totalEarned - totalDeducted)
                            .totalTransactions(totalTx)
                            .customerBreakdown(breakdown)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER: Type casting an toàn
    // ═══════════════════════════════════════════════════════════════════

    private int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }
}