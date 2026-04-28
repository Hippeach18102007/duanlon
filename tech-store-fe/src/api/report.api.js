import http from "./http";

export default {

  revenueByChannel() {
    return http.get("/api/reports/revenue-by-channel");
  },

  revenueByDate() {
    return http.get("/api/reports/revenue-by-date");
  },

  ordersByStaff() {
    return http.get("/api/reports/orders-by-staff");
  },

  // Tổng hợp doanh thu theo trạng thái đơn hàng
  // Response: [{ status, orderCount, totalRevenue }]
  revenueByStatus() {
    return http.get("/api/reports/revenue-by-status");
  },

  // Thống kê trả hàng theo sản phẩm (loại trừ REJECTED)
  // Response: [{ productName, sku, variantName, returnCount, totalQty, totalRefund }]
  returnsByProduct() {
    return http.get("/api/reports/returns-by-product");
  },

  // ✅ THÊM MỚI: Doanh thu theo tuần
  // Response: [{ weekStart, revenue }]
  revenueByWeek() {
    return http.get("/api/reports/revenue-by-week");
  },

  // ✅ THÊM MỚI: Doanh thu theo tháng
  // Response: [{ monthStart, revenue }]
  revenueByMonth() {
    return http.get("/api/reports/revenue-by-month");
  },

  // ✅ THÊM MỚI: Top sản phẩm bán chạy (theo số lượng, DELIVERED)
  // Response: [{ productName, sku, variantName, totalQty, totalRevenue }]
  topSellingProducts() {
    return http.get("/api/reports/top-selling-products");
  },

};