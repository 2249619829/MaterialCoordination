import { apiBase, cartStorageKey, savedLoginStorageKey, sessionTokenStorageKey, sessionUserStorageKey } from "./js/config.js";
import { state } from "./js/state.js";
import { allKnownOrders, ensureSelectedSupplierId, filteredSuppliers, selectedSupplier } from "./js/selectors.js";
import { captureFocus, defaultPurchaseQuantity, escapeHtml, restoreFocus } from "./js/utils.js";
import {
  appTemplate,
  defaultAuthUsername,
  defaultReviewText,
  loginTemplate,
} from "./js/views.js?v=20260611-dispatch-recommendations";

/**
 * 作用：把当前采购清单保存到浏览器本地存储。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行成功后，浏览器本地存储里会保存最新采购清单。
 */
function saveCart() {
  localStorage.setItem(cartStorageKey, JSON.stringify(state.cart));
}

/**
 * 作用：用新的采购清单替换当前清单，并立即保存。
 * 输入：
 * - nextCart：新的采购清单数组。
 * 输出：无显式返回值。执行成功后，state.cart 和本地存储都会变成新的清单。
 */
function setCart(nextCart) {
  state.cart = nextCart;
  saveCart();
}

/**
 * 作用：包装一次按钮或页面操作，防止同一个操作被重复点击。
 * 输入：
 * - actionKey：操作标识，用来判断这个操作是否正在执行。
 * - task：真正要执行的异步任务函数。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function runAction(actionKey, task) {
  if (state.actionLoading[actionKey]) return;
  state.actionLoading = { ...state.actionLoading, [actionKey]: true };
  render();
  try {
    await task();
  } finally {
    const nextLoading = { ...state.actionLoading };
    delete nextLoading[actionKey];
    state.actionLoading = nextLoading;
    render();
  }
}

/**
 * 作用：更新供应商筛选条件，并在需要时重新加载店铺详情。
 * 输入：
 * - nextFilters：新的筛选条件对象。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function applySupplierFilters(nextFilters = {}) {
  state.supplierFilters = { ...state.supplierFilters, ...nextFilters };
  const suppliers = filteredSuppliers();
  const visibleSupplier = ensureSelectedSupplierId(suppliers);
  if (!visibleSupplier || state.supplierStore?.supplier?.id !== visibleSupplier.id) state.supplierStore = null;
  render();
  if (state.user?.userType === "PURCHASER" && visibleSupplier && !state.supplierStore) {
    try {
      await loadSupplierStore();
    } catch (error) {
      showToast(error.message || "店铺详情加载失败");
    }
    render();
  }
}

/**
 * 作用：把后端返回的用户数据整理成前端统一使用的格式。
 * 输入：
 * - data：后端返回或页面传入的数据对象。
 * 输出：返回一个用户对象，里面统一包含 id、userType、username 和 displayName。
 */
function normalizeUser(data) {
  return {
    id: data.id ?? data.userId,
    userType: data.userType,
    username: data.username,
    displayName: data.displayName,
  };
}

/**
 * 作用：向后端接口发送请求，并取出统一响应里的 data 数据。
 * 输入：
 * - path：接口路径，比如 /auth/login。
 * - options：fetch 请求配置，比如请求方法、请求体和额外请求头。
 * 输出：返回 Promise；成功后 Promise 里的结果就是后端响应 data 字段的数据。
 */
async function requestJson(path, options = {}) {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}),
      ...(options.headers || {}),
    },
  });
  const payload = await response.json().catch(() => null);
  if (!response.ok || !payload || payload.code !== 200) {
    throw new Error(payload?.message || `请求失败：${response.status}`);
  }
  return payload.data;
}

/**
 * 作用：根据当前登录角色加载对应工作台数据。
 * 输入：
 * - 无输入参数。
 * 输出：返回 Promise；完成后，不同角色页面需要的数据会写入 state。
 */
async function loadRoleData() {
  if (!state.user) return;
  state.loading = true;
  render();
  try {
    const [suppliers, ranking, fulfillmentRankings, notifications] = await Promise.all([
      requestJson("/api/suppliers/catalog"),
      requestJson("/api/suppliers/ranking"),
      requestJson("/api/rankings/fulfillment"),
      requestJson("/api/notifications"),
    ]);
    state.suppliers = suppliers;
    state.supplierRanking = ranking;
    state.fulfillmentRankings = fulfillmentRankings;
    state.notifications = notifications;
    ensureSelectedSupplierId();
    if (state.user.userType === "PURCHASER") {
      const [orders, nearby, rfqs] = await Promise.all([
        requestJson("/api/purchase-orders/mine"),
        requestJson("/api/suppliers/nearby?longitude=121.47&latitude=31.23&radiusKm=500"),
        requestJson("/api/purchase-rfqs/mine"),
      ]);
      state.purchaserOrders = orders;
      state.nearbySuppliers = nearby;
      state.purchaserRfqs = rfqs;
      if (!state.purchaserRfqs.some((item) => item.id === state.selectedRfqId)) {
        state.selectedRfqId = state.purchaserRfqs[0]?.id || null;
      }
      state.selectedRfqQuotes = state.selectedRfqId
        ? await requestJson(`/api/purchase-rfqs/${state.selectedRfqId}/quotes`)
        : [];
      await loadDispatchRecommendations(orders);
      await loadSupplierStore();
    }
    if (state.user.userType === "SUPPLIER") {
      const [orders, materials, options, openRfqs, quotes, qualification] = await Promise.all([
        requestJson("/api/supplier/orders"),
        requestJson("/api/supplier/materials"),
        requestJson("/api/materials/options"),
        requestJson("/api/supplier/rfqs/open"),
        requestJson("/api/supplier/rfqs/quotes"),
        requestJson("/api/supplier/qualification"),
      ]);
      state.supplierOrders = orders;
      state.supplierMaterials = materials;
      state.materialOptions = options;
      state.supplierOpenRfqs = openRfqs;
      state.supplierQuotes = quotes;
      state.supplierQualification = qualification;
      await loadDispatchRecommendations(orders);
    }
    if (state.user.userType === "DRIVER") {
      const [hall, mine, push, follows, attendance] = await Promise.all([
        requestJson("/api/transport-orders/hall"),
        requestJson("/api/transport-orders/mine"),
        requestJson("/api/transport-orders/push"),
        requestJson("/api/drivers/follows"),
        requestJson("/api/drivers/attendance/today"),
      ]);
      state.transportHall = hall;
      state.driverOrders = mine;
      state.pushOrders = push;
      state.follows = follows;
      state.attendance = attendance;
      state.dispatchRecommendations = { orderId: null, items: [] };
    }
    if (state.user.userType === "ADMIN") {
      const [dashboard, suppliers, orders, deadLetters] = await Promise.all([
        requestJson("/api/admin/dashboard"),
        requestJson("/api/admin/suppliers"),
        requestJson("/api/admin/orders"),
        requestJson("/api/mq/dead-letters"),
      ]);
      state.adminDashboard = dashboard;
      state.adminSuppliers = suppliers;
      state.adminOrders = orders;
      state.deadLetters = deadLetters;
      await loadDispatchRecommendations(orders);
    }
  } catch (error) {
    showToast(error.message || "业务数据加载失败");
  } finally {
    state.loading = false;
    render();
  }
}

