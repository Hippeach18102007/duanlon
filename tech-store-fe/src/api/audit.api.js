import http from "./http";

const BASE = "/api/auth/audit-log";

export const auditApi = {

  // ===== DASHBOARD =====
  dashboard() {
    return http.get(`${BASE}/dashboard`);
  },

  logSummary(type = "week") {
    return http.get(`${BASE}/log/summary`, {
      params: { type }
    });
  },

  // ===== GET ALL =====
  list() {
    return http.get(BASE);
  },

  // ===== FILTER =====
  filterByDay(day) {
    return http.get(`${BASE}/filter/day`, {
      params: { day }
    });
  },

  filterChangeRole() {
    return http.get(`${BASE}/filter/action/change-role`);
  },

  filterByUserId(id) {
    return http.get(`${BASE}/filter/user_id`, {
      params: { id }
    });
  },

  filterModuleCustomer() {
    return http.get(`${BASE}/filter/module/customer`);
  },

  filterThisWeek() {
    return http.get(`${BASE}/filter/this-week`);
  },

  filterByModule(module, page = 0, size = 10) {
    return http.get(`${BASE}/filter/module`, {
      params: { module, page, size }
    });
  },

  // ===== REPORT =====
  reportByModule() {
    return http.get(`${BASE}/report/module`);
  },

  reportByUser() {
    return http.get(`${BASE}/report/user`);
  },

  reportSummary() {
    return http.get(`${BASE}/report/summary`);
  },

  // ===== SEARCH ADVANCED =====
  search(payload, page = 0, size = 10, sortBy = "createdAt", sortDir = "DESC") {
    return http.post(`${BASE}/search`, payload, {
      params: { page, size, sortBy, sortDir }
    });
  },

  // ===== EXPORT CSV =====
  exportCsv(from, to) {
    return http.get(`${BASE}/export`, {
      params: { from, to },
      responseType: "blob"
    });
  },

  // ===== EXPORT CSV ADVANCED =====
  exportAdvanced(filter) {
    return http.post(`${BASE}/export/advanced`, filter, {
      responseType: "blob"
    });
  }
};