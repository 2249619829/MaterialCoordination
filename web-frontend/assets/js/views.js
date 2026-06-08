import { icons, orderStatusFlow, roleMeta } from "./config.js";
import { state } from "./state.js";
import { escapeHtml } from "./utils.js";
import {
  cartTotalAmount,
  filteredSuppliers,
  notificationClass,
  selectedSupplier,
  supplierCategories,
  supplierMinPrice,
  supplierTotalStock,
  unreadNotificationCount,
} from "./selectors.js";

export function loginTemplate() {
  return `
    <main class="login-screen">
      <section class="login-hero">
        <div class="brand">
          <div class="brand-mark">M</div>
          <span>物资协同平台</span>
        </div>
        <div class="login-copy">
          <h1>采购、供货与运输协同的一体化平台</h1>
          <p>采购方浏览供应商资质并确认购货订单，订单进入平台大厅并基于关注关系推送给司机。</p>
        </div>
        <div class="login-metrics">
          <div class="metric-tile"><strong>3</strong><span>角色工作台</span></div>
          <div class="metric-tile"><strong>Redis</strong><span>Token 登录态</span></div>
          <div class="metric-tile"><strong>Nacos</strong><span>服务注册发现</span></div>
        </div>
      </section>
      <section class="login-panel-wrap">
        <form class="login-panel" id="loginForm">
          <h2>${state.authMode === "login" ? "登录工作台" : "注册账号"}</h2>
          <div class="auth-tabs">
            <button type="button" class="${state.authMode === "login" ? "active" : ""}" data-auth-mode="login">登录</button>
            <button type="button" class="${state.authMode === "register" ? "active" : ""}" data-auth-mode="register">注册</button>
          </div>
          <div class="muted">${state.authMode === "login" ? "切换角色后会进入不同业务页面。" : "注册成功后会自动登录并进入对应角色页面。"}</div>
          <div class="form-grid">
            <div class="field">
              <label for="userType">用户类型</label>
              <select id="userType" name="userType">
                <option value="SUPPLIER">供应商</option>
                <option value="PURCHASER">采购方</option>
                <option value="DRIVER">司机</option>
                <option value="ADMIN">平台管理员</option>
              </select>
            </div>
            <div class="field">
              <label for="username">用户名</label>
              <input id="username" name="username" autocomplete="username" value="${defaultAuthUsername("SUPPLIER")}" />
            </div>
            <div class="field">
              <label for="password">密码</label>
              <input id="password" name="password" type="password" autocomplete="current-password" value="123456" />
            </div>
            ${
              state.authMode === "register"
                ? `
                  <div class="field">
                    <label for="displayName">名称</label>
                    <input id="displayName" name="displayName" autocomplete="organization" value="${defaultRegisterDisplayName("SUPPLIER")}" />
                  </div>
                  <div class="field">
                    <label for="contactPhone">联系电话</label>
                    <input id="contactPhone" name="contactPhone" autocomplete="tel" value="13800000000" />
                  </div>
                `
                : ""
            }
            <button class="btn btn-primary" type="submit" ${state.loginLoading ? "disabled" : ""}>
              ${state.loginLoading ? "处理中..." : state.authMode === "login" ? "登录" : "注册并登录"}
            </button>
          </div>
          <div class="muted" style="margin-top:18px">
            ${state.authMode === "login" ? "供应商 supplier01 / 采购方 purchaser01 / 司机 driver01 / 管理员 admin01，密码均为 123456。" : "注册会写入对应账号表和资料表，密码会加密存储。"}
          </div>
        </form>
      </section>
    </main>
  `;
}

export function appTemplate() {
  const meta = roleMeta[state.user.userType] || roleMeta.SUPPLIER;
  const pageTitle = meta.nav.find(([id]) => id === state.page)?.[1] || meta.nav[0][1];
  return `
    <div class="app-shell">
      <aside class="sidebar ${state.sidebarOpen ? "open" : ""}" id="sidebar">
        <div class="brand">
          <div class="brand-mark">M</div>
          <span>物资协同平台</span>
        </div>
        <nav class="nav">
          ${meta.nav
            .map(
              ([id, label]) => `
                <button class="${state.page === id ? "active" : ""}" data-page="${id}">
                  <span>${label}</span>
                </button>
              `,
            )
            .join("")}
        </nav>
        <div class="sidebar-card">
          <strong>${meta.label}模式</strong>
          <div class="muted">当前权限自动通过，后续可在网关和服务层补细粒度 RBAC。</div>
        </div>
      </aside>
      <main class="main">
        <header class="topbar">
          <button class="btn btn-ghost btn-sm mobile-menu" id="menuBtn" aria-label="打开导航">${icons.menu}</button>
          <div class="page-title">
            <h1>${pageTitle}</h1>
            <span>${escapeHtml(state.user.displayName)} · ${meta.label}</span>
          </div>
          <button class="notification-btn ${unreadNotificationCount() ? "active" : ""}" id="notificationBtn" type="button">
            <span>消息</span>
            <strong>${state.notifications.length}</strong>
          </button>
          <div class="user-chip">
            <div>
              <strong>${escapeHtml(state.user.username)}</strong>
              <div class="muted">在线 · Token 鉴权</div>
            </div>
            <div class="avatar">${escapeHtml(state.user.displayName).slice(0, 1)}</div>
          </div>
        </header>
        ${state.showNotifications ? notificationCenterPanel() : ""}
        <section class="content">
          ${renderRoleContent()}
        </section>
      </main>
      <div class="toast ${state.toast ? "show" : ""}">${escapeHtml(state.toast)}</div>
      ${reviewModalTemplate()}
      ${timelineModalTemplate()}
    </div>
  `;
}

