import axios from "axios";
import { clearLastAuthResponse, clearSession, getToken } from "../stores/auth";

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 30000,
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err?.response?.status;

    if (status === 401) {
      clearSession();
      clearLastAuthResponse();
      window.dispatchEvent(
        new CustomEvent("auth:logout", { detail: "401 Unauthorized" })
      );
    }

    if (status === 403) {
      // Dùng replace để không thêm vào history, tránh loop back
      window.location.replace("/403");
    }

    return Promise.reject(err);
  }
);

export default http;