/**
 * 作用：加载当前选中供应商的店铺详情。
 * 输入：
 * - 无输入参数。
 * 输出：返回 Promise；完成后，当前供应商店铺详情会写入 state.supplierStore。
 */
async function loadSupplierStore() {
  const supplier = selectedSupplier();
  if (!supplier.id) {
    state.supplierStore = null;
    return;
  }
  state.supplierStore = await requestJson(`/api/suppliers/${supplier.id}/store`);
}

/**
 * 作用：基于待司机接单订单加载智能调度推荐。
 * 输入：
 * - orders：当前角色可见订单列表。
 * 输出：返回 Promise；推荐结果会写入 state.dispatchRecommendations。
 */
async function loadDispatchRecommendations(orders = []) {
  const targetOrder = orders.find((order) => order.status === "待司机接单");
  if (!targetOrder) {
    state.dispatchRecommendations = { orderId: null, items: [] };
    return;
  }
  try {
    const items = await requestJson(`/api/orders/${targetOrder.id}/dispatch-recommendations`);
    state.dispatchRecommendations = { orderId: targetOrder.id, items };
  } catch (error) {
    state.dispatchRecommendations = { orderId: targetOrder.id, items: [] };
  }
}

/**
 * 作用：处理登录表单提交。
 * 输入：
 * - event：浏览器事件对象，通常来自表单提交或按钮点击。
 * 输出：返回 Promise；登录成功后会保存 Token 和用户信息，并刷新页面数据。
 */
async function handleLogin(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const userType = String(form.get("userType") || "").trim();
  const username = String(form.get("username") || "").trim();
  const password = String(form.get("password") || "").trim();
  const rememberPassword = form.get("rememberPassword") === "on";
  state.authError = "";
  if (!username || !password) {
    state.authError = "请输入用户名和密码";
    showToast(state.authError);
    render();
    return;
  }

  state.loginLoading = true;
  render();
  try {
    const data = await requestJson("/auth/login", {
      method: "POST",
      body: JSON.stringify({ userType, username, password }),
    });
    state.token = data.token;
    state.user = normalizeUser(data);
    state.page = "home";
    sessionStorage.setItem(sessionTokenStorageKey, state.token);
    sessionStorage.setItem(sessionUserStorageKey, JSON.stringify(state.user));
    if (rememberPassword) {
      state.savedLogin = { userType, username, password };
      localStorage.setItem(savedLoginStorageKey, JSON.stringify(state.savedLogin));
    } else {
      state.savedLogin = null;
      localStorage.removeItem(savedLoginStorageKey);
    }
    await loadRoleData();
    state.authError = "";
    showToast("登录成功，已进入对应角色工作台");
  } catch (error) {
    state.authError = error.message || "登录失败，请检查账号和密码";
    showToast(state.authError);
  } finally {
    state.loginLoading = false;
    render();
  }
}

/**
 * 作用：处理注册表单提交。
 * 输入：
 * - event：浏览器事件对象，通常来自表单提交或按钮点击。
 * 输出：返回 Promise；注册成功后会保存 Token 和用户信息，并刷新页面数据。
 */
export async function handleRegister(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const userType = String(form.get("userType") || "").trim();
  const username = String(form.get("username") || "").trim();
  const password = String(form.get("password") || "").trim();
  const displayName = String(form.get("displayName") || "").trim();
  const contactPhone = String(form.get("contactPhone") || "").trim();
  const address = String(form.get("address") || "").trim();
  const longitude = parseOptionalCoordinate(form.get("longitude"));
  const latitude = parseOptionalCoordinate(form.get("latitude"));
  state.authError = "";
  if (!username || !password || !displayName || !contactPhone) {
    state.authError = "请填写注册信息";
    showToast(state.authError);
    render();
    return;
  }
  if ((userType === "PURCHASER" || userType === "SUPPLIER") && !address) {
    state.authError = "采购方和供应商注册需要填写地址，用于后端自动获取经纬度";
    showToast(state.authError);
    render();
    return;
  }
  if (longitude === null || latitude === null || (longitude === undefined) !== (latitude === undefined)) {
    state.authError = "经纬度可以留空自动获取，手动填写时需要成对填写有效数字";
    showToast(state.authError);
    render();
    return;
  }

  state.loginLoading = true;
  render();
  try {
    const data = await requestJson("/auth/register", {
      method: "POST",
      body: JSON.stringify({ userType, username, password, displayName, contactPhone, address, longitude, latitude }),
    });
    state.token = data.token;
    state.user = normalizeUser(data);
    state.page = "home";
    state.authMode = "login";
    state.savedLogin = { userType, username, password };
    sessionStorage.setItem(sessionTokenStorageKey, state.token);
    sessionStorage.setItem(sessionUserStorageKey, JSON.stringify(state.user));
    localStorage.setItem(savedLoginStorageKey, JSON.stringify(state.savedLogin));
    await loadRoleData();
    state.authError = "";
    showToast("注册成功，已自动登录");
  } catch (error) {
    state.authError = error.message || "注册失败";
    showToast(state.authError);
  } finally {
    state.loginLoading = false;
    render();
  }
}

/**
 * 作用：清空前端保存的登录状态。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行后，本地保存的登录信息会被清空。
 */
function clearSession() {
  sessionStorage.removeItem(sessionTokenStorageKey);
  sessionStorage.removeItem(sessionUserStorageKey);
  state.token = "";
  state.user = null;
  state.page = "home";
}

export function selectPage(pageId) {
  state.page = pageId;
  state.sidebarOpen = false;
  state.showNotifications = false;
}

