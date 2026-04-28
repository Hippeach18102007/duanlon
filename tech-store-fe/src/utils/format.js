export const formatVND = (v) =>
  v ? new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v) : "—";

// Đảm bảo chuỗi ngày tháng từ API luôn được hiểu là UTC nếu thiếu thông tin múi giờ
export const parseSafeDate = (d) => {
  if (!d) return null;
  if (d instanceof Date) return d;
  // Nếu là chuỗi và không có định dạng T/Z, chuyển sang ISO chuẩn để trình duyệt hiểu là UTC
  if (typeof d === "string" && !d.includes("T") && !d.includes("Z")) {
    return new Date(d.replace(" ", "T") + "Z");
  }
  return new Date(d);
};

export const formatDate = (d) => {
  const date = parseSafeDate(d);
  return date ? date.toLocaleDateString("vi-VN") : "—";
};

export const formatDateTime = (d) => {
  const date = parseSafeDate(d);
  return date ? date.toLocaleString("vi-VN") : "—";
};

export const initials = (name) =>
  name ? name.split(" ").slice(-2).map(n => n[0]).join("").toUpperCase() : "?";
