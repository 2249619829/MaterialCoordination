import { sessionTokenStorageKey, sessionUserStorageKey } from "./config.js";
import { loadSessionUser, loadStoredCart } from "./utils.js";

/**
 * 作用：完成 loadSavedLogin 这一步前端处理。
 * 输入：
 * - 无输入参数。
 * 输出：返回整理后的数据，供页面渲染或后续函数使用。
 */
function loadSavedLogin() {
  try {
    return JSON.parse(localStorage.getItem("material_saved_login") || "null") || null;
  } catch (error) {
    localStorage.removeItem("material_saved_login");
    return null;
  }
}

export const state = {
  page: "home",
  sidebarOpen: false,
  user: loadSessionUser(sessionUserStorageKey),
  token: sessionStorage.getItem(sessionTokenStorageKey) || "",
  savedLogin: loadSavedLogin(),
  suppliers: [],
  purchaserOrders: [],
  purchaserRfqs: [],
  selectedRfqId: null,
  selectedRfqQuotes: [],
  supplierOrders: [],
  supplierOpenRfqs: [],
  supplierQuotes: [],
  driverOrders: [],
  transportHall: [],
  pushOrders: [],
  follows: [],
  materialOptions: [],
  supplierMaterials: [],
  supplierQualification: null,
  supplierRanking: [],
  nearbySuppliers: [],
  supplierStore: null,
  notifications: [],
  adminDashboard: null,
  adminSuppliers: [],
  adminOrders: [],
  supplierFilters: { keyword: "", category: "ALL", sort: "rating" },
  cart: loadStoredCart(),
  showNotifications: false,
  attendance: null,
  deadLetters: [],
  selectedSupplierId: null,
  toast: "",
  reviewModal: null,
  acceptanceModal: null,
  paymentModal: null,
  timelineModal: null,
  authMode: "login",
  authError: "",
  loginLoading: false,
  actionLoading: {},
  loading: false,
};
