// \src\api\payments.js
import http from "./http";

export const paymentsApi = {
  getVnPayUrl(orderId) {
    return http.get(`/api/auth/payments/vnpay-url/${orderId}`);
  },

  // Tạo payment mới
  create(payload) {
    return http.post("/api/auth/payments", payload);
  },

  // Lấy danh sách payments theo order
  getByOrder(orderId) {
    return http.get(`/api/auth/payments/order/${orderId}`);
  },

  // Lấy chi tiết payment
  getDetail(paymentId) {
    return http.get(`/api/auth/payments/${paymentId}/detail`);
  },

  // Lấy payment cơ bản
  getById(paymentId) {
    return http.get(`/api/auth/payments/${paymentId}`);
  },

  // Lấy tất cả payments
  getAll() {
    return http.get("/api/auth/payments");
  },

  // Hoàn tiền
  refund(paymentId) {
    return http.put(`/api/auth/payments/${paymentId}/refund`);
  },
  softDelete(paymentId) {
    return http.delete(`/api/auth/payments/${paymentId}/soft-delete`);
  },

  // Khôi phục payment đã xóa
  restore(paymentId) {
    return http.post(`/api/auth/payments/${paymentId}/restore`);
  },

  confirmVnpay(queryParams) {
  return http.get("/api/auth/payments/vnpay-return", { params: queryParams });
}
};