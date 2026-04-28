import { ElNotification } from "element-plus";

function mapType(type) {
  const t = (type || "").toLowerCase();
  if (t === "success") return "success";
  if (t === "warning") return "warning";
  if (t === "danger" || t === "error") return "error";
  if (t === "info") return "info";
  return "info";
}

const defaultTitles = {
  success: "Thành công",
  warning: "Cảnh báo",
  error: "Lỗi",
  info: "Thông báo",
};

/**
 * Hiển thị thông báo góc trên bên phải bằng ElNotification.
 * @param {string} message - Nội dung thông báo
 * @param {string} type    - 'success' | 'warning' | 'error' | 'info'
 * @param {object} opts    - { title?: string, delay?: number, html?: boolean }
 */
export function toast(message, type = "success", opts = {}) {
  const resolvedType = mapType(type);
  ElNotification({
    title: opts?.title ?? defaultTitles[resolvedType],
    message: String(message ?? ""),
    type: resolvedType,
    duration: typeof opts?.delay === "number" ? opts.delay : 3000,
    showClose: true,
    position: "bottom-left",
    dangerouslyUseHTMLString: opts?.html === true,
  });
}