/**
 * 作用：调用退出接口并清空本地登录状态。
 * 输入：
 * - 无输入参数。
 * 输出：返回 Promise；完成后用户会退出登录，本地登录状态会被清空。
 */
async function logout() {
  try {
    if (state.token) await requestJson("/auth/logout", { method: "DELETE" });
  } catch (error) {
    showToast("退出时后端未响应，本地登录状态已清理");
  }
  clearSession();
  render();
}

/**
 * 作用：从本地存储恢复上次登录状态。
 * 输入：
 * - 无输入参数。
 * 输出：返回 Promise；如果本地 Token 仍有效，就恢复用户登录状态。
 */
async function restoreSession() {
  if (!state.token) {
    clearSession();
    return;
  }
  try {
    const data = await requestJson("/auth/me");
    state.user = normalizeUser(data);
    sessionStorage.setItem(sessionUserStorageKey, JSON.stringify(state.user));
    await loadRoleData();
  } catch (error) {
    clearSession();
  }
}

/**
 * 作用：为某个物资创建采购订单。
 * 输入：
 * - materialId：物资编号，用来找到要操作的物资。
 * 输出：返回 Promise；完成后会创建订单并刷新当前角色数据。
 */
async function createPurchaseOrder(materialId) {
  await runAction(`create-order:${materialId}`, async () => {
    const supplier = selectedSupplier();
    const material = supplier.materials.find((item) => item.id === materialId);
    if (!supplier.id || !material) {
      showToast("请选择有效的供应商和物资");
      return;
    }
    try {
      const order = await requestJson("/api/purchase-orders", {
        method: "POST",
        body: JSON.stringify({
          supplierId: supplier.id,
          materialId,
          quantity: defaultPurchaseQuantity(material),
          remark: "采购方通过供应商菜单确认订单",
        }),
      });
      state.purchaserOrders = [order, ...state.purchaserOrders];
      showToast("采购订单已确认，并进入平台大厅推送给司机");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "创建采购订单失败");
    }
  });
}

/**
 * 作用：把物资加入采购清单。
 * 输入：
 * - materialId：物资编号，用来找到要操作的物资。
 * 输出：无显式返回值。执行后，物资会出现在采购清单中。
 */
function addToCart(materialId) {
  const supplier = selectedSupplier();
  const material = supplier.materials.find((item) => item.id === materialId);
  if (!material) return;
  if (state.cart.length && state.cart[0].supplierId !== supplier.id) {
    setCart([]);
    showToast("已切换供应商，旧采购清单已清空");
  }
  const existing = state.cart.find((item) => item.materialId === materialId);
  if (existing) {
    showToast("采购清单中已有该物资，可以直接调整数量");
    return;
  }
  setCart([...state.cart, {
    supplierId: supplier.id,
    supplierName: supplier.companyName,
    materialId: material.id,
    name: material.name,
    unit: material.unit,
    price: material.price,
    quantity: `100 ${material.unit}`,
  }]);
  showToast("已加入采购清单");
}

/**
 * 作用：修改采购清单中某个物资的数量。
 * 输入：
 * - materialId：物资编号，用来找到要操作的物资。
 * - quantity：采购数量文本或数字。
 * 输出：无显式返回值。执行后，采购清单中对应物资数量会更新。
 */
function updateCartQuantity(materialId, quantity) {
  const item = state.cart.find((cartItem) => cartItem.materialId === materialId);
  if (!item) return;
  item.quantity = quantity.trim() || `100 ${item.unit}`;
  saveCart();
}

/**
 * 作用：从采购清单中移除某个物资。
 * 输入：
 * - materialId：物资编号，用来找到要操作的物资。
 * 输出：无显式返回值。执行后，采购清单中对应物资会被移除。
 */
function removeCartItem(materialId) {
  setCart(state.cart.filter((item) => item.materialId !== materialId));
  render();
}

/**
 * 作用：把采购清单批量提交成订单。
 * 输入：
 * - 无输入参数。
 * 输出：返回 Promise；完成后会生成订单并清空采购清单。
 */
async function checkoutCart() {
  if (!state.cart.length) {
    showToast("采购清单为空");
    return;
  }
  await runAction("checkout-cart", async () => {
    try {
      const orders = await requestJson("/api/purchase-orders/cart/checkout", {
        method: "POST",
        body: JSON.stringify({
          supplierId: state.cart[0].supplierId,
          remark: "采购方通过购物车/询价单批量提交",
          items: state.cart.map((item) => ({
            materialId: item.materialId,
            quantity: item.quantity,
          })),
        }),
      });
      setCart([]);
      state.purchaserOrders = [...orders, ...state.purchaserOrders];
      showToast(`采购清单已提交，生成 ${orders.length} 条采购订单`);
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "提交采购清单失败");
    }
  });
}

/**
 * 作用：采购方发布询价单。
 * 输入：
 * - event：浏览器事件对象，通常来自询价表单提交。
 * 输出：返回 Promise；成功后刷新询价列表，并选中新建询价单。
 */
export async function createPurchaseRfq(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const materialName = String(form.get("materialName") || "").trim();
  const category = String(form.get("category") || "").trim();
  const unit = String(form.get("unit") || "").trim();
  const quantity = String(form.get("quantity") || "").trim();
  const deliveryAddress = String(form.get("deliveryAddress") || "").trim();
  const remark = String(form.get("remark") || "").trim();
  const longitude = parseOptionalCoordinate(form.get("longitude"));
  const latitude = parseOptionalCoordinate(form.get("latitude"));

  if (!materialName || !category || !unit || !quantity || !deliveryAddress) {
    showToast("请填写物资、分类、单位、数量和收货地址");
    return;
  }
  if (longitude === null || latitude === null || (longitude === undefined) !== (latitude === undefined)) {
    showToast("经纬度可以留空由后端自动获取，手动填写时需要成对填写有效数字");
    return;
  }

  await runAction("create-rfq", async () => {
    try {
      const rfq = await requestJson("/api/purchase-rfqs", {
        method: "POST",
        body: JSON.stringify({
          materialName,
          category,
          unit,
          quantity,
          deliveryAddress,
          longitude,
          latitude,
          remark,
        }),
      });
      state.selectedRfqId = rfq.id;
      state.page = "rfqs";
      showToast("询价已发布，供应商可以开始报价");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "发布询价失败");
    }
  });
}

/**
 * 作用：加载采购方某个询价单的报价列表。
 * 输入：
 * - rfqId：询价单编号。
 * 输出：返回 Promise；成功后页面会显示该询价单所有报价。
 */
