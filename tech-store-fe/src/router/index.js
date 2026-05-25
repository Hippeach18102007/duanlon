import { createRouter, createWebHistory } from "vue-router";
import { getRole, getToken } from "../stores/auth";

// ===== Customer =====
import CustomerHome from "../pages/customer/CustomerHome.vue";
import CustomerLogin from "../pages/customer/CustomerLogin.vue";
import CustomerRegister from "../pages/customer/CustomerRegister.vue";
import CustomerOrderCreate from "../pages/customer/OrderCreate.vue";
import CustomerOrderDetail from "../pages/customer/OrderDetail.vue";
import CustomerProfile from "../pages/customer/CustomerProfile.vue";
import CartPage from "../pages/customer/CartPage.vue";
import CustomerOrders from "../pages/customer/CustomerOrders.vue";
import PaymentSuccess from "../pages/customer/PaymentSuccess.vue";
import Spinwheel from "../pages/customer/Spinwheel.vue";
import ProductDetail from "../pages/customer/ProductDetail.vue";
import ForgotPassword from "../pages/customer/Forgotpassword.vue";
// ===== System (Admin) =====
import SystemLogin from "../pages/system/SystemLogin.vue";
import SystemShell from "../pages/system/SystemShell.vue";
import SystemDashboard from "../pages/system/SystemDashboard.vue";
import UserManager from "../pages/system/UserManager.vue";
import CustomerManager from "../pages/system/CustomerManager.vue";
import CategoryManager from "../pages/system/CategoryManager.vue";
import ProductManager from "../pages/system/ProductManager.vue";
import OrderFilter from "../pages/system/OrderFilter.vue";
import SystemOrderDetail from "../pages/system/OrderDetail.vue";
import PricingManager from "../pages/system/PricingManager.vue";
import PromotionManager from "../pages/system/PromotionManager.vue";
import SettingsCurrency from "../pages/system/SettingsCurrency.vue";
import PaymentManagement from "../pages/system/PaymentManagement.vue";
import SerialManager from "../pages/system/SerialManager.vue";
import OrderDashboard from "../pages/system/OrderDashboard.vue";
import ProductDashboard from "../pages/system/ProductDashboard.vue";

import Toployalcustomers from "../pages/system/Toployalcustomers.vue";
import Loyaltysummaryadmin from "../pages/system/Loyaltysummaryadmin.vue";
import ReportDashboard from "../pages/system/ReportDashboard.vue";
import ReturnManager from "../pages/system/ReturnManager.vue";

// ===== Audit =====
import AuditLog from "../pages/audit/AuditLog.vue";
import AuditReportDashboard from "../pages/audit/AuditReportDashboard.vue";
import SecurityLog from "../pages/audit/SecurityLog.vue";
import AuditDashboard from "../pages/audit/AuditDashboard.vue";
import UserLoginLog from "../pages/audit/UserLoginLog.vue";

// ===== Inventory =====
import InventoryShell from "../pages/inventory/InventoryShell.vue";
import InventoryOrderDetail from "../pages/inventory/InventoryOrderDetail.vue";
import InventoryOrdersPaid from "../pages/inventory/InventoryOrdersPaid.vue";
import InventoryOrdersProcessing from "../pages/inventory/InventoryOrdersProcessing.vue";
import InventoryOrdersShipping from "../pages/inventory/InventoryOrdersShipping.vue";
import Changepassword from "../pages/customer/Changepassword.vue";

// ===== Sales (POS) =====
import SalesShell from "../pages/sales/SalesShell.vue";
import SalesPOS from "../pages/sales/SalesPOS.vue";
import SalesPickup from "../pages/sales/SalesPickup.vue";
import SalesCustomerManager from "../pages/sales/SalesCustomerManager.vue";
import SalesReturn from "../pages/sales/SalesReturn.vue";
// import SalesOrderHistory from "../pages/sales/SalesOrderHistory.vue"; // TODO

//===== 403 =====
import Forbidden from "../pages/customer/Forbidden.vue";

