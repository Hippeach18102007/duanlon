import http from "./http";

export const usersApi = {
  list() {
    return http.get("/api/auth/users");
  },
  add(payload) {
    // Body: { username, email, password, role }
    return http.post("/api/auth/users/add", payload);
  },
  update(id, payload) {
    // PUT /api/auth/users/update?id=<id>
    return http.put("/api/auth/users/update", payload, { params: { id } });
  },
  remove(id) {
    // DELETE /api/auth/users/delete?id=<id>
    return http.delete("/api/auth/users/delete", { params: { id } });
  },

    // ✅ Thêm: cập nhật role riêng
  updateRole(id, role) {
    // PUT /api/auth/users/update/role?id=<id>
    // Body: { role: "ADMIN" }
    return http.put("/api/auth/users/update/role", { role }, { params: { id } });
  },
};