async function loadRfqQuotes(rfqId) {
  await runAction(`load-rfq-quotes:${rfqId}`, async () => {
    try {
      state.selectedRfqId = rfqId;
      state.selectedRfqQuotes = await requestJson(`/api/purchase-rfqs/${rfqId}/quotes`);
      render();
    } catch (error) {
      showToast(error.message || "报价加载失败");
    }
  });
}

/**
 * 作用：采购方采纳供应商报价，并生成采购订单。
 * 输入：
 * - quoteId：报价编号。
 * 输出：返回 Promise；成功后询价关闭，订单进入现有采购/供货流程。
 */
async function acceptRfqQuote(quoteId) {
  await runAction(`accept-rfq-quote:${quoteId}`, async () => {
    try {
      await requestJson(`/api/purchase-rfqs/quotes/${quoteId}/accept`, { method: "POST" });
      showToast("已采纳报价，采购订单已生成");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "采纳报价失败");
    }
  });
}

/**
 * 作用：供应商对采购方询价提交或更新报价。
 * 输入：
 * - event：浏览器事件对象，通常来自报价表单提交。
 * 输出：返回 Promise；成功后刷新开放询价和我的报价。
 */
async function submitSupplierRfqQuote(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const rfqId = Number(form.get("rfqId") || 0);
  const supplierMaterialId = Number(form.get("supplierMaterialId") || 0);
  const unitPrice = parseAmount(form.get("unitPrice"));
  const availableQuantity = parseWholeNumber(form.get("availableQuantity"));
  const deliveryDays = parseWholeNumber(form.get("deliveryDays"));
  const remark = String(form.get("remark") || "").trim();

  if (!rfqId || !supplierMaterialId) {
    showToast("请选择要报价的供应物资");
    return;
  }
  if ([unitPrice, availableQuantity, deliveryDays].some((item) => item === null)) {
    showToast("请填写有效的单价、可供数量和交付天数");
    return;
  }

  await runAction(`quote-rfq:${rfqId}`, async () => {
    try {
      await requestJson("/api/supplier/rfqs/quotes", {
        method: "POST",
        body: JSON.stringify({
          rfqId,
          supplierMaterialId,
          unitPrice,
          availableQuantity,
          deliveryDays,
          remark,
        }),
      });
      showToast("报价已提交，采购方会按价格、库存、交期和评分看到排序");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "提交报价失败");
    }
  });
}

/**
 * 作用：司机抢下一个运输订单。
 * 输入：
 * - orderId：订单编号，用来找到要操作的订单。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function claimOrder(orderId) {
  await runAction(`claim-order:${orderId}`, async () => {
    try {
      await requestJson(`/api/transport-orders/${orderId}/claim`, { method: "POST" });
      showToast("抢单成功，订单状态已更新");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "抢单失败");
    }
  });
}

/**
 * 作用：供应商确认供货订单。
 * 输入：
 * - orderId：订单编号，用来找到要操作的订单。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function confirmSupplierOrder(orderId) {
  await runAction(`confirm-supplier-order:${orderId}`, async () => {
    try {
      await requestJson(`/api/supplier/orders/${orderId}/confirm`, { method: "POST" });
      showToast("已确认供货，库存已扣减，订单进入运输大厅");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "确认供货失败");
    }
  });
}

/**
 * 作用：供应商拒绝供货订单。
 * 输入：
 * - orderId：订单编号，用来找到要操作的订单。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function rejectSupplierOrder(orderId) {
  await runAction(`reject-supplier-order:${orderId}`, async () => {
    try {
      await requestJson(`/api/supplier/orders/${orderId}/reject`, { method: "POST" });
      showToast("已拒单，采购方可重新选择供应商");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "拒单失败");
    }
  });
}

/**
 * 作用：司机开始运输订单。
 * 输入：
 * - orderId：订单编号，用来找到要操作的订单。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function startTransport(orderId) {
  await runAction(`start-transport:${orderId}`, async () => {
    try {
      await requestJson(`/api/transport-orders/${orderId}/start`, { method: "POST" });
      showToast("订单已进入运输中");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "开始运输失败");
    }
  });
}

/**
 * 作用：司机完成运输订单。
 * 输入：
 * - orderId：订单编号，用来找到要操作的订单。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function completeTransport(orderId) {
  await runAction(`complete-transport:${orderId}`, async () => {
    try {
      await requestJson(`/api/transport-orders/${orderId}/complete`, { method: "POST" });
      showToast("订单已完成，可以发起三方评价");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "完成运输失败");
    }
  });
}

/**
 * 作用：司机关注一个采购方。
 * 输入：
 * - purchaserId：采购方编号，用来找到要关注的采购方。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function followPurchaser(purchaserId) {
  try {
    await requestJson("/api/drivers/follows", {
      method: "POST",
      body: JSON.stringify({ targetId: purchaserId }),
    });
    showToast("关注成功，后续可接收该采购方推送");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "关注失败");
  }
}

/**
 * 作用：处理供应商新增物资表单提交。
 * 输入：
 * - event：浏览器事件对象，通常来自表单提交或按钮点击。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function createSupplierMaterial(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const materialId = Number(form.get("materialId") || 0);
  const materialName = String(form.get("materialName") || "").trim();
  const category = String(form.get("category") || "").trim();
  const unit = String(form.get("unit") || "").trim();
  const supplyPrice = parseAmount(form.get("supplyPrice"));
  const stockQuantity = parseWholeNumber(form.get("stockQuantity"));
  const dailyCapacity = parseWholeNumber(form.get("dailyCapacity"));
  const deliveryRadiusKm = parseAmount(form.get("deliveryRadiusKm"));

  if (!materialId && !materialName) {
    showToast("请选择已有物资，或填写新物资名称");
    return;
  }
  if (materialName && (!category || !unit)) {
    showToast("新增物资需要填写分类和单位");
    return;
  }
  if (materialId && materialName) {
    showToast("请选择已有物资或填写新物资，不要同时填写");
    return;
  }
  if ([supplyPrice, stockQuantity, dailyCapacity, deliveryRadiusKm].some((item) => item === null)) {
    showToast("请填写有效的价格、库存、日产能和配送半径");
    return;
  }

  try {
    await requestJson("/api/supplier/materials", {
      method: "POST",
      body: JSON.stringify({
        materialId: materialName ? null : materialId,
        materialName,
        category,
        unit,
        supplyPrice,
        stockQuantity,
        dailyCapacity,
        deliveryRadiusKm,
        status: 1,
      }),
    });
    showToast("供货物资已保存，供应商目录缓存已删除");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "保存供货物资失败");
  }
}

/**
 * 作用：供应商保存企业资质资料。
 * 输入：
 * - event：浏览器事件对象，通常来自企业资质表单提交。
 * 输出：返回 Promise；成功后资质状态进入待复核。
 */
