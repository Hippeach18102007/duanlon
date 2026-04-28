import http from "./http";

const BASE = "/api/auth/security-log";

export const securityLogApi = {

  // ===== SEARCH (ADVANCED FILTER + PAGINATION) =====
  search(payload, page = 0, size = 5, sortBy = "createdAt", sortDir = "desc") {
    return http.post(`${BASE}/search`, payload, {
      params: { page, size, sortBy, sortDir }
    });
  },

};