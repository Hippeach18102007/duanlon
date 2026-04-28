import http from "./http";

export const serialsApi = {
  list(params) {
    return http.get("/api/products/variants/serials/all", { params });
  },
  updateStatus(id, status) {
    return http.put(`/api/products/variants/serials/${id}/status`, null, { params: { status } });
  }
};