export function renderRoleContent() {
  if (state.loading) return '<div class="panel"><div class="panel-body empty">加载中...</div></div>';
  if (state.user.userType === "ADMIN") return renderAdminContent();
  if (state.user.userType === "PURCHASER") return renderPurchaserContent();
  if (state.user.userType === "DRIVER") return renderDriverContent();
  return renderSupplierContent();
}

export function renderAdminContent() {
  if (state.page === "suppliers") {
    return adminSupplierPanel();
  }
  if (state.page === "orders") {
    return ordersPanel("订单监控", "平台管理员查看最近 50 条订单，识别异常状态和履约进度。", state.adminOrders);
  }
  if (state.page === "profile") return profilePanel("管理员资料", "供应商审核、订单监控、MQ 异常补偿和运营大盘。");

  const dashboard = state.adminDashboard || {};
  return `
    <div class="dashboard-grid">
      ${statCard("供应商", dashboard.supplierCount ?? 0, "资质审核")}
      ${statCard("采购方", dashboard.purchaserCount ?? 0, "需求发布")}
      ${statCard("司机", dashboard.driverCount ?? 0, "运力池")}
      ${statCard("订单总数", dashboard.orderCount ?? 0, "全链路监控")}
    </div>
    <div class="dashboard-grid">
      ${statCard("待供应商确认", dashboard.waitingSupplierConfirmCount ?? 0, "供货确认")}
      ${statCard("待司机接单", dashboard.waitingDriverCount ?? 0, "推拉结合")}
      ${statCard("运输中", dashboard.transportingCount ?? 0, "履约在途")}
      ${statCard("异常/死信", dashboard.abnormalCount ?? 0, "需要处理")}
    </div>
    ${adminOpsPanel(dashboard)}
    ${ordersPanel("最新订单", "运营端聚合采购、供货和运输状态。", state.adminOrders)}
  `;
}

export function renderPurchaserContent() {
  if (state.page === "orders") {
    return ordersPanel("我的采购订单", "采购方和供应商都能看到自己的订单状态。", state.purchaserOrders);
  }
  if (state.page === "drivers") {
    return `
      <div class="panel">
        <div class="panel-head"><div><h2>司机关注关系</h2><div class="muted">采购方确认订单后，会推送给关注关系内的司机。</div></div></div>
        <div class="panel-body timeline">
          <div class="empty">司机关注关系由司机端维护，采购方订单创建后会自动推送给相关司机。</div>
        </div>
      </div>
    `;
  }
  if (state.page === "profile") return profilePanel("采购方资料", "采购需求发布、供应商沟通、订单状态跟踪。");

  const suppliers = filteredSuppliers();
  const supplier = selectedSupplier();
  return `
    ${rankingPanel()}
    ${nearbySuppliersPanel()}
    ${supplierFiltersPanel()}
    <div class="layout-2">
      <div class="panel">
        <div class="panel-head">
          <div>
            <h2>供应商大厅</h2>
            <div class="muted">像菜单一样查看资质、评分和供应货物，确认后生成采购订单。</div>
          </div>
        </div>
        <div class="panel-body supplier-grid">
          ${suppliers.length ? suppliers.map(renderSupplierCard).join("") : '<div class="empty">没有匹配的供应商，换个关键词或筛选条件试试。</div>'}
        </div>
      </div>
      <aside class="panel detail-drawer">
        ${supplierStorePanel(supplier)}
      </aside>
    </div>
    ${cartPanel()}
  `;
}

export function renderSupplierContent() {
  const self = state.suppliers.find((supplier) => supplier.id === state.user.id) || state.suppliers[0];
  if (state.page === "materials") {
    return supplierMaterialManager();
  }
  if (state.page === "orders") {
    return ordersPanel("我的供货订单", "只展示属于当前供应商的订单状态。", state.supplierOrders);
  }
  if (state.page === "profile") return profilePanel("企业资质", "营业执照、履约评分、服务区域和供货能力。");

  return `
    <div class="dashboard-grid">
      ${statCard("供应货物", self?.materials.length || 0, "采购方可见")}
      ${statCard("供货订单", state.supplierOrders.length, "待备货 / 待运输")}
      ${statCard("履约评分", self?.rating || "96.8", "平台评级")}
      ${statCard("资质数量", self?.certifications.length || 0, "已展示")}
    </div>
    ${mqPanel()}
    ${ordersPanel("最新供货订单", "采购方确认购货后，供应商侧同步可见。", state.supplierOrders)}
  `;
}

