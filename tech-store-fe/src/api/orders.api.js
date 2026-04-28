import http from "./http";

export const ordersApi = {
  create(payload) {
    // POST /api/orders (Customer)
    // Body: { customerId, paymentMethod, channel, notes, promotionCode?, items:[{ variantId, quantity }] }
    return http.post("/api/orders", payload);
  },

  listNew() {
    return http.get("/api/orders/new");
  },

  listProcessing() {
    return http.get("/api/orders/processing");
  },

  listPaid() {
    return http.get("/api/orders/paid");
  },

  listDelivered() {
    return http.get("/api/orders/delivered");
  },

  listShipping() {
    return http.get("/api/orders/shipping");
  },

  getByCustomer(customerId) {
    return http.get(`/api/orders/customer/${customerId}`);
  },

  getById(orderId) {
    return http.get(`/api/orders/${orderId}`);
  },

  update(orderId, payload) {
    // Body: { paymentMethod, notes }
    return http.put(`/api/orders/${orderId}`, payload);
  },

  cancel(orderId, reason) {
    return http.put(`/api/orders/${orderId}/cancel`, {
      reason: reason || "Customer cancelled",
    });
  },

  markAsShipping(orderId) {
    return http.put(`/api/orders/${orderId}/ship`);
  },

  markAsDelivered(orderId) {
    return http.put(`/api/orders/${orderId}/deliver`);
  },

  markAsProcessing(orderId) {
    return http.put(`/api/orders/${orderId}/process`);
  },

  remove(orderId) {
    return http.delete(`/api/orders/${orderId}`);
  },

  getMyOrders() {
    return http.get("/api/orders/my-orders");
  },

  getByDate(from, to) {
    return http.get("/api/orders/by-date", { params: { from, to } });
  },

  filter(customerId, from, to, channel, status) {
    return http.get("/api/orders/filter", {
      params: { customerId, from, to, channel, status },
    });
  },

  getRevenueByCustomer() {
    return http.get("/api/orders/revenue-by-customer");
  },

  getOrderDetail(orderId) {
    return http.get(`/api/orders/${orderId}`);
  },

  // Tìm đơn theo mã orderNumber (ORD-xxx)
  // Backend cần có endpoint: GET /api/orders/number/{orderNumber}
  getByOrderNumber(orderNumber) {
    return http.get(`/api/orders/number/${orderNumber}`);
  },
  // Tìm đơn theo serial number của sản phẩm
  findBySerial(serialNumber) {
    return http.get(`/api/orders/by-serial/${serialNumber}`);
  },

  // ✅ THÊM MỚI: validate mã giảm giá trước khi tạo đơn
  // Trả về: { valid, discount, minOrder, message }
  validatePromoCode(code, orderTotal) {
    return http.get("/api/promotions/validate", {
      params: { code, orderTotal },
    });
  },

  assignSerial(orderId, itemId, serialNumber) {
    return http.post(`/api/orders/${orderId}/items/${itemId}/serials/assign`, {
      serialNumber,
    });
  },
  suggestVouchers(customerId, subtotal) {
  return http.get("/api/orders/suggest-vouchers", {
    params: { customerId, subtotal },
  });
},

  removeSerial(orderId, itemId, serialNumber) {
    return http.delete(
      `/api/orders/${orderId}/items/${itemId}/serials/remove`,
      {
        params: { serialNumber },
      },
    );
  },
};
