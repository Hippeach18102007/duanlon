import http from "./http";

const BASE = "/api/auth/userLogins";

export const userLoginApi = {

  // ===== GET ALL =====
  list() {
    return http.get(BASE);
  },

  // ===== FILTER + PAGINATION =====
  filter(payload, page = 0, size = 10) {
    return http.post(
      `${BASE}/filter?page=${page}&size=${size}`,
      payload
    );
  },

  // ===== EXPORT CSV =====
  exportCsv(filter) {
    return http.post(`${BASE}/export`, filter, {
      responseType: "blob"
    });
  }

};