import assert from "node:assert/strict";
import test from "node:test";

const storage = new Map();
const sessionStorageItems = new Map();
globalThis.localStorage = {
  getItem(key) {
    return storage.get(key) ?? null;
  },
  setItem(key, value) {
    storage.set(key, String(value));
  },
  removeItem(key) {
    storage.delete(key);
  },
};
globalThis.sessionStorage = {
  getItem(key) {
    return sessionStorageItems.get(key) ?? null;
  },
  setItem(key, value) {
    sessionStorageItems.set(key, String(value));
  },
  removeItem(key) {
    sessionStorageItems.delete(key);
  },
};

globalThis.window = {
  MATERIAL_API_BASE: "http://localhost:8080",
  MATERIAL_SKIP_AUTO_BOOTSTRAP: true,
  location: {
    protocol: "http:",
    port: "5173",
  },
  clearTimeout() {},
  setTimeout() {
    return 1;
  },
};

globalThis.document = {
  activeElement: null,
  getElementById(id) {
    if (id === "app") return { innerHTML: "" };
    return null;
  },
  querySelectorAll() {
    return [];
  },
};

globalThis.FormData = class TestFormData {
  constructor(form) {
    this.form = form;
  }

  get(key) {
    return this.form[key] ?? null;
  }
};

const { state } = await import("./js/state.js");
const { savedLoginStorageKey, sessionTokenStorageKey, sessionUserStorageKey } = await import("./js/config.js");
const { appTemplate } = await import("./js/views.js");
const appModule = await import("./app.js");
const { handleRegister, selectPage } = appModule;

test("driver registration remembers the new driver credentials for the next login", async () => {
  storage.clear();
  sessionStorageItems.clear();
  state.user = null;
  state.token = "";
  state.savedLogin = null;
  state.authMode = "register";

  globalThis.fetch = async (url) => {
    if (url.endsWith("/auth/register")) {
      return jsonResponse({
        token: "driver-token",
        userId: 42,
        userType: "DRIVER",
        username: "driver-new",
        displayName: "新司机",
      });
    }
    return jsonResponse([]);
  };

  await handleRegister({
    preventDefault() {},
    currentTarget: {
      userType: "DRIVER",
      username: "driver-new",
      password: "secret123",
      displayName: "新司机",
      contactPhone: "13800009999",
    },
  });

  assert.deepEqual(state.savedLogin, {
    userType: "DRIVER",
    username: "driver-new",
    password: "secret123",
  });
  assert.deepEqual(JSON.parse(storage.get(savedLoginStorageKey)), state.savedLogin);
  assert.equal(state.token, "driver-token");
  assert.equal(state.user.username, "driver-new");
  assert.equal(sessionStorageItems.get(sessionTokenStorageKey), "driver-token");
  assert.equal(JSON.parse(sessionStorageItems.get(sessionUserStorageKey)).username, "driver-new");
});

test("page navigation closes the notification center", () => {
  state.page = "home";
  state.sidebarOpen = true;
  state.showNotifications = true;

  selectPage("profile");

  assert.equal(state.page, "profile");
  assert.equal(state.sidebarOpen, false);
  assert.equal(state.showNotifications, false);
});

test("notification center renders as a popover instead of leading page content", () => {
  state.user = { id: 42, userType: "DRIVER", username: "driver-new", displayName: "新司机" };
  state.page = "profile";
  state.showNotifications = true;
  state.notifications = [
    {
      type: "PUSH",
      title: "收到运输订单推送",
      content: "测试订单",
      status: "PENDING",
      createTime: "2026-06-08 11:20",
    },
  ];

  const html = appTemplate();

  assert.match(html, /class="notification-popover"/);
  assert.ok(html.indexOf('class="notification-popover"') > html.indexOf('<section class="content">'));
});

