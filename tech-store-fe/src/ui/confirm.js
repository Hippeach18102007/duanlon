import { ElMessageBox } from "element-plus";

export async function confirmModal(
  message,
  title = "Confirm",
  okText = "OK",
  danger = true
) {
  try {
    await ElMessageBox.confirm(
      String(message ?? ""),
      String(title ?? "Confirm"),
      {
        confirmButtonText: String(okText ?? "OK"),
        cancelButtonText: "Cancel",
        type: danger ? "warning" : "info",
        distinguishCancelAndClose: true,
      }
    );
    return true;
  } catch {
    return false;
  }
}
