// src\api\prices.api.js
import http from "./http";

export const pricesApi = {
  setVariantPrice(variantId, payload) {
    return http.post(`/api/prices/variants/${variantId}`, payload);
  },
  listByProduct(productId) {
    return http.get(`/api/prices/products/${productId}`);
  },
  listByProductForCustomer(productId, customerId) {
    return http.get(`/api/prices/products/${productId}/customer/${customerId}`);
  },
  updateHistory(id, payload) {
    return http.put(`/api/prices/history/${id}`, payload);
  },
  removeHistory(id) {
    return http.delete(`/api/prices/history/${id}`);
  },
  getEffective(variantId) {
    return http.get(`/api/prices/variants/${variantId}/effective`);
  },
  getEffectiveForCustomer(variantId, customerId) {
    return http.get(
      `/api/prices/variants/${variantId}/effective/customer/${customerId}`,
    );
  },
  getVariantHistory(variantId, page = 0, size = 20) {
    return http.get(`/api/prices/variants/${variantId}/history`, {
      params: { page, size },
    });
  },
  getCostWarning(variantId) {
    return http.get(`/api/prices/variants/${variantId}/cost-warning`);
  },
  getConflicts() {
    return http.get("/api/prices/conflicts");
  },
  calculateOrder(customerId, items) {
    return http.post("/api/prices/calculate-order", { customerId, items });
  },
  getDashboard() {
    return http.get("/api/prices/dashboard");
  },
  getVariantsByProduct(productId) {
    return http.get(`/api/products/${productId}/variants`);
  },
};