export async function updateSupplierQualification(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const companyName = String(form.get("companyName") || "").trim();
  const contactName = String(form.get("contactName") || "").trim();
  const contactPhone = String(form.get("contactPhone") || "").trim();
  const licenseNo = String(form.get("licenseNo") || "").trim();
  const address = String(form.get("address") || "").trim();
  const longitude = parseOptionalCoordinate(form.get("longitude"));
  const latitude = parseOptionalCoordinate(form.get("latitude"));
  const previousQualification = state.supplierQualification || {};
  const addressChanged = address !== String(previousQualification.address || "").trim();
  const staleCoordinates = addressChanged
    && sameCoordinate(longitude, previousQualification.longitude)
    && sameCoordinate(latitude, previousQualification.latitude);
  const submittedLongitude = staleCoordinates ? undefined : longitude;
  const submittedLatitude = staleCoordinates ? undefined : latitude;
  const businessLicenseUrl = String(form.get("businessLicenseUrl") || "").trim();
  const safetyCertUrl = String(form.get("safetyCertUrl") || "").trim();
  const insuranceCertUrl = String(form.get("insuranceCertUrl") || "").trim();

  if (!companyName || !contactName || !contactPhone || !licenseNo || !address) {
    showToast("请填写企业名称、联系人、电话、执照编号和经营地址");
    return;
  }
  if (longitude === null || latitude === null) {
    showToast("经纬度可以留空，但填写时必须是有效数字");
    return;
  }

  await runAction("supplier-qualification", async () => {
    try {
      state.supplierQualification = await requestJson("/api/supplier/qualification", {
        method: "PUT",
        body: JSON.stringify({
          companyName,
          contactName,
          contactPhone,
          licenseNo,
          address,
          longitude: submittedLongitude,
          latitude: submittedLatitude,
          businessLicenseUrl,
          safetyCertUrl,
          insuranceCertUrl,
        }),
      });
      showToast("企业资质已保存，状态已进入待复核");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "保存企业资质失败");
    }
  });
}

/**
 * 作用：更新供应商物资信息。
 * 输入：
 * - materialId：物资编号，用来找到要操作的物资。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function updateSupplierMaterial(materialId) {
  const material = state.supplierMaterials.find((item) => item.id === materialId);
  if (!material) return;
  const supplyPrice = parseAmount(document.querySelector(`[data-edit-price="${materialId}"]`)?.value || material.supplyPrice);
  const stockQuantity = parseWholeNumber(document.querySelector(`[data-edit-stock="${materialId}"]`)?.value || material.stockQuantity);
  const dailyCapacity = parseWholeNumber(document.querySelector(`[data-edit-capacity="${materialId}"]`)?.value || material.dailyCapacity);
  const deliveryRadiusKm = parseAmount(document.querySelector(`[data-edit-radius="${materialId}"]`)?.value || material.deliveryRadiusKm);
  if ([supplyPrice, stockQuantity, dailyCapacity, deliveryRadiusKm].some((item) => item === null)) {
    showToast("请填写有效的价格、库存、日产能和配送半径");
    return;
  }
  try {
    await requestJson(`/api/supplier/materials/${materialId}`, {
      method: "PUT",
      body: JSON.stringify({
        materialId: material.materialId,
        supplyPrice,
        stockQuantity,
        dailyCapacity,
        deliveryRadiusKm,
        status: 1,
      }),
    });
    showToast("供应物资已更新，Cache Aside 写路径已触发");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "更新失败");
  }
}

/**
 * 作用：把页面输入的金额文本转换成数字。
 * 输入：
 * - value：页面输入的文本或数字。
 * 输出：返回数字，表示从输入文本中解析出的金额。
 */
function parseAmount(value) {
  const normalized = String(value ?? "").trim();
  if (!normalized) return null;
  const amount = Number(normalized);
  return Number.isFinite(amount) && amount >= 0 ? amount : null;
}

/**
 * 作用：把页面输入的整数文本转换成整数。
 * 输入：
 * - value：页面输入的文本或数字。
 * 输出：返回整数，表示从输入文本中解析出的数量。
 */
function parseWholeNumber(value) {
  const normalized = String(value ?? "").trim();
  if (!normalized) return null;
  const amount = Number(normalized);
  return Number.isInteger(amount) && amount >= 0 ? amount : null;
}

/**
 * 作用：把页面输入的经纬度文本转换成数字。
 * 输入：
 * - value：页面输入的经纬度文本。
 * 输出：返回数字；无法解析时返回 null。
 */
function parseCoordinate(value) {
  const normalized = String(value ?? "").trim();
  if (!normalized) return null;
  const coordinate = Number(normalized);
  return Number.isFinite(coordinate) ? coordinate : null;
}

/**
 * 作用：把可选经纬度文本转换成数字或空值。
 * 输入：
 * - value：页面输入的经纬度文本。
 * 输出：返回数字、undefined 或 null；null 表示填写了无效内容。
 */
function parseOptionalCoordinate(value) {
  const normalized = String(value ?? "").trim();
  if (!normalized) return undefined;
  return parseCoordinate(normalized);
}

function sameCoordinate(left, right) {
  if (left === undefined || left === null || right === undefined || right === null) {
    return false;
  }
  const leftNumber = Number(left);
  const rightNumber = Number(right);
  return Number.isFinite(leftNumber)
    && Number.isFinite(rightNumber)
    && Math.abs(leftNumber - rightNumber) < 0.000001;
}