test("supplier home renders a persistent logout action in the topbar", () => {
  state.user = { id: 7, userType: "SUPPLIER", username: "supplier01", displayName: "供应商" };
  state.page = "home";
  state.showNotifications = false;
  state.notifications = [];
  state.supplierOrders = [];
  state.supplierMaterials = [];
  state.supplierOpenRfqs = [];
  state.supplierQuotes = [];

  const html = appTemplate();
  const topbarHtml = html.slice(html.indexOf('<header class="topbar">'), html.indexOf("</header>"));

  assert.match(topbarHtml, /data-logout/);
  assert.match(topbarHtml, /退出登录/);
});

test("supplier home exposes panic-buy orders for confirmation and shows ranking", () => {
  state.user = { id: 1, userType: "SUPPLIER", username: "supplier01", displayName: "Shanghai Reliable Supplier Co., Ltd." };
  state.page = "home";
  state.showNotifications = false;
  state.notifications = [];
  state.suppliers = [{
    id: 1,
    companyName: "Shanghai Reliable Supplier Co., Ltd.",
    contactName: "张经理",
    region: "华东",
    address: "上海市浦东新区临港物资园",
    rating: "4.8",
    certifications: ["应急物资保障单位"],
    materials: [{ id: 101, name: "P.O42.5 散装水泥", category: "水泥", stock: 1000, price: "500", unit: "吨" }],
  }];
  state.supplierOrders = [{
    id: "PO-PERF-PANIC-0001",
    purchaserId: 20,
    purchaserName: "压测采购方 perf_purchaser_0005",
    supplierId: 1,
    supplierName: "Shanghai Reliable Supplier Co., Ltd.",
    materialId: 101,
    materialName: "P.O42.5 散装水泥",
    category: "水泥",
    quantity: "1000 吨",
    amount: "¥ 500000",
    status: "采购方已抢购",
    source: "JMeter 高并发抢购压测",
    pushedTo: "采购方 20 已抢购成功，等待供应商确认",
  }];
  state.supplierRanking = [{
    supplierId: 1,
    companyName: "Shanghai Reliable Supplier Co., Ltd.",
    ratingScore: "4.8",
    rank: 1,
  }];
  state.deadLetters = [];

  const html = appTemplate();

  assert.match(html, /待确认供货/);
  assert.match(html, /data-confirm-supplier-order="PO-PERF-PANIC-0001"/);
  assert.match(html, /data-reject-supplier-order="PO-PERF-PANIC-0001"/);
  assert.match(html, /供应商履约排行榜/);
  assert.match(html, /#1/);
});

test("purchaser supplier and driver homes render the three rankings together", () => {
  state.fulfillmentRankings = {
    purchasers: [{ participantId: 1, displayName: "Shanghai Material Purchaser Co., Ltd.", ratingScore: "5", rank: 1 }],
    suppliers: [{ participantId: 1, displayName: "Shanghai Reliable Supplier Co., Ltd.", ratingScore: "4.8", rank: 1 }],
    drivers: [{ participantId: 1, displayName: "李师傅 · 沪A-8899", ratingScore: "4.7", rank: 1 }],
  };
  state.page = "home";
  state.showNotifications = false;
  state.notifications = [];
  state.suppliers = [];
  state.supplierOrders = [];
  state.supplierMaterials = [];
  state.supplierOpenRfqs = [];
  state.supplierQuotes = [];
  state.transportHall = [];
  state.driverOrders = [];
  state.follows = [];
  state.attendance = { online: true, date: "2026-06-11" };
  state.cart = [];

  [
    { id: 1, userType: "PURCHASER", username: "purchaser01", displayName: "采购方" },
    { id: 1, userType: "SUPPLIER", username: "supplier01", displayName: "供应商" },
    { id: 1, userType: "DRIVER", username: "driver01", displayName: "李师傅" },
  ].forEach((user) => {
    state.user = user;
    const html = appTemplate();

    assert.match(html, /采购方履约排行榜/);
    assert.match(html, /供应商履约排行榜/);
    assert.match(html, /司机履约排行榜/);
    assert.match(html, /Shanghai Material Purchaser Co., Ltd./);
    assert.match(html, /Shanghai Reliable Supplier Co., Ltd./);
    assert.match(html, /李师傅 · 沪A-8899/);
  });
});

test("purchaser supplier and admin homes render dispatch recommendations", () => {
  state.fulfillmentRankings = { purchasers: [], suppliers: [], drivers: [] };
  state.dispatchRecommendations = {
    orderId: "PO-TRANSPORT-001",
    items: [{
      driverId: 8,
      driverName: "李师傅",
      vehicleNo: "沪A-8899",
      vehicleType: "4.2米厢式货车",
      online: true,
      distanceToOriginKm: "0.21",
      ratingScore: "4.7",
      recommendScore: "166.79",
      reason: "在线 · 距发货地 0.21 KM · 评分 4.7",
      rank: 1,
    }],
  };
  state.page = "home";
  state.showNotifications = false;
  state.notifications = [];
  state.suppliers = [];
  state.purchaserOrders = [];
  state.supplierOrders = [];
  state.adminOrders = [];
  state.supplierMaterials = [];
  state.supplierOpenRfqs = [];
  state.supplierQuotes = [];
  state.transportHall = [];
  state.driverOrders = [];
  state.follows = [];
  state.cart = [];
  state.deadLetters = [];
  state.adminDashboard = {};

  [
    { id: 1, userType: "PURCHASER", username: "purchaser01", displayName: "采购方" },
    { id: 1, userType: "SUPPLIER", username: "supplier01", displayName: "供应商" },
    { id: 99, userType: "ADMIN", username: "admin01", displayName: "管理员" },
  ].forEach((user) => {
    state.user = user;
    const html = appTemplate();

    assert.match(html, /智能调度推荐/);
    assert.match(html, /PO-TRANSPORT-001/);
    assert.match(html, /李师傅/);
    assert.match(html, /0\.21 KM/);
    assert.match(html, /在线 · 距发货地 0\.21 KM · 评分 4\.7/);
  });
});

test("supplier qualification and admin audit render compliance controls", () => {
  state.user = { id: 7, userType: "SUPPLIER", username: "supplier01", displayName: "供应商" };
  state.page = "profile";
  state.notifications = [];
  state.supplierMaterials = [];
  state.supplierQualification = {
    companyName: "上海可靠应急供应链有限公司",
    contactName: "张经理",
    contactPhone: "13800000001",
    licenseNo: "LIC-UPDATED-0001",
    address: "上海市浦东新区临港物资园",
    auditStatus: "PENDING",
    auditStatusText: "待复核",
    auditRemark: "供应商资料已更新，待管理员复核",
    qualificationCompletion: 86,
    riskTags: ["缺少安全生产证明", "暂无上架物资"],
  };

  const supplierHtml = appTemplate();

  assert.match(supplierHtml, /id="supplierQualificationForm"/);
  assert.match(supplierHtml, /保存并提交复核/);
  assert.match(supplierHtml, /缺少安全生产证明/);

  state.user = { id: 99, userType: "ADMIN", username: "admin01", displayName: "管理员" };
  state.page = "suppliers";
  state.adminSuppliers = [{
    supplierId: 7,
    companyName: "上海可靠应急供应链有限公司",
    contactName: "张经理",
    contactPhone: "13800000001",
    licenseNo: "LIC-UPDATED-0001",
    address: "上海市浦东新区临港物资园",
    ratingScore: "4.8",
    status: 1,
    auditStatus: "待复核",
    auditStatusCode: "PENDING",
    auditRemark: "供应商资料已更新，待管理员复核",
    qualificationCompletion: 86,
    riskTags: ["缺少安全生产证明", "暂无上架物资"],
    materialCount: 0,
    stockQuantity: 0,
  }];

  const adminHtml = appTemplate();

  assert.match(adminHtml, /86%/);
  assert.match(adminHtml, /供应商资料已更新，待管理员复核/);
  assert.match(adminHtml, /data-admin-approve-supplier="7"/);
});

test("supplier qualification omits stale coordinates when address changes", async () => {
  assert.equal(typeof appModule.updateSupplierQualification, "function");
  state.user = { id: 1, userType: "SUPPLIER", username: "supplier01", displayName: "供应商" };
  state.token = "supplier-token";
  state.page = "profile";
  state.actionLoading = {};
  state.toast = "";
  state.supplierQualification = {
    companyName: "上海可靠应急供应链有限公司",
    contactName: "张经理",
    contactPhone: "13800000001",
    licenseNo: "LIC-001",
    address: "江苏省南京市",
    longitude: "118.84",
    latitude: "31.95",
  };

  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url: String(url), options });
    if (String(url).endsWith("/api/supplier/qualification") && options.method === "PUT") {
      return jsonResponse({ ...state.supplierQualification, address: "山东省济南市", longitude: "117.12", latitude: "36.65" });
    }
    return jsonResponse([]);
  };

  await appModule.updateSupplierQualification({
    preventDefault() {},
    currentTarget: {
      companyName: "上海可靠应急供应链有限公司",
      contactName: "张经理",
      contactPhone: "13800000001",
      licenseNo: "LIC-001",
      address: "山东省济南市",
      longitude: "118.84",
      latitude: "31.95",
      businessLicenseUrl: "",
      safetyCertUrl: "",
      insuranceCertUrl: "",
    },
  });

  const putCall = calls.find((call) => call.url.endsWith("/api/supplier/qualification") && call.options.method === "PUT");
  assert.ok(putCall);
  assert.deepEqual(JSON.parse(putCall.options.body), {
    companyName: "上海可靠应急供应链有限公司",
    contactName: "张经理",
    contactPhone: "13800000001",
    licenseNo: "LIC-001",
    address: "山东省济南市",
    businessLicenseUrl: "",
    safetyCertUrl: "",
    insuranceCertUrl: "",
  });
});

