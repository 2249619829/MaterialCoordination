import { state } from "./state.js";
import { parseNumber } from "./utils.js";

export function selectedSupplier() {
  return state.suppliers.find((supplier) => supplier.id === state.selectedSupplierId) || state.suppliers[0] || {
    id: 1,
    companyName: "",
    materials: [],
    certifications: [],
  };
}

export function filteredSuppliers() {
  const keyword = state.supplierFilters.keyword.trim().toLowerCase();
  const category = state.supplierFilters.category;
  return [...state.suppliers]
    .filter((supplier) => {
      const searchable = [
        supplier.companyName,
        supplier.contactName,
        supplier.region,
        supplier.address,
        supplier.certifications.join(" "),
        supplier.materials.map((item) => `${item.name} ${item.category}`).join(" "),
      ].join(" ").toLowerCase();
      const keywordMatched = !keyword || searchable.includes(keyword);
      const categoryMatched = category === "ALL" || supplier.materials.some((item) => item.category === category);
      return keywordMatched && categoryMatched;
    })
    .sort((left, right) => {
      if (state.supplierFilters.sort === "price") return supplierMinPrice(left) - supplierMinPrice(right);
      if (state.supplierFilters.sort === "stock") return supplierTotalStock(right) - supplierTotalStock(left);
      if (state.supplierFilters.sort === "materials") return right.materials.length - left.materials.length;
      return Number(right.rating || 0) - Number(left.rating || 0);
    });
}

export function supplierCategories() {
  return [...new Set(state.suppliers.flatMap((supplier) => supplier.materials.map((item) => item.category)))].sort();
}

export function supplierMinPrice(supplier) {
  const prices = supplier.materials.map((item) => parseNumber(item.price)).filter((item) => Number.isFinite(item));
  return prices.length ? Math.min(...prices) : Number.MAX_SAFE_INTEGER;
}

export function supplierTotalStock(supplier) {
  return supplier.materials.reduce((sum, item) => sum + parseNumber(item.stock), 0);
}

export function cartTotalAmount() {
  if (!state.cart.length) return "¥ 0";
  const total = state.cart.reduce((sum, item) => sum + parseNumber(item.price) * parseNumber(item.quantity), 0);
  return total > 0 ? `¥ ${total.toFixed(2)}` : "待议价";
}

export function unreadNotificationCount() {
  return state.notifications.filter((item) => ["PENDING", "WARN"].includes(item.status)).length;
}

export function notificationClass(item) {
  if (item.status === "WARN") return "amber";
  if (item.status === "PENDING") return "amber";
  if (item.status === "CLAIMED" || item.status === "司机已接单") return "green";
  return item.type === "MQ" ? "amber" : "blue";
}

export function allKnownOrders() {
  return [
    ...state.purchaserOrders,
    ...state.supplierOrders,
    ...state.driverOrders,
    ...state.transportHall,
    ...state.pushOrders,
  ];
}