export function supplierMaterialManager() {
  const firstOption = state.materialOptions[0];
  return `
    <div class="layout-2">
      <div class="panel">
        <div class="panel-head">
          <div><h2>供应货物管理</h2><div class="muted">新增或更新物资后会删除供应商目录缓存，并执行延迟双删。</div></div>
          <button class="btn btn-ghost btn-sm" id="refreshData">刷新</button>
        </div>
        <div class="panel-body menu-list">
          ${
            state.supplierMaterials.length
              ? state.supplierMaterials.map(renderManagedMaterial).join("")
              : '<div class="empty">暂无供应物资，可以从右侧新增。</div>'
          }
        </div>
      </div>
      <aside class="panel detail-drawer">
        <div class="panel-head"><div><h2>新增供货物资</h2><div class="muted">选择基础物资后填写供应能力。</div></div></div>
        <form class="panel-body form-grid" id="supplierMaterialForm">
          <div class="field">
            <label for="materialId">物资</label>
            <select id="materialId" name="materialId">
              ${state.materialOptions.map((item) => `<option value="${item.id}">${escapeHtml(item.materialName)} / ${escapeHtml(item.unit)}</option>`).join("")}
            </select>
          </div>
          <div class="field">
            <label for="supplyPrice">价格</label>
            <input id="supplyPrice" name="supplyPrice" type="number" min="0" step="0.01" value="860" />
          </div>
          <div class="field">
            <label for="stockQuantity">库存</label>
            <input id="stockQuantity" name="stockQuantity" type="number" min="0" step="1" value="300" />
          </div>
          <div class="field">
            <label for="dailyCapacity">日产能</label>
            <input id="dailyCapacity" name="dailyCapacity" type="number" min="0" step="1" value="80" />
          </div>
          <div class="field">
            <label for="deliveryRadiusKm">配送半径 KM</label>
            <input id="deliveryRadiusKm" name="deliveryRadiusKm" type="number" min="0" step="0.01" value="180" />
          </div>
          <button class="btn btn-primary" type="submit" ${firstOption ? "" : "disabled"}>新增 / 重新上架</button>
        </form>
      </aside>
    </div>
  `;
}

export function renderDriverContent() {
  if (state.page === "push") {
    return ordersPanel("推送订单", "来自关注采购方和采购方关注司机的推送。", state.pushOrders, true);
  }
  if (state.page === "follows") {
    return `
      <div class="panel">
        <div class="panel-head"><div><h2>关注采购方</h2><div class="muted">关注后更容易收到该采购方订单推送。</div></div></div>
        <div class="panel-body timeline">
          ${followRows(true)}
        </div>
      </div>
    `;
  }
  if (state.page === "profile") return profilePanel("车辆资料", "出勤状态、车辆能力和运输评分。");

  return `
    <div class="dashboard-grid">
      ${statCard("平台大厅订单", state.transportHall.length, "司机主动拉取")}
      ${statCard("我的运输单", state.driverOrders.length, "接单 / 在途")}
      ${statCard("关注采购方", state.follows.filter((item) => item.followedByDriver).length, "可继续扩展")}
      ${statCard("出勤状态", state.attendance?.online ? "在线" : "离线", "Redis BitMap")}
    </div>
    ${driverAttendancePanel()}
    ${ordersPanel("我的运输订单", "司机接单后在这里推进运输中和已完成状态。", state.driverOrders, false)}
    ${ordersPanel("运输订单大厅", "订单进入平台大厅后，司机可以主动抢单。", state.transportHall, true)}
  `;
}

export function renderSupplierCard(supplier) {
  const minPrice = supplierMinPrice(supplier);
  const materialSummary = supplier.materials.slice(0, 3).map((item) => item.name).join(" / ");
  return `
    <article class="order-card supplier-card ${state.selectedSupplierId === supplier.id ? "urgent" : ""}" data-supplier="${supplier.id}">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(supplier.companyName)}</strong>
          <span>${escapeHtml(supplier.region)} · ${escapeHtml(supplier.address)}</span>
        </div>
        <span class="chip green">${escapeHtml(supplier.rating)} 分</span>
      </div>
      <div class="supplier-stats">
        <span><strong>${supplier.materials.length}</strong> 类物资</span>
        <span><strong>${supplierTotalStock(supplier)}</strong> 可供库存</span>
        <span><strong>${minPrice !== Number.MAX_SAFE_INTEGER ? `¥${minPrice}` : "议价"}</strong> 起供价</span>
      </div>
      <div class="order-meta">
        ${supplier.certifications.slice(0, 3).map((item) => `<span class="chip">${escapeHtml(item)}</span>`).join("")}
      </div>
      <div class="muted">${escapeHtml(materialSummary || "暂无上架物资")}</div>
      <button class="btn btn-ghost btn-sm" data-supplier="${supplier.id}">查看资质与菜单</button>
    </article>
  `;
}

export function renderPurchaserMaterial(material) {
  const inCart = state.cart.some((item) => item.materialId === material.id);
  return `
    <article class="task-card menu-item-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(material.name)}</strong>
          <span>${escapeHtml(material.category)} · ${escapeHtml(material.stock)} · ${escapeHtml(material.deliveryCycle)}</span>
        </div>
        <span class="price-pill">¥ ${escapeHtml(material.price)} / ${escapeHtml(material.unit)}</span>
      </div>
      <div class="material-facts">
        <span>库存 ${escapeHtml(material.stock)}</span>
        <span>周期 ${escapeHtml(material.deliveryCycle)}</span>
      </div>
      <div class="menu-actions">
        <button class="btn btn-ghost btn-sm" data-chat="${material.id}">发起对话</button>
        ${actionButton("create-order", material.id, "直接下单", "btn-ghost", `create-order:${material.id}`)}
        <button class="btn btn-primary btn-sm" data-add-cart="${material.id}">${inCart ? "已在清单" : "加入采购清单"}</button>
      </div>
    </article>
  `;
}

