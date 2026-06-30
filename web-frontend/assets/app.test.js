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
const { appTemplate, loginTemplate } = await import("./js/views.js");
const appModule = await import("./app.js");
const { handleRegister, openTrackingModal, scrollRankingList, selectPage, uploadTransportLocation } = appModule;

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

test("login screen presents the emergency logistics start experience", () => {
  state.user = null;
  state.authMode = "login";
  state.savedLogin = null;
  state.authError = "";
  state.loginLoading = false;

  const html = loginTemplate();

  assert.match(html, /应急物资从采购到运输，一屏协同/);
  assert.match(html, /class="hero-map-card"/);
  assert.match(html, /运输追踪/);
  assert.match(html, /PO-BULK-00025736/);
  assert.match(html, /supplier01/);
  assert.match(html, /driver01/);
});

test("ranking scroll control moves the internal ranking window", () => {
  const originalQuerySelector = document.querySelector;
  const rankingWindow = {
    clientHeight: 240,
    scrollHeight: 760,
    scrollTop: 0,
    scrollTo({ top }) {
      this.scrollTop = top;
    },
  };
  document.querySelector = (selector) => (selector === '[data-ranking-window="drivers"]' ? rankingWindow : null);

  try {
    scrollRankingList("drivers");
    assert.equal(rankingWindow.scrollTop, 240);

    rankingWindow.scrollTop = 520;
    scrollRankingList("drivers");
    assert.equal(rankingWindow.scrollTop, 0);
  } finally {
    document.querySelector = originalQuerySelector;
  }
});

test("order cards expose a transport tracking action", () => {
  state.user = { id: 1, userType: "DRIVER", username: "driver01", displayName: "司机" };
  state.page = "home";
  state.showNotifications = false;
  state.notifications = [];
  state.fulfillmentRankings = { purchasers: [], suppliers: [], drivers: [] };
  state.dispatchRecommendations = { orderId: null, items: [] };
  state.transportHall = [];
  state.pushOrders = [];
  state.follows = [];
  state.attendance = { online: true, date: "2026-06-30" };
  state.driverOrders = [{
    id: "PO-TRACK-001",
    purchaserName: "上海应急采购中心",
    supplierName: "上海可靠供应商",
    materialName: "净水设备",
    category: "净水",
    quantity: "20 台",
    amount: "20000",
    status: "司机已接单",
    source: "供应商确认供货",
    pushedTo: "司机 1 已抢单",
    originAddress: "上海供应商仓库",
    originLongitude: "121.0736",
    originLatitude: "31.0736",
    destinationAddress: "上海应急采购中心",
    destinationLongitude: "121.4700",
    destinationLatitude: "31.2300",
  }];

  const html = appTemplate();

  assert.match(html, /data-order-tracking="PO-TRACK-001"/);
  assert.match(html, /运输追踪/);
});

test("openTrackingModal loads tracking data and renders route nodes with timeline", async () => {
  assert.equal(typeof openTrackingModal, "function");
  state.user = { id: 1, userType: "DRIVER", username: "driver01", displayName: "司机" };
  state.token = "driver-token";
  state.page = "home";
  state.showNotifications = false;
  state.notifications = [];
  state.fulfillmentRankings = { purchasers: [], suppliers: [], drivers: [] };
  state.dispatchRecommendations = { orderId: null, items: [] };
  state.transportHall = [];
  state.pushOrders = [];
  state.follows = [];
  state.attendance = { online: true, date: "2026-06-30" };
  state.driverOrders = [{
    id: "PO-TRACK-001",
    purchaserName: "上海应急采购中心",
    supplierName: "上海可靠供应商",
    materialName: "净水设备",
    category: "净水",
    quantity: "20 台",
    amount: "20000",
    status: "司机已接单",
    source: "供应商确认供货",
    pushedTo: "司机 1 已抢单",
  }];

  const calls = [];
  globalThis.fetch = async (url) => {
    calls.push(String(url));
    if (String(url).endsWith("/api/transport-orders/PO-TRACK-001/tracking")) {
      return jsonResponse({
        orderId: "PO-TRACK-001",
        status: "司机已接单",
        driverId: 1,
        originAddress: "上海供应商仓库",
        originLongitude: "121.0736",
        originLatitude: "31.0736",
        destinationAddress: "上海应急采购中心",
        destinationLongitude: "121.4700",
        destinationLatitude: "31.2300",
        locationReports: [{
          id: 7,
          orderId: "PO-TRACK-001",
          driverId: 1,
          longitude: "121.473701",
          latitude: "31.230416",
          remark: "到达中转点",
          createdAt: "2026-06-30 15:40",
        }],
        timeline: [{
          id: 1,
          orderId: "PO-TRACK-001",
          status: "司机已接单",
          action: "司机抢运输单预占成功",
          operatorType: "DRIVER",
          operatorId: 1,
          remark: "Redis Lua 已完成运力名额预占",
          createdAt: "2026-06-30 15:20",
        }],
      });
    }
    return jsonResponse([]);
  };

  await openTrackingModal("PO-TRACK-001");

  assert.ok(calls.some((url) => url.endsWith("/api/transport-orders/PO-TRACK-001/tracking")));
  assert.equal(state.trackingModal.tracking.orderId, "PO-TRACK-001");
  const html = appTemplate();

  assert.match(html, /id="trackingTitle"/);
  assert.match(html, /运输追踪/);
  assert.match(html, /上海供应商仓库/);
  assert.match(html, /121\.0736/);
  assert.match(html, /上海应急采购中心/);
  assert.match(html, /121\.4700/);
  assert.match(html, /司机上传节点/);
  assert.match(html, /到达中转点/);
  assert.match(html, /121\.473701/);
  assert.match(html, /31\.230416/);
  assert.match(html, /司机抢运输单预占成功/);
});