/**
 * 作用：下架供应商物资。
 * 输入：
 * - materialId：物资编号，用来找到要操作的物资。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function offlineSupplierMaterial(materialId) {
  try {
    await requestJson(`/api/supplier/materials/${materialId}/offline`, { method: "POST" });
    showToast("物资已下架，采购方供应商大厅会刷新");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "下架失败");
  }
}

/**
 * 作用：更新司机在线或出勤状态。
 * 输入：
 * - online：司机是否在线或出勤，true 表示在线，false 表示离线。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function markAttendance(online) {
  try {
    state.attendance = await requestJson(`/api/drivers/attendance?online=${online}`, { method: "POST" });
    showToast(online ? "已上线，BitMap 出勤位已写入" : "已离线，BitMap 出勤位已清理");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "更新出勤失败");
  }
}

/**
 * 作用：把推送订单标记为已读。
 * 输入：
 * - orderId：订单编号，用来找到要操作的订单。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function markPushRead(orderId) {
  await runAction(`read-push:${orderId}`, async () => {
    try {
      await requestJson(`/api/transport-orders/push/${orderId}/read`, { method: "POST" });
      showToast("推送已标记为已读");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "标记已读失败");
    }
  });
}

/**
 * 作用：触发后端补偿生成推送记录。
 * 输入：
 * - 无输入参数。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function retryPushRecords() {
  try {
    const count = await requestJson("/api/order-push/retry", { method: "POST" });
    showToast(`补偿完成，新增 ${count} 条推送记录`);
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "补偿失败");
  }
}

/**
 * 作用：管理员审核通过供应商。
 * 输入：
 * - supplierId：供应商编号，用来找到要审核或展示的供应商。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function approveSupplier(supplierId) {
  await runAction(`admin-approve-supplier:${supplierId}`, async () => {
    try {
      await requestJson(`/api/admin/suppliers/${supplierId}/approve`, { method: "POST" });
      showToast("供应商已审核通过");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "审核通过失败");
    }
  });
}

/**
 * 作用：管理员驳回供应商。
 * 输入：
 * - supplierId：供应商编号，用来找到要审核或展示的供应商。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function rejectSupplierByAdmin(supplierId) {
  await runAction(`admin-reject-supplier:${supplierId}`, async () => {
    try {
      await requestJson(`/api/admin/suppliers/${supplierId}/reject`, { method: "POST" });
      showToast("供应商已驳回/停用");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "驳回供应商失败");
    }
  });
}

/**
 * 作用：打开订单评价弹窗。
 * 输入：
 * - orderId：订单编号，用来找到要操作的订单。
 * 输出：无显式返回值。执行后，评价弹窗会打开并显示指定订单。
 */
function openReviewModal(orderId) {
  const order = allKnownOrders()
    .find((item) => item.id === orderId);
  if (!order) return;
  state.reviewModal = { order };
  render();
}

/**
 * 作用：打开采购方订单验收弹窗。
 * 输入：
 * - orderId：订单编号，用来找到要验收的订单。
 * 输出：无显式返回值。执行后会打开验收弹窗。
 */
function openAcceptanceModal(orderId) {
  const order = allKnownOrders().find((item) => item.id === orderId);
  if (!order) return;
  state.acceptanceModal = { order };
  render();
}

/**
 * 作用：打开采购方订单付款弹窗。
 * 输入：
 * - orderId：订单编号，用来找到要付款的订单。
 * 输出：无显式返回值。执行后会打开付款弹窗。
 */
function openPaymentModal(orderId) {
  const order = allKnownOrders().find((item) => item.id === orderId);
  if (!order) return;
  state.paymentModal = { order };
  render();
}

/**
 * 作用：打开订单时间线弹窗并加载数据。
 * 输入：
 * - orderId：订单编号，用来找到要操作的订单。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function openTimelineModal(orderId) {
  const order = allKnownOrders().find((item) => item.id === orderId);
  if (!order) return;
  try {
    const items = await requestJson(`/api/orders/${orderId}/timeline`);
    state.timelineModal = { order, items };
    render();
  } catch (error) {
    showToast(error.message || "时间线加载失败");
  }
}

/**
 * 作用：关闭订单评价弹窗。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行后，评价弹窗会关闭。
 */
function closeReviewModal() {
  state.reviewModal = null;
  render();
}

/**
 * 作用：关闭订单验收弹窗。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行后，验收弹窗会关闭。
 */
function closeAcceptanceModal() {
  state.acceptanceModal = null;
  render();
}

/**
 * 作用：关闭付款登记弹窗。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行后，付款弹窗会关闭。
 */
function closePaymentModal() {
  state.paymentModal = null;
  render();
}

/**
 * 作用：关闭订单时间线弹窗。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行后，订单时间线弹窗会关闭。
 */
function closeTimelineModal() {
  state.timelineModal = null;
  render();
}

/**
 * 作用：提交订单评价表单。
 * 输入：
 * - event：浏览器事件对象，通常来自表单提交或按钮点击。
 * 输出：返回 Promise；等异步操作完成后，页面状态或后端数据会被更新。
 */
async function submitReview(event) {
  event.preventDefault();
  if (!state.reviewModal) return;
  const form = new FormData(event.currentTarget);
  const [targetType, targetId] = String(form.get("target") || "").split(":");
  const score = Number(form.get("score"));
  const content = String(form.get("content") || "").trim();
  if (!Number.isInteger(score) || score < 1 || score > 5) {
    showToast("评分必须是 1 到 5 的整数");
    return;
  }
  try {
    await requestJson(`/api/orders/${state.reviewModal.order.id}/reviews`, {
      method: "POST",
      body: JSON.stringify({
        targetType,
        targetId: Number(targetId),
        score,
        content: content || defaultReviewText(targetType),
      }),
    });
    state.reviewModal = null;
    showToast("评价已提交，供应商评分和 ZSet 排行榜已同步");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "评价失败");
  }
}

/**
 * 作用：提交采购方验收签收表单。
 * 输入：
 * - event：浏览器事件对象，通常来自表单提交。
 * 输出：返回 Promise；成功后会刷新订单列表并关闭弹窗。
 */
async function submitAcceptance(event) {
  event.preventDefault();
  if (!state.acceptanceModal) return;
  const form = new FormData(event.currentTarget);
  const signerName = String(form.get("signerName") || "").trim();
  const acceptanceResult = String(form.get("acceptanceResult") || "ACCEPTED").trim();
  const proofUrl = String(form.get("proofUrl") || "").trim();
  const remark = String(form.get("remark") || "").trim();
  if (!signerName) {
    showToast("请填写签收人");
    return;
  }
  await runAction(`accept-order:${state.acceptanceModal.order.id}`, async () => {
    try {
      await requestJson(`/api/purchase-orders/${state.acceptanceModal.order.id}/acceptance`, {
        method: "POST",
        body: JSON.stringify({
          signerName,
          acceptanceResult,
          proofUrl,
          remark,
        }),
      });
      state.acceptanceModal = null;
      showToast("验收签收已提交，订单凭证已归档");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "提交验收失败");
    }
  });
}

