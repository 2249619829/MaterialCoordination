import { state } from "./state.js";
import { parseNumber } from "./utils.js";

/**
 * 作用：取得当前选中的供应商。
 * 输入：
 * - 无输入参数。
 * 输出：返回供应商对象；如果没有选中供应商，就返回一个空的默认供应商对象。
 */
export function selectedSupplier() {
  return supplierFromList(state.suppliers, state.selectedSupplierId) || {
    id: null,
    companyName: "",
    materials: [],
    certifications: [],
  };
}

/**
 * 作用：从供应商列表里找到指定供应商。
 * 输入：
 * - suppliers：供应商列表，方法会从里面筛选或查找。
 * - selectedSupplierId：当前选中的供应商编号。
 * 输出：返回供应商对象；如果没有匹配编号，就返回列表第一个，列表为空时返回 null。
 */
export function supplierFromList(suppliers, selectedSupplierId) {
  return suppliers.find((supplier) => supplier.id === selectedSupplierId) || suppliers[0] || null;
}

/**
 * 作用：确保当前选中的供应商编号仍然有效。
 * 输入：
 * - suppliers：供应商列表，方法会从里面筛选或查找。
 * 输出：返回当前有效供应商对象；如果没有供应商，就返回 null。
 */
export function ensureSelectedSupplierId(suppliers = state.suppliers) {
  const supplier = supplierFromList(suppliers, state.selectedSupplierId);
  state.selectedSupplierId = supplier?.id ?? null;
  return supplier;
}

/**
 * 作用：根据搜索、分类和排序条件筛选供应商。
 * 输入：
 * - 无输入参数。
 * 输出：返回供应商数组，已经按筛选和排序条件处理好。
 */
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

/**
 * 作用：统计供应商提供的所有物资分类。
 * 输入：
 * - 无输入参数。
 * 输出：返回物资分类数组，每个分类只出现一次。
 */
export function supplierCategories() {
  return [...new Set(state.suppliers.flatMap((supplier) => supplier.materials.map((item) => item.category)))].sort();
}

/**
 * 作用：计算供应商物资中的最低价格。
 * 输入：
 * - supplier：供应商对象，里面有公司名称、评分、物资列表等信息。
 * 输出：返回数字，表示供应商所有物资中的最低价格。
 */
export function supplierMinPrice(supplier) {
  const prices = supplier.materials.map((item) => parseNumber(item.price)).filter((item) => Number.isFinite(item));
  return prices.length ? Math.min(...prices) : Number.MAX_SAFE_INTEGER;
}

/**
 * 作用：计算供应商全部物资的总库存。
 * 输入：
 * - supplier：供应商对象，里面有公司名称、评分、物资列表等信息。
 * 输出：返回数字，表示供应商全部物资库存总和。
 */
export function supplierTotalStock(supplier) {
  return supplier.materials.reduce((sum, item) => sum + parseNumber(item.stock), 0);
}

/**
 * 作用：计算采购清单总金额。
 * 输入：
 * - 无输入参数。
 * 输出：返回文本，表示采购清单总金额，比如 ¥ 100.00 或待议价。
 */
export function cartTotalAmount() {
  if (!state.cart.length) return "¥ 0";
  const total = state.cart.reduce((sum, item) => sum + parseNumber(item.price) * parseNumber(item.quantity), 0);
  return total > 0 ? `¥ ${total.toFixed(2)}` : "待议价";
}

/**
 * 作用：统计未读或待处理通知数量。
 * 输入：
 * - 无输入参数。
 * 输出：返回数字，表示未读或待处理通知数量。
 */
export function unreadNotificationCount() {
  return state.notifications.filter((item) => ["PENDING", "WARN"].includes(item.status)).length;
}

/**
 * 作用：根据通知状态决定它在页面上的颜色样式。
 * 输入：
 * - item：列表中的一项数据。
 * 输出：返回样式名称文本，用来决定通知颜色。
 */
export function notificationClass(item) {
  if (item.status === "WARN") return "amber";
  if (item.status === "PENDING") return "amber";
  if (item.status === "CLAIMED" || item.status === "司机已接单") return "green";
  return item.type === "MQ" ? "amber" : "blue";
}

/**
 * 作用：合并当前页面已经加载过的所有订单。
 * 输入：
 * - 无输入参数。
 * 输出：返回订单数组，包含当前页面已经加载到的各类订单。
 */
export function allKnownOrders() {
  return [
    ...state.purchaserOrders,
    ...state.supplierOrders,
    ...state.driverOrders,
    ...state.transportHall,
    ...state.pushOrders,
  ];
}