export function renderReadonlyMaterial(material) {
  return `
    <article class="task-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(material.name)}</strong>
          <span>${escapeHtml(material.category)} · 库存 ${escapeHtml(material.stock)}</span>
        </div>
        <span class="chip blue">¥ ${escapeHtml(material.price)} / ${escapeHtml(material.unit)}</span>
      </div>
      <div class="muted">${escapeHtml(material.deliveryCycle)}</div>
    </article>
  `;
}

export function renderManagedMaterial(material) {
  return `
    <article class="task-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(material.materialName)}</strong>
          <span>${escapeHtml(material.category)} · 库存 ${material.stockQuantity} ${escapeHtml(material.unit)} · 日供 ${material.dailyCapacity} ${escapeHtml(material.unit)}</span>
        </div>
        <span class="chip ${material.status === 1 ? "green" : "amber"}">${material.status === 1 ? "上架中" : "已下架"}</span>
      </div>
      <div class="order-meta">
        <span class="chip blue">¥ ${material.supplyPrice} / ${escapeHtml(material.unit)}</span>
        <span class="chip">配送 ${material.deliveryRadiusKm} KM</span>
      </div>
      <div class="inline-form">
        <input aria-label="价格" type="number" min="0" step="0.01" value="${material.supplyPrice}" data-edit-price="${material.id}" />
        <input aria-label="库存" type="number" min="0" step="1" value="${material.stockQuantity}" data-edit-stock="${material.id}" />
        <input aria-label="日产能" type="number" min="0" step="1" value="${material.dailyCapacity}" data-edit-capacity="${material.id}" />
        <button class="btn btn-ghost btn-sm" data-update-material="${material.id}">保存</button>
        <button class="btn btn-danger btn-sm" data-offline-material="${material.id}">下架</button>
      </div>
    </article>
  `;
}

export function rankingPanel() {
  return `
    <div class="panel ranking-panel">
      <div class="panel-head"><div><h2>供应商履约排行榜</h2><div class="muted">评价写入 MySQL 后同步 Redis ZSet，用于快速读取 Top 排名。</div></div></div>
      <div class="panel-body ranking-list">
        ${
          state.supplierRanking.length
            ? state.supplierRanking.map((item) => `
                <div class="ranking-row">
                  <span class="rank-no">#${item.rank}</span>
                  <strong>${escapeHtml(item.companyName)}</strong>
                  <span class="chip green">${escapeHtml(item.ratingScore)} 分</span>
                </div>
              `).join("")
            : '<div class="empty">暂无排行榜数据。</div>'
        }
      </div>
    </div>
  `;
}

export function nearbySuppliersPanel() {
  return `
    <div class="panel ranking-panel">
      <div class="panel-head"><div><h2>附近供应商</h2><div class="muted">Redis GEO 按应急地点距离优先匹配供应商。</div></div></div>
      <div class="panel-body ranking-list">
        ${
          state.nearbySuppliers.length
            ? state.nearbySuppliers.map((item) => `
                <div class="ranking-row">
                  <span class="rank-no">${Number(item.distanceKm).toFixed(1)} KM</span>
                  <strong>${escapeHtml(item.companyName)}</strong>
                  <span class="muted">${escapeHtml(item.address)}</span>
                  <span class="chip green">${escapeHtml(item.ratingScore)} 分</span>
                </div>
              `).join("")
            : '<div class="empty">暂无附近供应商。</div>'
        }
      </div>
    </div>
  `;
}

export function driverAttendancePanel() {
  return `
    <div class="panel ranking-panel">
      <div class="panel-head">
        <div><h2>司机出勤</h2><div class="muted">今日出勤状态写入 Redis BitMap，适合海量司机签到。</div></div>
        <div style="display:flex; gap:10px; flex-wrap:wrap">
          <button class="btn btn-primary btn-sm" data-attendance="true">上线</button>
          <button class="btn btn-ghost btn-sm" data-attendance="false">离线</button>
        </div>
      </div>
      <div class="panel-body">
        <div class="status-line">
          <span class="chip ${state.attendance?.online ? "green" : "amber"}">${state.attendance?.online ? "今日在线" : "今日离线"}</span>
          <span class="muted">${escapeHtml(state.attendance?.date || "")}</span>
        </div>
      </div>
    </div>
  `;
}

export function mqPanel() {
  const total = state.deadLetters.reduce((sum, item) => sum + Number(item.messages || 0), 0);
  return `
    <div class="panel ranking-panel">
      <div class="panel-head">
        <div><h2>MQ 异常观测</h2><div class="muted">展示死信队列积压，并支持补偿重建推送记录。</div></div>
        <button class="btn btn-ghost btn-sm" id="retryPush">补偿推送</button>
      </div>
      <div class="panel-body ranking-list">
        ${
          state.deadLetters.length
            ? state.deadLetters.map((item) => `
                <div class="ranking-row">
                  <strong>${escapeHtml(item.queueName)}</strong>
                  <span class="chip ${Number(item.messages || 0) === 0 ? "green" : "amber"}">${item.messages} 条消息</span>
                  <span class="muted">消费者 ${item.consumers}</span>
                </div>
              `).join("")
            : '<div class="empty">暂无 MQ 统计。</div>'
        }
      </div>
      <div class="panel-body" style="padding-top:0"><span class="muted">当前死信总数：${total}</span></div>
    </div>
  `;
}