test("completed purchaser order renders acceptance action and modal", () => {
  state.user = { id: 1, userType: "PURCHASER", username: "purchaser01", displayName: "王主管" };
  state.page = "orders";
  state.showNotifications = false;
  state.acceptanceModal = null;
  state.purchaserOrders = [{
    id: "PO-ACCEPT-001",
    purchaserId: 1,
    purchaserName: "采购方",
    supplierId: 7,
    supplierName: "供应商",
    materialId: 101,
    materialName: "瓶装饮用水",
    category: "食品饮水",
    quantity: "80 箱",
    amount: "3184.00",
    status: "已完成",
    source: "询价报价已采纳",
    pushedTo: "订单已完成，等待采购方验收",
    driverId: 8,
    acceptanceStatus: "待验收",
    acceptanceSummary: "运输完成后由采购方验收签收",
    acceptanceProofUrl: "",
  }];

  const html = appTemplate();

  assert.match(html, /data-accept-order="PO-ACCEPT-001"/);
  assert.match(html, /待验收/);

  state.acceptanceModal = { order: state.purchaserOrders[0] };
  const modalHtml = appTemplate();

  assert.match(modalHtml, /id="acceptanceForm"/);
  assert.match(modalHtml, /验收签收/);
  assert.match(modalHtml, /提交验收/);
});

