package com.retailmanagement.controller;

import com.retailmanagement.service.SpinWheelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spin-wheel")
@RequiredArgsConstructor
public class SpinWheelController {

    private final SpinWheelService spinWheelService;

    /**
     * GET /api/spin-wheel/status/{customerId}
     * Lấy trạng thái spin của customer (canSpin, currentBonus, bonusExpiresAt, ...)
     */
    @GetMapping("/status/{customerId}")
    public ResponseEntity<Map<String, Object>> getSpinStatus(@PathVariable Integer customerId) {
        Map<String, Object> status = spinWheelService.getSpinStatus(customerId);
        return ResponseEntity.ok(status);
    }

    /**
     * POST /api/spin-wheel/spin/{customerId}
     * Thực hiện quay thưởng
     */
    @PostMapping("/spin/{customerId}")
    public ResponseEntity<Map<String, Object>> spin(@PathVariable Integer customerId) {
        Map<String, Object> result = spinWheelService.spin(customerId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/spin-wheel/history/{customerId}
     * Lịch sử quay thưởng của customer
     */
    @GetMapping("/history/{customerId}")
    public ResponseEntity<List<Map<String, Object>>> getHistory(@PathVariable Integer customerId) {
        List<Map<String, Object>> history = spinWheelService.getSpinHistory(customerId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/spin-wheel/prizes
     * Danh sách các phần thưởng có thể nhận (để hiển thị trên vòng quay)
     */
    @GetMapping("/prizes")
    public ResponseEntity<List<Map<String, Object>>> getPrizes() {
        List<Map<String, Object>> prizes = spinWheelService.getPrizeOptions();
        return ResponseEntity.ok(prizes);
    }
    @GetMapping("/spin-expiry/preview")
    public ResponseEntity<?> previewExpiringBonuses(
            @RequestParam(defaultValue = "24") int hours) {
        List<Map<String, Object>> expiring = spinWheelService.getExpiringBonuses(hours);
        return ResponseEntity.ok(Map.of(
                "count", expiring.size(),
                "bonuses", expiring
        ));
    }
}