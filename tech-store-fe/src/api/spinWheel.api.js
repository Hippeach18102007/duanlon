import http from "./http";

export const spinWheelApi = {
  /**
   * Lấy spin status của customer (canSpin, currentBonus, bonusExpiresAt, ...)
   * GET /api/spin-wheel/status/:customerId
   */
  getStatus(customerId) {
    return http.get(`/api/spin-wheel/status/${customerId}`);
  },

  /**
   * Thực hiện quay thưởng
   * POST /api/spin-wheel/spin/:customerId
   */
  spin(customerId) {
    return http.post(`/api/spin-wheel/spin/${customerId}`);
  },

  /**
   * Lịch sử quay thưởng của customer
   * GET /api/spin-wheel/history/:customerId
   */
  getHistory(customerId) {
    return http.get(`/api/spin-wheel/history/${customerId}`);
  },

  /**
   * Danh sách phần thưởng có thể nhận
   * GET /api/spin-wheel/prizes
   */
  getPrizes() {
    return http.get("/api/spin-wheel/prizes");
  },
  previewExpiringBonuses(hours = 24) {
    return http.get(`/api/spin-wheel/spin-expiry/preview?hours=${hours}`);
  }
};