test("accepted purchaser order renders payment action and modal", () => {
  state.user = { id: 1, userType: "PURCHASER", username: "purchaser01", displayName: "王主管" };
  state.page = "orders";
  state.showNotifications = false;
  state.acceptanceModal = null;
  state.paymentModal = null;
  state.purchaserOrders = [{
    id: "PO-PAY-001",
    purchaserId: 1,
    purchaserName: "采购方",
    supplierId: 7,
    supplierName: "供应商",
    materialId: 101,
    materialName: "瓶装饮用水",
    category: "食品饮水",
    quantity: "80 箱",
    amount: "3184.00",
    status: "已完成",
    source: "询价报价已采纳",
    pushedTo: "已验收：王主管，数量和外观验收通过",
    driverId: 8,
    acceptanceStatus: "已验收",
    acceptanceSummary: "已验收 · 签收人 王主管 · 数量和外观验收通过",
    acceptanceProofUrl: "https://files.example.com/pod.pdf",
    paymentStatus: "待付款",
    paymentSummary: "验收完成后由采购方登记付款凭证",
    paymentProofUrl: "",
  }];

  const html = appTemplate();

  assert.match(html, /data-pay-order="PO-PAY-001"/);
  assert.match(html, /待付款/);

  state.paymentModal = { order: state.purchaserOrders[0] };
  const modalHtml = appTemplate();

  assert.match(modalHtml, /id="paymentForm"/);
  assert.match(modalHtml, /付款登记/);
  assert.match(modalHtml, /提交付款/);
});

