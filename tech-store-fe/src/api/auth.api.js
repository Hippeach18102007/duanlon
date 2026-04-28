import http from "./http";

export const authApi = {
  register(payload) {
    return http.post("/api/auth/register", payload);
  },
  login(payload) {
    return http.post("/api/auth/login", payload);
  },
  testProtected(path = "/api/test/protected") {
    return http.get(path);
  },
   changePassword(payload) {
    return http.post("/api/auth/change-password", payload);
  },
  forgotPassword(email) {
    return http.post("/api/auth/forgot-password", { email });
  },
  resetPassword(email, otp, newPassword) {
    return http.post("/api/auth/reset-password", { email, otp, newPassword });
  }
};
