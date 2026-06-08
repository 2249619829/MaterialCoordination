import { loadStoredCart } from "./utils.js";

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
  user: JSON.parse(localStorage.getItem("material_user") || "null") || null,
  token: localStorage.getItem("material_token") || "",
  savedLogin: loadSavedLogin(),
  suppliers: [],
  purchaserOrders: [],
  supplierOrders: [],
  driverOrders: [],
  transportHall: [],
  pushOrders: [],
  follows: [],
  materialOptions: [],
  supplierMaterials: [],
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
  selectedSupplierId: 1,
  toast: "",
  reviewModal: null,
  timelineModal: null,
  authMode: "login",
  loginLoading: false,
  actionLoading: {},
  loading: false,
};
