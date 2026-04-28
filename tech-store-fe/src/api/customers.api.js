import http from "./http";

export const customersApi = {
  create(payload) {
    return http.post("/api/auth/customers", payload);
  },
  listAll() {
    return http.get("/api/auth/customers");
  },
  listByType(type) {
    return http.get(`/api/auth/customers/type/${type}`);
  },
  update(id, payload) {
    return http.put(`/api/auth/customers/${id}`, payload);
  },
  remove(id) {
    return http.delete(`/api/auth/customers/${id}`);
  },
  getProfile() {
    return http.get("/api/auth/customers/me");
  },
  listActiveLast30Days() {
    return http.get("/api/auth/customers/active-last-30-days");
  },
   getLoyaltyHistory(customerId) {
    return http.get(`/api/auth/customers/${customerId}/loyalty-history`);
  },
  getTierHistory(customerId) {
    return http.get(`/api/auth/customers/${customerId}/tier-history`);
  },
  listByPointsRange(minPoints, maxPoints) {
    return http.get(`/api/auth/customers/points?min=${minPoints}&max=${maxPoints}`);
  },
  listByVipTier(tier) {
    return http.get(`/api/auth/customers/vip-tier/${tier}`);
  },
   listBySpendingRange(minSpent, maxSpent) {
    return http.get(`/api/auth/customers/spending?min=${minSpent}&max=${maxSpent}`);
  },
  
  listTopSpenders(limit = 10) {
    return http.get(`/api/auth/customers/top-spenders?limit=${limit}`);
  },
  
  listTopSpendersByVipTier(tier, limit = 10) {
    return http.get(`/api/auth/customers/vip-tier/${tier}/top-spenders?limit=${limit}`);
  },
  
  getSpendingStatistics() {
    return http.get(`/api/auth/customers/spending-stats`);
  },

  // Fixed duplicate method
  listByVipTierAndPoints(tier, minPoints, maxPoints) {
    return http.get(`/api/auth/customers/vip-tier/${tier}/points?min=${minPoints}&max=${maxPoints}`);
  },
  getSpinWheelStatus() {
    return http.get("/api/auth/customers/spin-wheel/status");
  },
  
  spinWheel() {
    return http.post("/api/auth/customers/spin-wheel/spin");
  },
  
  getSpinWheelHistory() {
    return http.get("/api/auth/customers/spin-wheel/history");
  },
  
  getSpinWheelPrizes() {
    return http.get("/api/auth/customers/spin-wheel/prizes");
  },
  // ✅ SPIN WHEEL
  getSpinWheelStatus() {
    return http.get("/api/auth/customers/spin-wheel/status");
  },

  spin() {
    return http.post("/api/auth/customers/spin-wheel/spin");
  },

  getSpinHistory() {
    return http.get("/api/auth/customers/spin-wheel/history");
  },

  getSpinPrizes() {
    return http.get("/api/auth/customers/spin-wheel/prizes");
  },
  getTierProgress() {
    return http.get("/api/auth/tier-progress/me");
  },
  checkTierProgressWithCart(cartTotal) {
    return http.post("/api/auth/tier-progress/check-cart", { cartTotal });
  },
  getCustomerTierProgress(customerId) {
    return http.get(`/api/auth/tier-progress/customer/${customerId}`);
  },
    getPayments(customerId, includeDeleted = false) {
  if (includeDeleted) {
    return http.get(`/api/auth/customers/${customerId}/payments/all`);
  }
  return http.get(`/api/auth/customers/${customerId}/payments`);
},
  getPaymentsIncludingDeleted(customerId) {
    return http.get(`/api/auth/customers/${customerId}/payments/all`);
  },
  getDeletedPayments(customerId) {
    return http.get(`/api/auth/customers/${customerId}/payments/deleted`);
  },
  getPaymentDetail(paymentId) {
  return http.get(`/api/auth/payments/${paymentId}/detail`);
},

// ✅ PAYMENT ACTIONS - Soft delete and restore
softDeletePayment(paymentId) {
  return http.delete(`/api/auth/payments/${paymentId}/soft-delete`);
},
restorePayment(paymentId) {
  return http.post(`/api/auth/payments/${paymentId}/restore`);
},
listInactiveTransaction(days = 30) {
  return http.get(`/api/auth/customers/inactive-transaction?days=${days}`);
},
updateVipNote(id, vipNote) {
  return http.patch(`/api/auth/customers/${id}/vip-note`, { vipNote });
},
listZeroOrder(minDays = 3) {
  return http.get(`/api/auth/customers/zero-order?minDays=${minDays}`);
},

/**
 * Thống kê nhanh zero-order theo các mốc 3/7/30 ngày
 */
getZeroOrderStats() {
  return http.get("/api/auth/customers/zero-order/stats");
},
getPromotionHistory(customerId, type = null) {
    const params = type ? `?type=${type}` : "";
    return http.get(`/api/auth/customers/${customerId}/promotion-history${params}`);
  },
  // Thêm vào cuối object customersApi, trước dấu }

// ── LOYALTY SUMMARY ──────────────────────────────────────────────────────────
getLoyaltyWeeklySummaryMe(weeks = 12) {
  return http.get(`/api/auth/loyalty/summary/me/weekly?weeks=${weeks}`);
},
getLoyaltyMonthlySummaryMe(months = 12) {
  return http.get(`/api/auth/loyalty/summary/me/monthly?months=${months}`);
},
getLoyaltyWeeklySummaryByCustomer(customerId, weeks = 12) {
  return http.get(`/api/auth/loyalty/summary/customer/${customerId}/weekly?weeks=${weeks}`);
},
getLoyaltyMonthlySummaryByCustomer(customerId, months = 12) {
  return http.get(`/api/auth/loyalty/summary/customer/${customerId}/monthly?months=${months}`);
},
// Admin only
getLoyaltyWeeklySummaryAdmin(weeks = 12) {
  return http.get(`/api/auth/loyalty/summary/admin/weekly?weeks=${weeks}`);
},
getLoyaltyMonthlySummaryAdmin(months = 12) {
  return http.get(`/api/auth/loyalty/summary/admin/monthly?months=${months}`);
}, 
 getMyDashboard() {
    return http.get("/api/auth/customers/my/dashboard");
  },
  /**
   * Chỉ lấy summary nhẹ: điểm, hạng, % giảm giá, spin bonus.
   * Dùng cho header / widget nhỏ.
   */
  getMySummary() {
    return http.get("/api/auth/customers/my/summary");
  },
  /**
   * Tiến trình VIP: tier hiện tại, % thanh progress, milestones.
   * Dùng cho trang "Ưu đãi thành viên".
   */
  getMyVipJourney() {
    return http.get("/api/auth/customers/my/vip-journey");
  },
   triggerYearEndReset() {
    return http.post("/api/auth/customers/loyalty/reset/year-end");
  },
  triggerMonthlyReset() {
    return http.post("/api/auth/customers/loyalty/reset/monthly");
  },
  
};