test("uploadTransportLocation uses browser geolocation and posts the driver node", async () => {
  assert.equal(typeof uploadTransportLocation, "function");
  const originalNavigator = globalThis.navigator;
  Object.defineProperty(globalThis, "navigator", {
    configurable: true,
    value: {
      geolocation: {
        getCurrentPosition(resolve) {
          resolve({
            coords: {
              longitude: 121.473701,
              latitude: 31.230416,
            },
          });
        },
      },
    },
  });

  state.user = { id: 1, userType: "DRIVER", username: "driver01", displayName: "司机" };
  state.token = "driver-token";
  state.page = "home";
  state.actionLoading = {};
  state.toast = "";
  state.trackingModal = null;
  state.driverOrders = [{
    id: "PO-TRACK-001",
    status: "运输中",
    materialName: "净水设备",
  }];

  const calls = [];
  globalThis.fetch = async (url, options = {}) => {
    calls.push({ url: String(url), options });
    if (String(url).endsWith("/api/transport-orders/PO-TRACK-001/location")) {
      return jsonResponse({
        id: 7,
        orderId: "PO-TRACK-001",
        driverId: 1,
        longitude: "121.473701",
        latitude: "31.230416",
        remark: "到达运输节点",
        createdAt: "2026-06-30 15:40",
      });
    }
    if (String(url).endsWith("/api/transport-orders/mine")) return jsonResponse(state.driverOrders);
    if (String(url).endsWith("/api/drivers/attendance/today")) return jsonResponse({ online: true, date: "2026-06-30" });
    if (String(url).endsWith("/api/rankings/fulfillment")) return jsonResponse({ purchasers: [], suppliers: [], drivers: [] });
    return jsonResponse([]);
  };

  try {
    await uploadTransportLocation("PO-TRACK-001");
  } finally {
    Object.defineProperty(globalThis, "navigator", { configurable: true, value: originalNavigator });
  }

  const postCall = calls.find((call) => call.url.endsWith("/api/transport-orders/PO-TRACK-001/location"));
  assert.ok(postCall);
  assert.equal(postCall.options.method, "POST");
  assert.deepEqual(JSON.parse(postCall.options.body), {
    longitude: 121.473701,
    latitude: 31.230416,
    remark: "到达运输节点",
  });
  assert.equal(state.toast, "到达节点已上传");
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

test("topbar and profile render role identity avatars instead of plain initials", () => {
  [
    { userType: "PURCHASER", username: "purchaser01", displayName: "采购方", roleClass: "role-avatar--purchaser", mark: "采", label: "采购方" },
    { userType: "SUPPLIER", username: "supplier01", displayName: "供应商", roleClass: "role-avatar--supplier", mark: "供", label: "供应商" },
    { userType: "DRIVER", username: "driver01", displayName: "李师傅", roleClass: "role-avatar--driver", mark: "运", label: "司机" },
    { userType: "ADMIN", username: "admin01", displayName: "管理员", roleClass: "role-avatar--admin", mark: "管", label: "平台管理员" },
  ].forEach((user, index) => {
    state.user = { id: index + 1, userType: user.userType, username: user.username, displayName: user.displayName };
    state.page = "profile";
    state.showNotifications = false;
    state.notifications = [];

    const html = appTemplate();

    assert.match(html, new RegExp(user.roleClass));
    assert.equal((html.match(new RegExp(user.roleClass, "g")) || []).length, 2);
    assert.match(html, new RegExp(`aria-label="${user.label}身份标识"`));
    assert.match(html, new RegExp(`<span class="role-avatar-mark">${user.mark}</span>`));
  });
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

test("ranking panel renders all top ten accounts as avatar rows with internal scrolling", () => {
  const drivers = Array.from({ length: 12 }, (_, index) => ({
    participantId: index + 1,
    displayName: `司机 ${index + 1}`,
    ratingScore: (5 - index * 0.05).toFixed(2),
    rank: index + 1,
  }));
  state.fulfillmentRankings = {
    purchasers: [],
    suppliers: [],
    drivers,
  };
  state.page = "home";
  state.showNotifications = false;
  state.notifications = [];
  state.transportHall = [];
  state.driverOrders = [];
  state.follows = [];
  state.attendance = { online: true, date: "2026-06-11" };
  state.user = { id: 1, userType: "DRIVER", username: "driver01", displayName: "李师傅" };

  const html = appTemplate();
  const driverColumn = html.match(/<section class="ranking-column" data-ranking-key="drivers">[\s\S]*?<\/section>/)?.[0] ?? "";

  assert.match(driverColumn, /data-ranking-window="drivers"/);
  assert.match(driverColumn, /data-visible-count="6"/);
  assert.match(driverColumn, /data-ranking-count="10"/);
  assert.match(driverColumn, /data-scroll-ranking="drivers"/);
  assert.equal((driverColumn.match(/data-ranking-entry="drivers"/g) || []).length, 10);
  assert.equal((driverColumn.match(/class="ranking-avatar ranking-avatar--driver/g) || []).length, 10);
  assert.match(driverColumn, /#10/);
  assert.match(driverColumn, /司机 10/);
  assert.doesNotMatch(driverColumn, /#11/);
  assert.doesNotMatch(driverColumn, /#12/);
  assert.doesNotMatch(driverColumn, /ranking-leader|ranking-crown/);
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
