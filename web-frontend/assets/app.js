import { apiBase, cartStorageKey, savedLoginStorageKey } from "./js/config.js";
import { state } from "./js/state.js";
import { allKnownOrders, filteredSuppliers, selectedSupplier } from "./js/selectors.js";
import { captureFocus, escapeHtml, restoreFocus } from "./js/utils.js";
import {
  appTemplate,
  defaultAuthUsername,
  defaultReviewText,
  loginTemplate,
} from "./js/views.js";

function saveCart() {
  localStorage.setItem(cartStorageKey, JSON.stringify(state.cart));
}

function setCart(nextCart) {
  state.cart = nextCart;
  saveCart();
}

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

async function applySupplierFilters(nextFilters = {}) {
  state.supplierFilters = { ...state.supplierFilters, ...nextFilters };
  const suppliers = filteredSuppliers();
  if (suppliers.length && !suppliers.some((supplier) => supplier.id === state.selectedSupplierId)) {
    state.selectedSupplierId = suppliers[0].id;
    state.supplierStore = null;
  }
  render();
  if (state.user?.userType === "PURCHASER" && state.selectedSupplierId && !state.supplierStore) {
    try {
      await loadSupplierStore();
    } catch (error) {
      showToast(error.message || "店铺详情加载失败");
    }
    render();
  }
}

function normalizeUser(data) {
  return {
    id: data.id ?? data.userId,
    userType: data.userType,
    username: data.username,
    displayName: data.displayName,
  };
}

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

async function loadRoleData() {
  if (!state.user) return;
  state.loading = true;
  render();
  try {
    const [suppliers, ranking, notifications] = await Promise.all([
      requestJson("/api/suppliers/catalog"),
      requestJson("/api/suppliers/ranking"),
      requestJson("/api/notifications"),
    ]);
    state.suppliers = suppliers;
    state.supplierRanking = ranking;
    state.notifications = notifications;
    if (!state.selectedSupplierId && state.suppliers[0]) state.selectedSupplierId = state.suppliers[0].id;
    if (state.user.userType === "PURCHASER") {
      const [orders, nearby] = await Promise.all([
        requestJson("/api/purchase-orders/mine"),
        requestJson("/api/suppliers/nearby?longitude=121.47&latitude=31.23&radiusKm=500"),
      ]);
      state.purchaserOrders = orders;
      state.nearbySuppliers = nearby;
      await loadSupplierStore();
    }
    if (state.user.userType === "SUPPLIER") {
      const [orders, materials, options] = await Promise.all([
        requestJson("/api/supplier/orders"),
        requestJson("/api/supplier/materials"),
        requestJson("/api/materials/options"),
      ]);
      state.supplierOrders = orders;
      state.supplierMaterials = materials;
      state.materialOptions = options;
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
    }
  } catch (error) {
    showToast(error.message || "业务数据加载失败");
  } finally {
    state.loading = false;
    render();
  }
}

async function loadSupplierStore() {
  if (!state.selectedSupplierId) return;
  state.supplierStore = await requestJson(`/api/suppliers/${state.selectedSupplierId}/store`);
}

async function handleLogin(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const userType = String(form.get("userType") || "").trim();
  const username = String(form.get("username") || "").trim();
  const password = String(form.get("password") || "").trim();
  const rememberPassword = form.get("rememberPassword") === "on";
  if (!username || !password) {
    showToast("请输入用户名和密码");
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
    localStorage.setItem("material_token", state.token);
    localStorage.setItem("material_user", JSON.stringify(state.user));
    if (rememberPassword) {
      state.savedLogin = { userType, username, password };
      localStorage.setItem(savedLoginStorageKey, JSON.stringify(state.savedLogin));
    } else {
      state.savedLogin = null;
      localStorage.removeItem(savedLoginStorageKey);
    }
    await loadRoleData();
    showToast("登录成功，已进入对应角色工作台");
  } catch (error) {
    showToast(error.message || "登录失败，请检查账号和密码");
  } finally {
    state.loginLoading = false;
    render();
  }
}

async function handleRegister(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const userType = String(form.get("userType") || "").trim();
  const username = String(form.get("username") || "").trim();
  const password = String(form.get("password") || "").trim();
  const displayName = String(form.get("displayName") || "").trim();
  const contactPhone = String(form.get("contactPhone") || "").trim();
  if (!username || !password || !displayName || !contactPhone) {
    showToast("请填写注册信息");
    return;
  }

  state.loginLoading = true;
  render();
  try {
    const data = await requestJson("/auth/register", {
      method: "POST",
      body: JSON.stringify({ userType, username, password, displayName, contactPhone }),
    });
    state.token = data.token;
    state.user = normalizeUser(data);
    state.page = "home";
    state.authMode = "login";
    localStorage.setItem("material_token", state.token);
    localStorage.setItem("material_user", JSON.stringify(state.user));
    await loadRoleData();
    showToast("注册成功，已自动登录");
  } catch (error) {
    showToast(error.message || "注册失败");
  } finally {
    state.loginLoading = false;
    render();
  }
}

function clearSession() {
  localStorage.removeItem("material_token");
  localStorage.removeItem("material_user");
  state.token = "";
  state.user = null;
  state.page = "home";
}

async function logout() {
  try {
    if (state.token) await requestJson("/auth/logout", { method: "DELETE" });
  } catch (error) {
    showToast("退出时后端未响应，本地登录状态已清理");
  }
  clearSession();
  render();
}

