// FILE: src/api/vip-checkin.api.js
import http from "./http";

export const vipCheckInApi = {
  /**
   * Lấy trạng thái điểm danh của khách hàng hiện tại
   * GET /api/auth/vip-checkin/status
   */
  getStatus() {
    return http.get("/api/auth/vip-checkin/status");
  },

  /**
   * Thực hiện điểm danh hôm nay
   * POST /api/auth/vip-checkin/checkin
   */
  performCheckIn() {
    return http.post("/api/auth/vip-checkin/checkin");
  },

  /**
   * Nhận phần thưởng từ điểm danh
   * POST /api/auth/vip-checkin/claim/{checkInId}
   */
  claimReward(checkInId) {
    return http.post(`/api/auth/vip-checkin/claim/${checkInId}`);
  },

  /**
   * Lấy danh sách ưu đãi đang hoạt động
   * GET /api/auth/vip-checkin/active-rewards
   */
  getActiveRewards() {
    return http.get("/api/auth/vip-checkin/active-rewards");
  },
};