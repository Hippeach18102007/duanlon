import http from "./http";

export const productsApi = {
  list(params = {}) {
    return http.get("/api/products", { params });
  },

  create(formData) {
    return http.post("/api/products", formData);
  },

  update(id, formData) {
    return http.put(`/api/products/${id}`, formData);
  },


  remove(id) {
    return http.delete(`/api/products/${id}`);
  },

  get(id) {
    return http.get(`/api/products/${id}`);
  },

  // ── Variants ──────────────────────────────────────────
  getVariants(productId) {
    return http.get(`/api/products/${productId}/variants`);
  },

  createVariant(productId, data) {
    return http.post(`/api/products/${productId}/variants`, data);
  },

  updateVariant(variantId, data) {
    return http.put(`/api/products/variants/${variantId}`, data);
  },

  deleteVariant(variantId) {
    return http.delete(`/api/products/variants/${variantId}`);
  },

  // ── Serials ───────────────────────────────────────────
  getSerials(variantId) {
    return http.get(`/api/products/variants/${variantId}/serials`);
  },

  addSerials(variantId, serialNumbers) {
    return http.post(`/api/products/variants/${variantId}/serials`, {
      serialNumbers,
    });
  },

  importVariantSerials(variantId, file) {
    const fd = new FormData();
    fd.append('file', file);
    return http.post(`/api/products/variants/${variantId}/serials/import`, fd, {
      transformRequest: [(data, headers) => {
        if (headers.delete) headers.delete('Content-Type');
        else delete headers['Content-Type'];
        return data;
      }]
    });
  },

  generateSerials(variantId, quantity = 1) {
    return http.post(
      `/api/products/variants/${variantId}/serials/generate`,
      null,
      { params: { quantity } }
    );
  },

  deleteSerial(serialId) {
    return http.delete(`/api/products/variants/serials/${serialId}`);
  },

  batchDelete(ids) {
    return http.post(`/api/products/batch-delete`, ids);
  },

  batchUpdate(payload) {
    return http.put(`/api/products/batch-update`, payload);
  },

  getHistory(productId) {
    return http.get(`/api/products/${productId}/history`);
  },

  getDashboardStats() {
    return http.get("/api/products/dashboard-stats");
  },

  exportReport() {
    return http.get("/api/reports/export/excel", {
      responseType: 'blob'
    });
  },
  importFromExcel(file) {
    const fd = new FormData();
    fd.append('file', file);
    return http.post('/api/products/import', fd);
  },

  downloadTemplate() {
    return http.get('/api/products/import/template', {
      responseType: 'blob'
    })
  },
};