export function adminOpsPanel(dashboard) {
  return `
    <div class="panel ranking-panel">
      <div class="panel-head">
        <div><h2>运营异常处理</h2><div class="muted">集中展示 MQ 死信、待推送和异常订单，管理员可触发补偿。</div></div>
        ${actionButton("retry-push", "true", "补偿推送", "btn-primary", "retry-push")}
      </div>
      <div class="panel-body ranking-list">
        <div class="ranking-row">
          <strong>MQ 死信</strong>
          <span class="chip ${Number(dashboard.deadLetterCount || 0) > 0 ? "amber" : "green"}">${escapeHtml(dashboard.deadLetterCount || 0)} 条</span>
          <span class="muted">订单创建 / 抢单消费者异常</span>
        </div>
        <div class="ranking-row">
          <strong>待读推送</strong>
          <span class="chip blue">${escapeHtml(dashboard.pendingPushCount || 0)} 条</span>
          <span class="muted">司机未读或未处理推送</span>
        </div>
        <div class="ranking-row">
          <strong>已完成订单</strong>
          <span class="chip green">${escapeHtml(dashboard.completedCount || 0)} 单</span>
          <span class="muted">可发起三方评价</span>
        </div>
      </div>
      <div class="panel-body" style="padding-top:0">
        ${state.deadLetters.length ? state.deadLetters.map((item) => `
          <span class="chip ${Number(item.messages || 0) > 0 ? "amber" : "green"}">${escapeHtml(item.queueName)} · ${escapeHtml(item.messages)} 条</span>
        `).join(" ") : '<span class="muted">暂无死信队列统计。</span>'}
      </div>
    </div>
  `;
}

export function adminSupplierPanel() {
  return `
    <div class="panel">
      <div class="panel-head">
        <div><h2>供应商资质审核</h2><div class="muted">管理员审核供应商状态，审核结果会影响登录和采购大厅可用性。</div></div>
        <button class="btn btn-ghost btn-sm" id="refreshData">刷新</button>
      </div>
      <div class="panel-body supplier-grid">
        ${
          state.adminSuppliers.length
            ? state.adminSuppliers.map(renderAdminSupplierCard).join("")
            : '<div class="empty">暂无供应商资料。</div>'
        }
      </div>
    </div>
  `;
}

export function renderAdminSupplierCard(supplier) {
  return `
    <article class="order-card supplier-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(supplier.companyName)}</strong>
          <span>${escapeHtml(supplier.address)} · ${escapeHtml(supplier.licenseNo)}</span>
        </div>
        <span class="chip ${supplier.status === 1 ? "green" : "amber"}">${escapeHtml(supplier.auditStatus)}</span>
      </div>
      <div class="supplier-stats">
        <span><strong>${escapeHtml(supplier.ratingScore)}</strong> 履约评分</span>
        <span><strong>${escapeHtml(supplier.materialCount)}</strong> 供货品类</span>
        <span><strong>${escapeHtml(supplier.stockQuantity)}</strong> 库存合计</span>
      </div>
      <div class="order-meta">
        <span class="chip">联系人 ${escapeHtml(supplier.contactName)}</span>
        <span class="chip">电话 ${escapeHtml(supplier.contactPhone)}</span>
      </div>
      <div class="order-actions">
        ${actionButton("admin-approve-supplier", supplier.supplierId, "审核通过", "btn-primary", `admin-approve-supplier:${supplier.supplierId}`)}
        ${actionButton("admin-reject-supplier", supplier.supplierId, "驳回/停用", "btn-danger", `admin-reject-supplier:${supplier.supplierId}`)}
      </div>
    </article>
  `;
}

export function notificationCenterPanel() {
  return `
    <section class="notification-panel">
      <div class="panel-head">
        <div><h2>消息通知中心</h2><div class="muted">订单、推送和 MQ 异常按当前角色聚合展示。</div></div>
        <button class="btn btn-ghost btn-sm" id="refreshNotifications">刷新</button>
      </div>
      <div class="notification-list">
        ${
          state.notifications.length
            ? state.notifications.map((item) => `
                <div class="notification-row">
                  <span class="chip ${notificationClass(item)}">${escapeHtml(item.type)}</span>
                  <div>
                    <strong>${escapeHtml(item.title)}</strong>
                    <p>${escapeHtml(item.content)}</p>
                    <span class="muted">${escapeHtml(item.status)} · ${escapeHtml(item.createTime)}</span>
                  </div>
                </div>
              `).join("")
            : '<div class="empty">暂无通知。</div>'
        }
      </div>
    </section>
  `;
}

export function supplierFiltersPanel() {
  return `
    <div class="panel filter-panel">
      <div class="toolbar">
        <div class="search-box">
          <input id="supplierKeyword" value="${escapeHtml(state.supplierFilters.keyword)}" placeholder="搜索供应商、物资、资质或地址" />
        </div>
        <select id="supplierCategory">
          <option value="ALL">全部物资</option>
          ${supplierCategories().map((category) => `<option value="${escapeHtml(category)}" ${state.supplierFilters.category === category ? "selected" : ""}>${escapeHtml(category)}</option>`).join("")}
        </select>
        <select id="supplierSort">
          <option value="rating" ${state.supplierFilters.sort === "rating" ? "selected" : ""}>评分优先</option>
          <option value="price" ${state.supplierFilters.sort === "price" ? "selected" : ""}>价格优先</option>
          <option value="stock" ${state.supplierFilters.sort === "stock" ? "selected" : ""}>库存优先</option>
          <option value="materials" ${state.supplierFilters.sort === "materials" ? "selected" : ""}>供货丰富度</option>
        </select>
      </div>
    </div>
  `;
}