async function restoreSession() {
  if (!state.token) {
    clearSession();
    return;
  }
  try {
    const data = await requestJson("/auth/me");
    state.user = normalizeUser(data);
    localStorage.setItem("material_user", JSON.stringify(state.user));
    await loadRoleData();
  } catch (error) {
    clearSession();
  }
}

async function createPurchaseOrder(materialId) {
  await runAction(`create-order:${materialId}`, async () => {
    const supplier = selectedSupplier();
    try {
      const order = await requestJson("/api/purchase-orders", {
        method: "POST",
        body: JSON.stringify({
          supplierId: supplier.id,
          materialId,
          quantity: "100 吨",
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

function updateCartQuantity(materialId, quantity) {
  const item = state.cart.find((cartItem) => cartItem.materialId === materialId);
  if (!item) return;
  item.quantity = quantity.trim() || `100 ${item.unit}`;
  saveCart();
}

function removeCartItem(materialId) {
  setCart(state.cart.filter((item) => item.materialId !== materialId));
  render();
}

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

async function createSupplierMaterial(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    await requestJson("/api/supplier/materials", {
      method: "POST",
      body: JSON.stringify({
        materialId: Number(form.get("materialId")),
        supplyPrice: Number(form.get("supplyPrice")),
        stockQuantity: Number(form.get("stockQuantity")),
        dailyCapacity: Number(form.get("dailyCapacity")),
        deliveryRadiusKm: Number(form.get("deliveryRadiusKm")),
        status: 1,
      }),
    });
    showToast("供货物资已保存，供应商目录缓存已删除");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "保存供货物资失败");
  }
}

async function updateSupplierMaterial(materialId) {
  const material = state.supplierMaterials.find((item) => item.id === materialId);
  if (!material) return;
  try {
    await requestJson(`/api/supplier/materials/${materialId}`, {
      method: "PUT",
      body: JSON.stringify({
        materialId: material.materialId,
        supplyPrice: Number(document.querySelector(`[data-edit-price="${materialId}"]`)?.value || material.supplyPrice),
        stockQuantity: Number(document.querySelector(`[data-edit-stock="${materialId}"]`)?.value || material.stockQuantity),
        dailyCapacity: Number(document.querySelector(`[data-edit-capacity="${materialId}"]`)?.value || material.dailyCapacity),
        deliveryRadiusKm: material.deliveryRadiusKm,
        status: 1,
      }),
    });
    showToast("供应物资已更新，Cache Aside 写路径已触发");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "更新失败");
  }
}

async function offlineSupplierMaterial(materialId) {
  try {
    await requestJson(`/api/supplier/materials/${materialId}/offline`, { method: "POST" });
    showToast("物资已下架，采购方供应商大厅会刷新");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "下架失败");
  }
}

async function markAttendance(online) {
  try {
    state.attendance = await requestJson(`/api/drivers/attendance?online=${online}`, { method: "POST" });
    showToast(online ? "已上线，BitMap 出勤位已写入" : "已离线，BitMap 出勤位已清理");
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "更新出勤失败");
  }
}

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

async function retryPushRecords() {
  try {
    const count = await requestJson("/api/order-push/retry", { method: "POST" });
    showToast(`补偿完成，新增 ${count} 条推送记录`);
    await loadRoleData();
  } catch (error) {
    showToast(error.message || "补偿失败");
  }
}

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

function openReviewModal(orderId) {
  const order = allKnownOrders()
    .find((item) => item.id === orderId);
  if (!order) return;
  state.reviewModal = { order };
  render();
}

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

function closeReviewModal() {
  state.reviewModal = null;
  render();
}

function closeTimelineModal() {
  state.timelineModal = null;
  render();
}

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

function bindEvents() {
  const loginForm = document.getElementById("loginForm");
  if (loginForm) {
    loginForm.addEventListener("submit", state.authMode === "login" ? handleLogin : handleRegister);
    document.querySelectorAll("[data-auth-mode]").forEach((element) => {
      element.addEventListener("click", () => {
        state.authMode = element.dataset.authMode;
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
      state.page = element.dataset.page;
      state.sidebarOpen = false;
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
  document.getElementById("supplierMaterialForm")?.addEventListener("submit", createSupplierMaterial);
  document.getElementById("reviewForm")?.addEventListener("submit", submitReview);
  document.querySelectorAll("[data-close-review]").forEach((element) => {
    element.addEventListener("click", (event) => {
      if (event.target === element) closeReviewModal();
    });
  });
  document.querySelectorAll("[data-close-timeline]").forEach((element) => {
    element.addEventListener("click", (event) => {
      if (event.target === element) closeTimelineModal();
    });
  });
  document.getElementById("refreshData")?.addEventListener("click", loadRoleData);
  document.getElementById("logoutBtn")?.addEventListener("click", logout);
  document.getElementById("menuBtn")?.addEventListener("click", () => {
    state.sidebarOpen = !state.sidebarOpen;
    render();
  });
}

function showToast(message) {
  state.toast = message;
  render();
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    state.toast = "";
    render();
  }, 2600);
}

function render() {
  const app = document.getElementById("app");
  const focus = captureFocus();
  app.innerHTML = state.user ? appTemplate() : loginTemplate();
  bindEvents();
  restoreFocus(focus);
}

restoreSession().finally(render);
