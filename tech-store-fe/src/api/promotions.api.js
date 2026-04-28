import http from "./http";

export const promotionsApi = {
  create(payload) {
    return http.post("/api/promotions", payload);
  },
  list(activeOnly = false) {
    return http.get("/api/promotions", {
      params: { activeOnly: !!activeOnly },
    });
  },
  getById(id) {
    return http.get(`/api/promotions/${id}`);
  },
  update(id, payload) {
    return http.put(`/api/promotions/${id}`, payload);
  },
  remove(id) {
    return http.delete(`/api/promotions/${id}`);
  },
  getConflicts() {
    return http.get("/api/promotions/conflicts");
  },
  getExpiring(withinDays = 3) {
    return http.get("/api/promotions/expiring", { params: { withinDays } });
  },
  validate(code, orderTotal) {
    return http.get("/api/promotions/validate", {
      params: { code, orderTotal },
    });
  },
  getReport(period = "month") {
    return http.get("/api/promotions/report", { params: { period } });
  },
  getActiveReport() {
    return http.get("/api/reports/promotions/active");
  },
  getConflictsReport() {
    return http.get("/api/reports/promotions/conflicts");
  },
  getSummaryReport(period = "month") {
    return http.get("/api/reports/promotions/summary", { params: { period } });
  },
  getRedemptionDetails(id) {
    return http.get(`/api/promotions/${id}/redemption-details`);
  },

  /**
   * NEW: Get available promotions for current logged-in customer.
   * Returns only promotions that are active, within date range,
   * not yet used by this customer, and matching customer type/tier.
   */
  getAvailable(customerId) {
  return http.get("/api/promotions/available", { params: { customerId } });
},
getAvailableForCustomer(customerId) {
  return http.get(`/api/promotions/available-for-customer/${customerId}`);
},
};