/**
 * 作用：提交采购方付款登记表单。
 * 输入：
 * - event：浏览器事件对象，通常来自表单提交。
 * 输出：返回 Promise；成功后会刷新订单列表并关闭弹窗。
 */
async function submitPayment(event) {
  event.preventDefault();
  if (!state.paymentModal) return;
  const form = new FormData(event.currentTarget);
  const amount = String(form.get("amount") || "").trim();
  const paymentMethod = String(form.get("paymentMethod") || "BANK_TRANSFER").trim();
  const paymentReference = String(form.get("paymentReference") || "").trim();
  const proofUrl = String(form.get("proofUrl") || "").trim();
  const remark = String(form.get("remark") || "").trim();
  if (!/^\d+(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) {
    showToast("请填写正确的付款金额，最多两位小数");
    return;
  }
  if (!paymentReference) {
    showToast("请填写付款流水号");
    return;
  }
  await runAction(`pay-order:${state.paymentModal.order.id}`, async () => {
    try {
      await requestJson(`/api/purchase-orders/${state.paymentModal.order.id}/payment`, {
        method: "POST",
        body: JSON.stringify({
          amount,
          paymentMethod,
          paymentReference,
          proofUrl,
          remark,
        }),
      });
      state.paymentModal = null;
      showToast("付款登记已提交，订单结算状态已更新");
      await loadRoleData();
    } catch (error) {
      showToast(error.message || "提交付款失败");
    }
  });
}

/**
 * 作用：给页面元素绑定点击、提交和输入事件。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行后，页面按钮、表单和输入框会绑定对应事件。
 */
function bindEvents() {
  const loginForm = document.getElementById("loginForm");
  if (loginForm) {
    loginForm.addEventListener("submit", state.authMode === "login" ? handleLogin : handleRegister);
    document.querySelectorAll("[data-auth-mode]").forEach((element) => {
      element.addEventListener("click", () => {
        state.authMode = element.dataset.authMode;
        state.authError = "";
        render();
      });
    });
    const userType = document.getElementById("userType");
    const username = document.getElementById("username");
    const password = document.getElementById("password");
    const rememberPassword = document.getElementById("rememberPassword");
    const displayName = document.getElementById("displayName");
    const contactPhone = document.getElementById("contactPhone");
    userType?.addEventListener("change", () => {
      if (state.authMode === "login") {
        const savedLogin = state.savedLogin?.userType === userType.value ? state.savedLogin : null;
        username.value = savedLogin?.username || defaultAuthUsername(userType.value);
        password.value = savedLogin?.password || "";
        if (rememberPassword) rememberPassword.checked = Boolean(savedLogin);
      } else {
        username.value = "";
        password.value = "";
        if (displayName) displayName.value = "";
        if (contactPhone) contactPhone.value = "";
      }
    });
    return;
  }

  document.querySelectorAll("[data-page]").forEach((element) => {
    element.addEventListener("click", () => {
      selectPage(element.dataset.page);
      render();
    });
  });
  document.querySelectorAll("[data-supplier]").forEach((element) => {
    element.addEventListener("click", async () => {
      state.selectedSupplierId = Number(element.dataset.supplier);
      state.supplierStore = null;
      render();
      try {
        await loadSupplierStore();
      } catch (error) {
        showToast(error.message || "店铺详情加载失败");
      }
      render();
    });
  });
  document.getElementById("supplierKeyword")?.addEventListener("input", (event) => {
    window.clearTimeout(bindEvents.supplierFilterTimer);
    const keyword = event.target.value;
    bindEvents.supplierFilterTimer = window.setTimeout(() => {
      applySupplierFilters({ keyword });
    }, 250);
  });
  document.getElementById("supplierCategory")?.addEventListener("change", (event) => {
    applySupplierFilters({ category: event.target.value });
  });
  document.getElementById("supplierSort")?.addEventListener("change", (event) => {
    applySupplierFilters({ sort: event.target.value });
  });
  document.querySelectorAll("[data-create-order]").forEach((element) => {
    element.addEventListener("click", () => createPurchaseOrder(Number(element.dataset.createOrder)));
  });
  document.querySelectorAll("[data-add-cart]").forEach((element) => {
    element.addEventListener("click", () => {
      addToCart(Number(element.dataset.addCart));
      render();
    });
  });
  document.querySelectorAll("[data-cart-quantity]").forEach((element) => {
    element.addEventListener("change", () => {
      updateCartQuantity(Number(element.dataset.cartQuantity), element.value);
      render();
    });
  });
  document.querySelectorAll("[data-remove-cart]").forEach((element) => {
    element.addEventListener("click", () => removeCartItem(Number(element.dataset.removeCart)));
  });
  document.getElementById("clearCart")?.addEventListener("click", () => {
    setCart([]);
    render();
  });
  document.querySelectorAll("[data-checkout-cart]").forEach((element) => {
    element.addEventListener("click", checkoutCart);
  });
  const rfqForm = document.getElementById("rfqForm");
  rfqForm?.addEventListener("submit", createPurchaseRfq);
  document.querySelectorAll("[data-view-rfq-quotes]").forEach((element) => {
    element.addEventListener("click", () => loadRfqQuotes(Number(element.dataset.viewRfqQuotes)));
  });
  document.querySelectorAll("[data-accept-rfq-quote]").forEach((element) => {
    element.addEventListener("click", () => acceptRfqQuote(Number(element.dataset.acceptRfqQuote)));
  });
  document.querySelectorAll("[data-rfq-quote-form]").forEach((element) => {
    element.addEventListener("submit", submitSupplierRfqQuote);
  });
  document.querySelectorAll("[data-chat]").forEach((element) => {
    element.addEventListener("click", () => showToast("已发起对话：演示版默认沟通通过"));
  });
  document.querySelectorAll("[data-claim-order]").forEach((element) => {
    element.addEventListener("click", () => claimOrder(element.dataset.claimOrder));
  });
  document.querySelectorAll("[data-confirm-supplier-order]").forEach((element) => {
    element.addEventListener("click", () => confirmSupplierOrder(element.dataset.confirmSupplierOrder));
  });
  document.querySelectorAll("[data-reject-supplier-order]").forEach((element) => {
    element.addEventListener("click", () => rejectSupplierOrder(element.dataset.rejectSupplierOrder));
  });
  document.querySelectorAll("[data-start-transport]").forEach((element) => {
    element.addEventListener("click", () => startTransport(element.dataset.startTransport));
  });
  document.querySelectorAll("[data-complete-transport]").forEach((element) => {
    element.addEventListener("click", () => completeTransport(element.dataset.completeTransport));
  });
  document.querySelectorAll("[data-follow-purchaser]").forEach((element) => {
    element.addEventListener("click", () => followPurchaser(Number(element.dataset.followPurchaser)));
  });
  document.querySelectorAll("[data-update-material]").forEach((element) => {
    element.addEventListener("click", () => updateSupplierMaterial(Number(element.dataset.updateMaterial)));
  });
  document.querySelectorAll("[data-offline-material]").forEach((element) => {
    element.addEventListener("click", () => offlineSupplierMaterial(Number(element.dataset.offlineMaterial)));
  });
  document.querySelectorAll("[data-review-order]").forEach((element) => {
    element.addEventListener("click", () => openReviewModal(element.dataset.reviewOrder));
  });
  document.querySelectorAll("[data-accept-order]").forEach((element) => {
    element.addEventListener("click", () => openAcceptanceModal(element.dataset.acceptOrder));
  });
  document.querySelectorAll("[data-pay-order]").forEach((element) => {
    element.addEventListener("click", () => openPaymentModal(element.dataset.payOrder));
  });
  document.querySelectorAll("[data-order-timeline]").forEach((element) => {
    element.addEventListener("click", () => openTimelineModal(element.dataset.orderTimeline));
  });
  document.querySelectorAll("[data-attendance]").forEach((element) => {
    element.addEventListener("click", () => markAttendance(element.dataset.attendance === "true"));
  });
  document.querySelectorAll("[data-read-push]").forEach((element) => {
    element.addEventListener("click", () => markPushRead(element.dataset.readPush));
  });
  document.querySelectorAll("[data-retry-push]").forEach((element) => {
    element.addEventListener("click", retryPushRecords);
  });
  document.querySelectorAll("[data-admin-approve-supplier]").forEach((element) => {
    element.addEventListener("click", () => approveSupplier(Number(element.dataset.adminApproveSupplier)));
  });
  document.querySelectorAll("[data-admin-reject-supplier]").forEach((element) => {
    element.addEventListener("click", () => rejectSupplierByAdmin(Number(element.dataset.adminRejectSupplier)));
  });
  document.getElementById("retryPush")?.addEventListener("click", retryPushRecords);
  document.getElementById("notificationBtn")?.addEventListener("click", () => {
    state.showNotifications = !state.showNotifications;
    render();
  });
  document.getElementById("refreshNotifications")?.addEventListener("click", loadRoleData);
  bindSupplierMaterialFormHelpers();
  document.getElementById("supplierMaterialForm")?.addEventListener("submit", createSupplierMaterial);
  document.getElementById("supplierQualificationForm")?.addEventListener("submit", updateSupplierQualification);
  document.getElementById("reviewForm")?.addEventListener("submit", submitReview);
  document.getElementById("acceptanceForm")?.addEventListener("submit", submitAcceptance);
  document.getElementById("paymentForm")?.addEventListener("submit", submitPayment);
  document.querySelectorAll("[data-close-review]").forEach((element) => {
    element.addEventListener("click", (event) => {
      if (event.target === element) closeReviewModal();
    });
  });
  document.querySelectorAll("[data-close-acceptance]").forEach((element) => {
    element.addEventListener("click", (event) => {
      if (event.target === element) closeAcceptanceModal();
    });
  });
  document.querySelectorAll("[data-close-payment]").forEach((element) => {
    element.addEventListener("click", (event) => {
      if (event.target === element) closePaymentModal();
    });
  });
  document.querySelectorAll("[data-close-timeline]").forEach((element) => {
    element.addEventListener("click", (event) => {
      if (event.target === element) closeTimelineModal();
    });
  });
  document.getElementById("refreshData")?.addEventListener("click", loadRoleData);
  document.querySelectorAll("[data-logout]").forEach((element) => {
    element.addEventListener("click", logout);
  });
  document.getElementById("menuBtn")?.addEventListener("click", () => {
    state.sidebarOpen = !state.sidebarOpen;
    render();
  });
}

/**
 * 作用：绑定供应商物资表单的辅助交互。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行后，供应商物资表单会自动联动物资单位。
 */
function bindSupplierMaterialFormHelpers() {
  const form = document.getElementById("supplierMaterialForm");
  if (!form) return;
  const materialId = document.getElementById("materialId");
  const materialName = document.getElementById("materialName");
  const category = document.getElementById("materialCategory");
  const unit = document.getElementById("materialUnit");
  /**
   * 作用：完成 syncUnits 这一步前端处理。
   * 输入：
   * - 无输入参数。
   * 输出：无显式返回值。执行后，价格、库存和产能输入框旁边的单位会同步更新。
   */
  const syncUnits = () => {
    const selected = state.materialOptions.find((item) => String(item.id) === String(materialId?.value || ""));
    const currentUnit = String(unit?.value || selected?.unit || "").trim() || "单位";
    document.getElementById("priceUnit").textContent = `元/${currentUnit}`;
    document.getElementById("stockUnit").textContent = currentUnit;
    document.getElementById("capacityUnit").textContent = `${currentUnit}/日`;
  };
  materialId?.addEventListener("change", () => {
    const selected = state.materialOptions.find((item) => String(item.id) === String(materialId.value));
    if (selected) {
      materialName.value = "";
      category.value = selected.category || "";
      unit.value = selected.unit || "";
    }
    syncUnits();
  });
  materialName?.addEventListener("input", () => {
    if (materialName.value.trim() && materialId) materialId.value = "";
  });
  unit?.addEventListener("input", syncUnits);
  syncUnits();
}

/**
 * 作用：显示一条短提示消息。
 * 输入：
 * - message：要显示给用户看的提示文字。
 * 输出：无显式返回值。执行后，页面会出现一条短提示，并在几秒后自动隐藏。
 */
function showToast(message) {
  state.toast = message;
  render();
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    state.toast = "";
    render();
  }, 2600);
}

/**
 * 作用：根据当前 state 重新渲染页面。
 * 输入：
 * - 无输入参数。
 * 输出：无显式返回值。执行后，页面 HTML 会根据最新 state 重新生成。
 */
function render() {
  const app = document.getElementById("app");
  const focus = captureFocus();
  app.innerHTML = state.user ? appTemplate() : loginTemplate();
  bindEvents();
  restoreFocus(focus);
}

if (!window.MATERIAL_SKIP_AUTO_BOOTSTRAP) {
  restoreSession().finally(render);
}