export function supplierStorePanel(supplier) {
  const store = state.supplierStore?.supplier?.id === supplier.id ? state.supplierStore : null;
  return `
    <div class="store-head">
      <div>
        <h2>${escapeHtml(supplier.companyName)}</h2>
        <div class="muted">${escapeHtml(supplier.region || "华东")} · 评分 ${escapeHtml(supplier.rating || "")}</div>
      </div>
      <span class="chip green">${store ? `${store.totalOrders} 单` : "店铺"}</span>
    </div>
    <div class="panel-body">
      <div class="detail-list">
        ${detailItem("联系人", supplier.contactName)}
        ${detailItem("地址", supplier.address)}
        ${detailItem("资质", supplier.certifications.join(" / "))}
        ${store ? detailItem("服务摘要", store.serviceSummary) : ""}
      </div>
      <h3 class="section-subtitle">供应货物菜单</h3>
      <div class="menu-list">
        ${supplier.materials.length ? supplier.materials.map(renderPurchaserMaterial).join("") : '<div class="empty">该供应商暂无上架物资。</div>'}
      </div>
      ${store ? storeInsightPanel(store) : '<div class="empty">正在读取店铺订单和评价。</div>'}
    </div>
  `;
}

export function storeInsightPanel(store) {
  return `
    <h3 class="section-subtitle">最近履约</h3>
    <div class="store-insights">
      <div>
        <strong>最近订单</strong>
        ${
          store.recentOrders.length
            ? store.recentOrders.map((order) => `<span>${escapeHtml(order.materialName)} · ${escapeHtml(order.status)}</span>`).join("")
            : '<span class="muted">暂无历史订单</span>'
        }
      </div>
      <div>
        <strong>最近评价</strong>
        ${
          store.recentReviews.length
            ? store.recentReviews.map((review) => `<span>${"★".repeat(Number(review.score || 0))} ${escapeHtml(review.content)}</span>`).join("")
            : '<span class="muted">暂无评价</span>'
        }
      </div>
    </div>
  `;
}

export function cartPanel() {
  const supplier = selectedSupplier();
  const sameSupplier = state.cart.length && state.cart[0].supplierId === supplier.id;
  return `
    <div class="panel cart-panel">
      <div class="panel-head">
        <div><h2>采购清单</h2><div class="muted">像购物车一样聚合物资，提交后批量生成采购订单。</div></div>
        <span class="chip blue">${state.cart.length} 项</span>
      </div>
      <div class="panel-body">
        ${
          state.cart.length
            ? `
              <div class="cart-list">${state.cart.map(renderCartItem).join("")}</div>
              ${sameSupplier ? "" : '<div class="empty">采购清单只支持同一供应商，切换供应商加入物资会自动重建清单。</div>'}
              <div class="cart-footer">
                <div>
                  <span class="cart-total">${escapeHtml(cartTotalAmount())}</span>
                  <span class="muted">预估金额，提交后进入 MQ 异步流程</span>
                </div>
                <div class="cart-actions">
                  <button class="btn btn-ghost btn-sm" id="clearCart">清空</button>
                  ${actionButton("checkout-cart", "true", "提交采购清单", "btn-primary", "checkout-cart")}
                </div>
              </div>
            `
            : '<div class="empty">从供应货物菜单加入物资后，这里会生成采购清单。</div>'
        }
      </div>
    </div>
  `;
}

export function renderCartItem(item) {
  return `
    <div class="cart-row">
      <div>
        <strong>${escapeHtml(item.name)}</strong>
        <span class="muted">¥ ${escapeHtml(item.price)} / ${escapeHtml(item.unit)}</span>
      </div>
      <input aria-label="采购数量" value="${escapeHtml(item.quantity)}" data-cart-quantity="${item.materialId}" />
      <button class="btn btn-ghost btn-sm" data-remove-cart="${item.materialId}">移除</button>
    </div>
  `;
}

export function reviewModalTemplate() {
  if (!state.reviewModal) return "";
  const order = state.reviewModal.order;
  const options = reviewTargetOptions(order);
  const defaultTarget = options[0];
  return `
    <div class="modal-backdrop" role="presentation" data-close-review="true">
      <section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="reviewTitle">
        <div class="modal-head">
          <div>
            <h2 id="reviewTitle">评价履约</h2>
            <div class="muted">${escapeHtml(order.id)} · ${escapeHtml(order.materialName)}</div>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭评价弹窗" data-close-review="true">×</button>
        </div>
        <form class="modal-body form-grid" id="reviewForm">
          <div class="field">
            <label for="reviewTarget">评价对象</label>
            <select id="reviewTarget" name="target">
              ${options.map((option, index) => `<option value="${option.targetType}:${option.targetId}" ${index === 0 ? "selected" : ""}>${escapeHtml(option.label)}</option>`).join("")}
            </select>
          </div>
          <div class="field">
            <label for="reviewScore">评分</label>
            <select id="reviewScore" name="score">
              <option value="5">5 分 - 履约优秀</option>
              <option value="4">4 分 - 整体稳定</option>
              <option value="3">3 分 - 基本达标</option>
              <option value="2">2 分 - 有明显问题</option>
              <option value="1">1 分 - 履约失败</option>
            </select>
          </div>
          <div class="field">
            <label for="reviewContent">评价内容</label>
            <textarea id="reviewContent" name="content" rows="4">${escapeHtml(defaultReviewText(defaultTarget?.targetType))}</textarea>
          </div>
          <div class="modal-actions">
            <button class="btn btn-ghost" type="button" data-close-review="true">取消</button>
            <button class="btn btn-primary" type="submit">提交评价</button>
          </div>
        </form>
      </section>
    </div>
  `;
}

