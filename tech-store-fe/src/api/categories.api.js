import http from "./http";

export const categoriesApi = {
  create(payload) {
    // Body: { name, description, imageUrl, displayOrder, isActive, parent?: { id } }
    return http.post("/api/categories", payload);
  },
  list(activeOnly = false) {
    // GET /api/categories?activeOnly=true|false
    return http.get("/api/categories", {
      params: { activeOnly: !!activeOnly },
    });
  },
};