test("payment timeout order renders warning without payment action", () => {
  state.user = { id: 1, userType: "PURCHASER", username: "purchaser01", displayName: "王主管" };
  state.page = "orders";
  state.showNotifications = false;
  state.acceptanceModal = null;
  state.paymentModal = null;
  state.purchaserOrders = [{
    id: "PO-TIMEOUT-001",
    purchaserId: 1,
    purchaserName: "采购方",
    supplierId: 7,
    supplierName: "供应商",
    materialId: 101,
    materialName: "瓶装饮用水",
    category: "食品饮水",
    quantity: "80 箱",
    amount: "3184.00",
    status: "已完成",
    source: "询价报价已采纳",
    pushedTo: "订单支付超时",
    driverId: 8,
    acceptanceStatus: "已验收",
    acceptanceSummary: "已验收 · 签收人 王主管 · 数量和外观验收通过",
    acceptanceProofUrl: "https://files.example.com/pod.pdf",
    paymentStatus: "支付超时",
    paymentSummary: "支付超时 · 付款单已超过1小时，请联系管理员重新开启付款",
    paymentProofUrl: "",
    paymentExpiresAt: "2026-06-08 16:00",
  }];

  const html = appTemplate();

  assert.match(html, /支付超时/);
  assert.doesNotMatch(html, /data-pay-order="PO-TIMEOUT-001"/);
});

test("purchase RFQ leaves blank coordinates for backend geocoding", async () => {
  assert.equal(typeof appModule.createPurchaseRfq, "function");
  state.user = null;
  state.token = "";
  state.selectedRfqId = null;
  state.page = "rfqs";
  state.actionLoading = {};
  state.toast = "";

  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url: String(url), options });
    if (String(url).endsWith("/api/purchase-rfqs")) {
      return jsonResponse({
        id: 81,
        materialName: "瓶装饮用水",
      });
    }
    return jsonResponse([]);
  };

  await appModule.createPurchaseRfq({
    preventDefault() {},
    currentTarget: {
      materialName: "瓶装饮用水",
      category: "食品饮水",
      unit: "箱",
      quantity: "80",
      deliveryAddress: "北京交通大学",
      longitude: "",
      latitude: "",
      remark: "",
    },
  });

  const postCall = calls.find((call) => call.url.endsWith("/api/purchase-rfqs"));
  assert.equal(calls.some((call) => call.url.startsWith("https://nominatim.openstreetmap.org/search")), false);
  assert.ok(postCall);
  assert.deepEqual(JSON.parse(postCall.options.body), {
    materialName: "瓶装饮用水",
    category: "食品饮水",
    unit: "箱",
    quantity: "80",
    deliveryAddress: "北京交通大学",
    remark: "",
  });
});

function jsonResponse(data) {
  return {
    ok: true,
    async json() {
      return { code: 200, message: "success", data };
    },
  };
}