export function timelineModalTemplate() {
  if (!state.timelineModal) return "";
  const { order, items } = state.timelineModal;
  return `
    <div class="modal-backdrop" role="presentation" data-close-timeline="true">
      <section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="timelineTitle">
        <div class="modal-head">
          <div>
            <h2 id="timelineTitle">订单时间线</h2>
            <div class="muted">${escapeHtml(order.id)} · ${escapeHtml(order.materialName)}</div>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭时间线弹窗" data-close-timeline="true">×</button>
        </div>
        <div class="modal-body timeline">
          ${
            items.length
              ? items.map((item) => `
                  <div class="timeline-item">
                    <span class="dot"></span>
                    <div>
                      <strong>${escapeHtml(item.action)}</strong>
                      <div class="muted">${escapeHtml(item.status)} · ${escapeHtml(item.operatorType)} ${item.operatorId}</div>
                      <div class="muted">${escapeHtml(item.remark)} · ${escapeHtml(item.createdAt)}</div>
                    </div>
                    <span class="chip blue">${escapeHtml(item.status)}</span>
                  </div>
                `).join("")
              : '<div class="empty">暂无时间线记录。</div>'
          }
        </div>
      </section>
    </div>
  `;
}

export function reviewTargetOptions(order) {
  if (state.user.userType === "PURCHASER") {
    return [
      { targetType: "SUPPLIER", targetId: order.supplierId, label: `供应商：${order.supplierName}` },
      ...(order.driverId ? [{ targetType: "DRIVER", targetId: order.driverId, label: `司机：${order.driverId}` }] : []),
    ];
  }
  if (state.user.userType === "SUPPLIER") {
    return [
      { targetType: "PURCHASER", targetId: order.purchaserId, label: `采购方：${order.purchaserName}` },
      ...(order.driverId ? [{ targetType: "DRIVER", targetId: order.driverId, label: `司机：${order.driverId}` }] : []),
    ];
  }
  return [{ targetType: "PURCHASER", targetId: order.purchaserId, label: `采购方：${order.purchaserName}` }];
}

export function defaultReviewText(targetType) {
  return {
    SUPPLIER: "应急物资响应及时，备货稳定，履约协同顺畅。",
    PURCHASER: "需求信息明确，确认流程顺畅，协同效率高。",
    DRIVER: "运输到场及时，回单完整，线路执行稳定。",
  }[targetType] || "履约及时，协同顺畅。";
}

export function ordersPanel(title, desc, orders, claimable = false) {
  return `
    <div class="panel">
      <div class="panel-head">
        <div><h2>${title}</h2><div class="muted">${desc}</div></div>
        <button class="btn btn-ghost btn-sm" id="refreshData">刷新</button>
      </div>
      <div class="panel-body">
        ${
          orders.length
            ? `<div class="task-board">${orders.map((order) => renderOrderCard(order, claimable)).join("")}</div>`
            : '<div class="empty">暂无订单。采购方确认购货订单后，这里会出现数据。</div>'
        }
      </div>
    </div>
  `;
}

export function renderOrderCard(order, claimable) {
  const actions = orderActions(order, claimable);
  return `
    <article class="task-card order-task-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(order.materialName)}</strong>
          <span>${escapeHtml(order.id)} · ${escapeHtml(order.purchaserName)} -> ${escapeHtml(order.supplierName)}</span>
        </div>
        <span class="chip ${orderStatusClass(order.status)}">${escapeHtml(order.status)}</span>
      </div>
      <div class="order-meta">
        <span class="chip">${escapeHtml(order.category)}</span>
        <span class="chip">${escapeHtml(order.quantity)}</span>
        <span class="chip blue">${escapeHtml(order.amount)}</span>
        ${order.pushStatus ? `<span class="chip ${order.pushStatus === "PENDING" ? "amber" : "green"}">推送 ${escapeHtml(pushStatusLabel(order.pushStatus))}</span>` : ""}
      </div>
      ${orderStatusProgress(order.status)}
      <div class="muted">${escapeHtml(order.source)} · ${escapeHtml(order.pushedTo)}</div>
      <div class="order-actions">
        ${actions.join("")}
      </div>
    </article>
  `;
}