const routes = [
  // ===== CUSTOMER =====
  {
    path: "/payment-result", // Phải khớp với vnp_ReturnUrl ở Backend
    name: "vnpay-result",
    component: () => import("../pages/customer/VnPayResult.vue"), // Tí nữa tạo file này
    meta: { portal: "customer", requiresAuth: true },
  },
  {
    path: "/delivery-confirm/:orderId",
    name: "delivery-confirm",
    component: () => import("../pages/public/DeliveryConfirm.vue"),
    meta: { portal: "customer", hideHeader: true },
  },
  {
    path: "/",
    name: "home",
    component: CustomerHome,
    meta: { portal: "customer" },
  },
  {
    path: "/login",
    name: "login",
    component: CustomerLogin,
    meta: { portal: "customer", hideHeader: true },
  },
  {
    path: "/change-password",
    name: "change-password",
    component: Changepassword,
    meta: { portal: "customer", requiresAuth: true, hideHeader: true },
  },
  {
    path: "/register",
    name: "register",
    component: CustomerRegister,
    meta: { portal: "customer", hideHeader: true },
  },
  {
    path: "/profile",
    name: "customer-profile",
    component: CustomerProfile,
    meta: { portal: "customer", requiresAuth: true },
  },
  {
    path: "/Spin-wheel",
    name: "Spin-Wheel",
    component: Spinwheel,
    meta: { portal: "customer", requiresAuth: true },
  },
  {
    path: "/orders/new",
    name: "customer-order-create",
    component: CustomerOrderCreate,
    meta: { portal: "customer", requiresAuth: true },
  },
  {
    path: "/orders/:orderId",
    name: "customer-order-detail",
    component: CustomerOrderDetail,
    meta: { portal: "customer", requiresAuth: true },
  },
  {
    path: "/payment/success/:paymentId",
    name: "payment-success",
    component: PaymentSuccess,
    meta: { portal: "customer", requiresAuth: true },
  },
  {
    path: "/cart",
    name: "cart",
    component: CartPage,
    meta: { portal: "customer", requiresAuth: true },
  },
  {
  path: "/forgot-password",
  name: "forgot-password",
  component: ForgotPassword,
  meta: { portal: "customer", hideHeader: true },
},
  
  {
    path: "/my-orders",
    name: "my orders",
    component: CustomerOrders,
    meta: { portal: "customer", requiresAuth: true },
  },
  {
    path: "/product/:id",
    name: "product-detail",
    component: ProductDetail,
    meta: { portal: "customer" },
  },

  // ===== 403 FORBIDDEN =====
  {
    path: "/403",
    name: "forbidden",
    component: Forbidden,
    meta: { hideHeader: true },
  },

  // ===== SYSTEM LOGIN =====
  {
    path: "/system/login",
    name: "system-login",
    component: SystemLogin,
    meta: { portal: "system", hideHeader: true },
  },

  // ===== SYSTEM AREA =====
  {
    path: "/system",
    component: SystemShell,
    meta: { portal: "system", requiresAuth: true },
    children: [
      { path: "", redirect: "/system/dashboard" },

      {
        path: "dashboard",
        name: "system-dashboard",
        component: SystemDashboard,
        meta: { title: "Dashboard" },
      },

      {
        path: "users",
        name: "system-users",
        component: UserManager,
        meta: { title: "User Management" },
      },

      {
        path: "customers",
        name: "system-customers",
        component: CustomerManager,
        meta: { title: "Customers & Loyalty" },
      },
      {
        path: "Toployalcustomers",
        name: "Toployal customers",
        component: Toployalcustomers,
        meta: { title: "Top loyal customers" },
      },

      {
        path: "categories",
        name: "system-categories",
        component: CategoryManager,
        meta: { title: "Categories" },
      },
      {
        path: "products",
        name: "system-products",
        component: ProductManager,
        meta: { title: "Products" },
      },
      {
        path: "serials",
        name: "SerialManager",
        component: SerialManager,
        meta: { title: "Quản lý số Seri" },
      },
      {
        path: "dashboard-products",
        name: "ProductDashboard",
        component: ProductDashboard,
        meta: { title: "Báo cáo Tổng quan" },
      },

      // ===== Orders =====
      {
        path: "order-dashboard",
        name: "order-dashboard",
        component: OrderDashboard,
        meta: { title: "Order Dashboard" },
      },

      {
        path: "orders/:orderId",
        name: "system-order-detail",
        component: SystemOrderDetail,
        meta: { title: "Order Detail" },
      },

      {
        path: "orders/filter",
        name: "order-filter",
        component: OrderFilter,
        meta: { title: "Order Filter" },
      },

      // ===== Pricing / Promotion =====
      {
        path: "pricing",
        name: "system-pricing",
        component: PricingManager,
        meta: { title: "Pricing" },
      },
      {
        path: "promotions",
        name: "system-promotions",
        component: PromotionManager,
        meta: { title: "Promotions" },
      },

      {
        path: "settings/currency",
        name: "system-settings-currency",
        component: SettingsCurrency,
        meta: { title: "Settings - Currency" },
      },

      {
        path: "payments",
        name: "system-payments",
        component: PaymentManagement,
        meta: { title: "Payment History" },
      },

      {
        path: "Loyaltysummaryadmin",
        name: "Loyalty summary admin",
        component: Loyaltysummaryadmin,
        meta: { title: "ADMIN - LOYALTY SUMMARY" },
      },

      {
        path: "return-manager",
        name: "return-manager",
        component: ReturnManager,
        meta: { title: "Return Manager" },
      },

      // ===== Audit =====
      {
        path: "audit-logs",
        name: "system-audit-logs",
        component: AuditLog,
        meta: { title: "Audit Logs" },
      },
      {
        path: "audit-report",
        name: "system-audit-report",
        component: AuditReportDashboard,
        meta: { title: "Audit Report Dashboard" },
      },
      {
        path: "reports",
        name: "reports",
        component: ReportDashboard,
        meta: { title: "Report Dashboard" },
      },
      {
        path: "security-logs",
        name: "system-security-logs",
        component: SecurityLog,
        meta: { title: "Security logs" },
      },
      {
        path: "audit-dashboard",
        name: "system-audit-dashboard",
        component: AuditDashboard,
        meta: { title: "Audit-dashboard" },
      },

      // ===== UserLogin log =====
      {
    path: "user-login-logs",
    name: "system-user-login-logs",
    component: UserLoginLog,
    meta: { title: "User Login Logs" },
    },
    ],
  },

  // ===== INVENTORY =====
  {
    path: "/inventory",
    component: InventoryShell,
    meta: { portal: "inventory", requiresAuth: true },
    children: [
      {
        path: "orders/paid",
        name: "Inventory Orders Paid",
        component: InventoryOrdersPaid,
        meta: { title: "Inventory Orders Paid" },
      },
      {
        path: "orders/processing",
        name: "Inventory Orders Processing",
        component: InventoryOrdersProcessing,
        meta: { title: "Inventory Orders Processing" },
      },
      {
        path: "orders/shipping",
        name: "Inventory Orders Shipping",
        component: InventoryOrdersShipping,
        meta: { title: "Inventory Orders Shipping" },
      },
      {
        path: "orders/:orderId",
        name: "Inventory Order Details",
        component: InventoryOrderDetail,
        meta: { title: "Inventory Order Details" },
      },
    ],
  },

  { path: "/:pathMatch(.*)*", redirect: "/" },

  // ===== SALES LOGIN =====
  {
    path: "/sales/login",
    name: "sales-login",
    component: SystemLogin,
    meta: { portal: "sales", hideHeader: true },
  },

  // ===== SALES AREA =====
  {
    path: "/sales",
    component: SalesShell,
    meta: {
      portal: "sales",
      requiresAuth: true,
      roles: ["SALES"],
      hideHeader: true,
    },
    children: [
      { path: "", redirect: "/sales/pos" },
      {
        path: "pos",
        name: "sales-pos",
        component: SalesPOS,
        meta: { title: "Bán hàng tại quầy" },
      },
      {
        path: "pickup",
        name: "sales-pickup",
        component: SalesPickup,
        meta: { title: "Nhận hàng tại quầy" },
      },
      {
        path: "customers",
        name: "sales-customers",
        component: SalesCustomerManager,
        meta: { title: "Khách hàng tại quầy" },
      },
        {
        path: "returns",
        name: "sales-returns",
        component: SalesReturn,
        meta: { title: "Trả hàng tại quầy" },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const token = getToken();
  const role = (getRole() || "").toUpperCase();
  const isAuthed = !!token;

  // Vue Router 4: child routes không tự inherit meta từ parent
  // → dùng to.matched để lấy portal từ route gần nhất có set portal
  const portal =
    [...to.matched].reverse().find((r) => r.meta?.portal)?.meta?.portal ||
    "customer";
  const isInventoryRoute = portal === "inventory";
  const isSalesRoute = portal === "sales";
  const isInventory = role === "INVENTORY";
  const isSales = role === "SALES";
  const isSystemRoute = portal === "system";
  const isCustomer = role === "CUSTOMER";

  if (to.meta?.requiresAuth && !isAuthed) {
    const redirectUrl = to.fullPath;
    if (isSystemRoute) return { path: "/system/login", query: { redirect: redirectUrl } };
    if (isSalesRoute) return { path: "/sales/login", query: { redirect: redirectUrl } };
    return { path: "/login", query: { redirect: redirectUrl } };
  }

  if (isAuthed) {
    // Redirect khỏi trang login nếu đã đăng nhập
    if (!isSystemRoute && (to.path === "/login" || to.path === "/register")) {
      if (isCustomer) return "/";
      if (isSales) return "/sales/pos";
      return "/system/dashboard";
    }

    if (isSystemRoute && to.path === "/system/login") {
      return !isCustomer ? "/system/dashboard" : "/";
    }

    if (to.path === "/sales/login") {
      if (isSales) return "/sales/pos";
      return "/";
    }
  }

  if (isAuthed) {
    if (role === "CUSTOMER" && portal !== "customer") return "/";
    if (role === "INVENTORY" && portal !== "inventory")
      return "/inventory/orders/paid";
    if (role === "SALES" && portal !== "sales") return "/sales/pos";
    if (role === "ADMIN" && portal !== "system") return "/system/dashboard";
  }

  if (isAuthed) {
    if (isInventoryRoute && !isInventory) {
      return isCustomer ? "/" : isSales ? "/sales/pos" : "/system/dashboard";
    }
    if (!isInventoryRoute && isInventory) {
      return "/inventory/orders/paid";
    }

    if (isSalesRoute && !isSales) {
      return isCustomer ? "/" : "/system/dashboard";
    }
    if (!isSalesRoute && isSales) {
      return "/sales/pos";
    }
  }

  return true;
});

export default router;