export function orderActions(order, claimable) {
  const actions = [];
  if (order.pushStatus === "PENDING") {
    actions.push(actionButton("read-push", order.id, "标记已读", "btn-ghost", `read-push:${order.id}`));
  }
  if (state.user.userType === "SUPPLIER" && order.status === "待供应商确认") {
    actions.push(actionButton("confirm-supplier-order", order.id, "确认供货", "btn-primary", `confirm-supplier-order:${order.id}`));
    actions.push(actionButton("reject-supplier-order", order.id, "拒单", "btn-danger", `reject-supplier-order:${order.id}`));
  }
  if (claimable && order.status === "待司机接单") {
    actions.push(actionButton("claim-order", order.id, "抢运输单", "btn-primary", `claim-order:${order.id}`));
  }
  if (state.user.userType === "DRIVER" && order.status === "司机已接单") {
    actions.push(actionButton("start-transport", order.id, "开始运输", "btn-primary", `start-transport:${order.id}`));
  }
  if (state.user.userType === "DRIVER" && order.status === "运输中") {
    actions.push(actionButton("complete-transport", order.id, "完成运输", "btn-primary", `complete-transport:${order.id}`));
  }
  if (order.status === "已完成" && state.user.userType !== "ADMIN") {
    actions.push(`<button class="btn btn-ghost btn-sm" data-review-order="${order.id}">评价履约</button>`);
  }
  actions.push(`<button class="btn btn-ghost btn-sm" data-order-timeline="${order.id}">时间线</button>`);
  return actions;
}

export function pushStatusLabel(status) {
  return {
    PENDING: "未读",
    READ: "已读",
    CLAIMED: "已接单",
  }[status] || status;
}

export function orderStatusClass(status) {
  if (status === "已拒单") return "red";
  if (status === "已完成" || status === "司机已接单" || status === "运输中") return "green";
  return "amber";
}

export function orderStatusProgress(status) {
  const currentIndex = orderStatusFlow.indexOf(status);
  if (currentIndex < 0) {
    return `<div class="order-progress-line rejected"><span>${escapeHtml(status)}</span></div>`;
  }
  return `
    <div class="order-progress-line" aria-label="订单状态：${escapeHtml(status)}">
      ${orderStatusFlow.map((step, index) => {
        const className = index < currentIndex ? "done" : index === currentIndex ? "active" : "";
        return `<span class="${className}">${escapeHtml(step)}</span>`;
      }).join("")}
    </div>
  `;
}

export function actionButton(dataName, dataValue, label, variant = "btn-primary", actionKey = "") {
  const key = actionKey || `${dataName}:${dataValue}`;
  const loading = Boolean(state.actionLoading[key]);
  return `
    <button class="btn ${variant} btn-sm" data-${dataName}="${escapeHtml(dataValue)}" ${loading ? "disabled" : ""}>
      ${loading ? "处理中..." : label}
    </button>
  `;
}

export function followRows(withAction = false) {
  return state.follows
    .map(
      (item) => `
        <div class="timeline-item">
          <span class="dot"></span>
          <div>
            <strong>${escapeHtml(item.purchaserName)}</strong>
            <div class="muted">司机关注：${item.followedByDriver ? "已关注" : "未关注"} · 采购方关注司机：${item.followedByPurchaser ? "已关注" : "未关注"}</div>
          </div>
          ${
            withAction && !item.followedByDriver
              ? `<button class="btn btn-primary btn-sm" data-follow-purchaser="${item.purchaserId}">关注</button>`
              : '<span class="chip green">可接收推送</span>'
          }
        </div>
      `,
    )
    .join("");
}

export function statCard(label, value, trend) {
  return `
    <div class="stat-card">
      <span>${label}</span>
      <strong>${value}</strong>
      <div class="trend">${trend}</div>
    </div>
  `;
}

export function profilePanel(title, desc) {
  return `
    <div class="profile-layout">
      <div class="profile-card">
        <div class="profile-hero">
          <div class="avatar">${escapeHtml(state.user.displayName).slice(0, 1)}</div>
          <div>
            <h2>${escapeHtml(state.user.displayName)}</h2>
            <div class="muted">${roleMeta[state.user.userType].label} · ${desc}</div>
          </div>
        </div>
        <div class="panel-body kv">
          <div><span>账号</span><strong>${escapeHtml(state.user.username)}</strong></div>
          <div><span>用户 ID</span><strong>${state.user.id}</strong></div>
          <div><span>权限模式</span><strong>自动通过</strong></div>
        </div>
        <button class="btn btn-danger" id="logoutBtn">退出登录</button>
      </div>
      <div class="panel">
        <div class="panel-head"><div><h2>${title}</h2><div class="muted">${desc}</div></div></div>
        <div class="panel-body">
          <div class="dashboard-grid">
            ${statCard("登录状态", "正常", "Redis Token")}
            ${statCard("服务发现", "Nacos", "网关统一入口")}
            ${statCard("角色", roleMeta[state.user.userType].label, "三方隔离")}
            ${statCard("后续扩展", "RBAC", "可细化")}
          </div>
        </div>
      </div>
    </div>
  `;
}

export function detailItem(label, value) {
  return `<div class="detail-item"><span>${label}</span><strong>${escapeHtml(value)}</strong></div>`;
}

export function defaultAuthUsername(userType) {
  if (state.authMode === "login") {
    return {
      SUPPLIER: "supplier01",
      PURCHASER: "purchaser01",
      DRIVER: "driver01",
      ADMIN: "admin01",
    }[userType];
  }
  return {
    SUPPLIER: "supplier_new",
    PURCHASER: "purchaser_new",
    DRIVER: "driver_new",
    ADMIN: "admin_new",
  }[userType];
}

export function defaultRegisterDisplayName(userType) {
  return {
    SUPPLIER: "新注册供应商公司",
    PURCHASER: "新注册采购公司",
    DRIVER: "新注册司机",
    ADMIN: "新注册管理员",
  }[userType];
}
