import { icons, orderStatusFlow, roleMeta } from "./config.js";
import { state } from "./state.js";
import { escapeHtml } from "./utils.js";
import {
  cartTotalAmount,
  filteredSuppliers,
  notificationClass,
  selectedSupplier,
  supplierFromList,
  supplierCategories,
  supplierMinPrice,
  supplierTotalStock,
  unreadNotificationCount,
} from "./selectors.js";

/**
 * 作用：生成登录页 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function loginTemplate() {
  const activeUserType = state.authMode === "login" && state.savedLogin?.userType ? state.savedLogin.userType : "SUPPLIER";
  const savedLogin = state.authMode === "login" && state.savedLogin?.userType === activeUserType ? state.savedLogin : null;
  const usernameValue = state.authMode === "login" ? savedLogin?.username || defaultAuthUsername(activeUserType) : "";
  const passwordValue = state.authMode === "login" ? savedLogin?.password || "" : "";
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
                <option value="SUPPLIER" ${activeUserType === "SUPPLIER" ? "selected" : ""}>供应商</option>
                <option value="PURCHASER" ${activeUserType === "PURCHASER" ? "selected" : ""}>采购方</option>
                <option value="DRIVER" ${activeUserType === "DRIVER" ? "selected" : ""}>司机</option>
                <option value="ADMIN" ${activeUserType === "ADMIN" ? "selected" : ""}>平台管理员</option>
              </select>
            </div>
            <div class="field">
              <label for="username">用户名</label>
              <input id="username" name="username" autocomplete="username" value="${escapeHtml(usernameValue)}" />
            </div>
            <div class="field">
              <label for="password">密码</label>
              <input id="password" name="password" type="password" autocomplete="${state.authMode === "login" ? "current-password" : "new-password"}" value="${escapeHtml(passwordValue)}" />
            </div>
            ${
              state.authMode === "login"
                ? `
                  <label class="check-row" for="rememberPassword">
                    <input id="rememberPassword" name="rememberPassword" type="checkbox" ${savedLogin ? "checked" : ""} />
                    <span>保存密码</span>
                  </label>
                `
                : ""
            }
            ${
              state.authMode === "register"
                ? `
                  <div class="field">
                    <label for="displayName">名称</label>
                    <input id="displayName" name="displayName" autocomplete="organization" value="" />
                  </div>
                  <div class="field">
                    <label for="contactPhone">联系电话</label>
                    <input id="contactPhone" name="contactPhone" autocomplete="tel" value="" />
                  </div>
                  <div class="field">
                    <label for="registerAddress">地址</label>
                    <input id="registerAddress" name="address" autocomplete="street-address" placeholder="采购方/供应商必填，例如：北京交通大学" />
                    <div class="field-help">提交后由后端自动获取经纬度；获取失败时请手动填写。</div>
                  </div>
                  <div class="form-split">
                    <div class="field">
                      <label for="registerLongitude">经度</label>
                      <input id="registerLongitude" name="longitude" inputmode="decimal" placeholder="自动获取失败时填写" />
                    </div>
                    <div class="field">
                      <label for="registerLatitude">纬度</label>
                      <input id="registerLatitude" name="latitude" inputmode="decimal" placeholder="自动获取失败时填写" />
                    </div>
                  </div>
                `
                : ""
            }
            ${state.authError ? `<div class="auth-error" role="alert">${escapeHtml(state.authError)}</div>` : ""}
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

/**
 * 作用：生成应用主界面 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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
          <button class="btn btn-danger btn-sm" type="button" data-logout>退出登录</button>
        </header>
        <section class="content">
          ${renderRoleContent()}
        </section>
        ${state.showNotifications ? `<div class="notification-popover">${notificationCenterPanel()}</div>` : ""}
      </main>
      <div class="toast ${state.toast ? "show" : ""}">${escapeHtml(state.toast)}</div>
      ${reviewModalTemplate()}
      ${acceptanceModalTemplate()}
      ${paymentModalTemplate()}
      ${timelineModalTemplate()}
    </div>
  `;
}

/**
 * 作用：根据当前用户角色生成中间内容区。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function renderRoleContent() {
  if (state.loading) return '<div class="panel"><div class="panel-body empty">加载中...</div></div>';
  if (state.user.userType === "ADMIN") return renderAdminContent();
  if (state.user.userType === "PURCHASER") return renderPurchaserContent();
  if (state.user.userType === "DRIVER") return renderDriverContent();
  return renderSupplierContent();
}

/**
 * 作用：生成管理员工作台内容。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function renderAdminContent() {
  if (state.page === "suppliers") {
    return adminSupplierPanel();
  }
  if (state.page === "orders") {
    return `
      ${dispatchRecommendationPanel()}
      ${ordersPanel("订单监控", "平台管理员查看最近 50 条订单，识别异常状态和履约进度。", state.adminOrders)}
    `;
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
    ${dispatchRecommendationPanel()}
    ${ordersPanel("最新订单", "运营端聚合采购、供货和运输状态。", state.adminOrders)}
  `;
}

/**
 * 作用：生成采购方工作台内容。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function renderPurchaserContent() {
  if (state.page === "orders") {
    return `
      ${dispatchRecommendationPanel()}
      ${ordersPanel("我的采购订单", "采购方和供应商都能看到自己的订单状态。", state.purchaserOrders)}
    `;
  }
  if (state.page === "rfqs") {
    return purchaserRfqPanel();
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
  const supplier = supplierFromList(suppliers, state.selectedSupplierId);
  return `
    ${rankingPanel()}
    ${nearbySuppliersPanel()}
    ${dispatchRecommendationPanel()}
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

/**
 * 作用：生成供应商工作台内容。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function renderSupplierContent() {
  const self = state.suppliers.find((supplier) => supplier.id === state.user.id) || {
    id: state.user.id,
    companyName: state.user.displayName,
    contactName: state.user.displayName,
    region: "待完善",
    address: "待完善",
    rating: "4.50",
    certifications: ["注册资料待完善"],
    materials: [],
  };
  const pendingConfirmOrders = state.supplierOrders.filter((order) => isSupplierConfirmationStatus(order.status));
  if (state.page === "materials") {
    return supplierMaterialManager();
  }
  if (state.page === "rfqs") {
    return supplierRfqPanel();
  }
  if (state.page === "orders") {
    return `
      ${dispatchRecommendationPanel()}
      ${ordersPanel("我的供货订单", "只展示属于当前供应商的订单状态。", state.supplierOrders)}
    `;
  }
  if (state.page === "profile") return supplierQualificationPanel();

  return `
    <div class="dashboard-grid">
      ${statCard("供应货物", self?.materials.length || 0, "采购方可见")}
      ${statCard("供货订单", state.supplierOrders.length, "待备货 / 待运输")}
      ${statCard("待确认", pendingConfirmOrders.length, "确认供货 / 拒单")}
      ${statCard("履约评分", self?.rating || "4.50", "平台评级")}
    </div>
    ${supplierPendingConfirmPanel(pendingConfirmOrders)}
    ${rankingPanel()}
    ${dispatchRecommendationPanel()}
    ${mqPanel()}
    ${ordersPanel("最新供货订单", "采购方确认购货后，供应商侧同步可见。", state.supplierOrders)}
  `;
}

/**
 * 作用：生成供应商待确认订单面板。
 * 输入：
 * - orders：待供应商确认的订单列表。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function supplierPendingConfirmPanel(orders) {
  return `
    <div class="panel">
      <div class="panel-head">
        <div><h2>待确认供货</h2><div class="muted">普通采购单和高并发抢购成功单都会在这里确认供货或拒单。</div></div>
        <button class="btn btn-ghost btn-sm" id="refreshData">刷新</button>
      </div>
      <div class="panel-body">
        ${
          orders.length
            ? `<div class="task-board">${orders.map((order) => renderOrderCard(order, false)).join("")}</div>`
            : '<div class="empty">暂无待确认订单。采购方下单或抢购成功后会出现在这里。</div>'
        }
      </div>
    </div>
  `;
}

/**
 * 作用：生成供应商物资管理区域 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成供应商物资管理区域。
 */
export function supplierMaterialManager() {
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
        <div class="panel-head"><div><h2>新增供货物资</h2><div class="muted">可选择基础物资，也可以直接输入新物资并上架。</div></div></div>
        <form class="panel-body form-grid" id="supplierMaterialForm">
          <div class="field">
            <label for="materialId">已有物资</label>
            <select id="materialId" name="materialId" ${state.materialOptions.length ? "" : "disabled"}>
              <option value="">${state.materialOptions.length ? "选择已有物资，或填写下方新物资" : "暂无基础物资，请直接填写新物资"}</option>
              ${state.materialOptions.map((item) => `<option value="${item.id}">${escapeHtml(item.materialName)} / ${escapeHtml(item.unit)}</option>`).join("")}
            </select>
          </div>
          <div class="form-note">没有合适的基础物资时，直接填写下面三项；系统会先写入物资基础表，再上架到你的供应目录。</div>
          <div class="field">
            <label for="materialName">新物资名称</label>
            <input id="materialName" name="materialName" list="materialNameHints" placeholder="例如：应急帐篷、瓶装饮用水、柴油发电机" />
            <datalist id="materialNameHints">
              <option value="应急帐篷"></option>
              <option value="瓶装饮用水"></option>
              <option value="柴油发电机"></option>
              <option value="防汛沙袋"></option>
            </datalist>
          </div>
          <div class="form-split">
            <div class="field">
              <label for="materialCategory">分类</label>
              <input id="materialCategory" name="category" list="materialCategoryHints" placeholder="例如：应急物资" />
              <datalist id="materialCategoryHints">
                <option value="应急物资"></option>
                <option value="水泥"></option>
                <option value="钢材"></option>
                <option value="食品饮水"></option>
                <option value="设备"></option>
              </datalist>
            </div>
            <div class="field">
              <label for="materialUnit">单位</label>
              <input id="materialUnit" name="unit" list="materialUnitHints" placeholder="例如：件、吨、箱" />
              <datalist id="materialUnitHints">
                <option value="件"></option>
                <option value="吨"></option>
                <option value="箱"></option>
                <option value="卷"></option>
                <option value="车"></option>
              </datalist>
            </div>
          </div>
          <div class="field">
            <label for="supplyPrice">价格</label>
            <div class="input-with-unit">
              <input id="supplyPrice" name="supplyPrice" inputmode="decimal" placeholder="例如：849.98" />
              <span id="priceUnit">元/单位</span>
            </div>
          </div>
          <div class="field">
            <label for="stockQuantity">库存</label>
            <div class="input-with-unit">
              <input id="stockQuantity" name="stockQuantity" inputmode="numeric" placeholder="例如：300" />
              <span id="stockUnit">单位</span>
            </div>
          </div>
          <div class="field">
            <label for="dailyCapacity">日产能</label>
            <div class="input-with-unit">
              <input id="dailyCapacity" name="dailyCapacity" inputmode="numeric" placeholder="例如：80" />
              <span id="capacityUnit">单位/日</span>
            </div>
          </div>
          <div class="field">
            <label for="deliveryRadiusKm">配送半径 KM</label>
            <div class="input-with-unit">
              <input id="deliveryRadiusKm" name="deliveryRadiusKm" inputmode="decimal" placeholder="例如：180" />
              <span>KM</span>
            </div>
          </div>
          <button class="btn btn-primary" type="submit">新增 / 重新上架</button>
        </form>
      </aside>
    </div>
  `;
}

/**
 * 作用：生成供应商企业资质维护页面。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成企业资质页面。
 */
export function supplierQualificationPanel() {
  const qualification = state.supplierQualification || {};
  const risks = qualification.riskTags || ["资质资料待完善"];
  return `
    <div class="dashboard-grid">
      ${statCard("审核状态", qualification.auditStatusText || "待完善", qualification.auditRemark || "提交后管理员复核")}
      ${statCard("资料完整度", `${qualification.qualificationCompletion ?? 0}%`, "证照 / 地址 / 联系方式")}
      ${statCard("风险项", risks.filter((item) => item !== "资质资料完整").length, "管理员审核可见")}
      ${statCard("供应货物", state.supplierMaterials.length, "影响准入判断")}
    </div>
    <div class="layout-2">
      <div class="panel">
        <div class="panel-head">
          <div><h2>企业资质资料</h2><div class="muted">保存后会进入待复核，管理员审核页会同步显示完整度和风险项。</div></div>
          <span class="chip ${qualificationStatusClass(qualification.auditStatus)}">${escapeHtml(qualification.auditStatusText || "待完善")}</span>
        </div>
        <form class="panel-body form-grid" id="supplierQualificationForm">
          <div class="form-split">
            <div class="field">
              <label for="qualificationCompanyName">企业名称</label>
              <input id="qualificationCompanyName" name="companyName" value="${escapeHtml(qualification.companyName || state.user.displayName)}" />
            </div>
            <div class="field">
              <label for="qualificationLicenseNo">营业执照编号</label>
              <input id="qualificationLicenseNo" name="licenseNo" value="${escapeHtml(qualification.licenseNo || "")}" />
            </div>
          </div>
          <div class="form-split">
            <div class="field">
              <label for="qualificationContactName">联系人</label>
              <input id="qualificationContactName" name="contactName" value="${escapeHtml(qualification.contactName || state.user.displayName)}" />
            </div>
            <div class="field">
              <label for="qualificationContactPhone">联系电话</label>
              <input id="qualificationContactPhone" name="contactPhone" inputmode="tel" value="${escapeHtml(qualification.contactPhone || "")}" />
            </div>
          </div>
          <div class="field">
            <label for="qualificationAddress">经营地址</label>
            <input id="qualificationAddress" name="address" value="${escapeHtml(qualification.address || "")}" />
            <div class="field-help">保存时由后端按经营地址自动获取经纬度；获取失败时请手动填写。</div>
          </div>
          <div class="form-split">
            <div class="field">
              <label for="qualificationLongitude">服务经度</label>
              <input id="qualificationLongitude" name="longitude" inputmode="decimal" value="${escapeHtml(qualification.longitude ?? "")}" />
            </div>
            <div class="field">
              <label for="qualificationLatitude">服务纬度</label>
              <input id="qualificationLatitude" name="latitude" inputmode="decimal" value="${escapeHtml(qualification.latitude ?? "")}" />
            </div>
          </div>
          <div class="field">
            <label for="businessLicenseUrl">营业执照附件 URL</label>
            <input id="businessLicenseUrl" name="businessLicenseUrl" value="${escapeHtml(qualification.businessLicenseUrl || "")}" placeholder="https://files.example.com/license.pdf" />
          </div>
          <div class="field">
            <label for="safetyCertUrl">安全生产证明 URL</label>
            <input id="safetyCertUrl" name="safetyCertUrl" value="${escapeHtml(qualification.safetyCertUrl || "")}" placeholder="https://files.example.com/safety.pdf" />
          </div>
          <div class="field">
            <label for="insuranceCertUrl">履约保险证明 URL</label>
            <input id="insuranceCertUrl" name="insuranceCertUrl" value="${escapeHtml(qualification.insuranceCertUrl || "")}" placeholder="https://files.example.com/insurance.pdf" />
          </div>
          ${actionButton("supplier-qualification", "true", "保存并提交复核", "btn-primary", "supplier-qualification")}
        </form>
      </div>
      <aside class="panel detail-drawer">
        <div class="panel-head"><div><h2>准入检查</h2><div class="muted">这些信息会辅助管理员判断供应商是否可参与报价。</div></div></div>
        <div class="panel-body qualification-summary">
          <div class="qualification-meter">
            <div><strong>${qualification.qualificationCompletion ?? 0}%</strong><span>资料完整度</span></div>
            <div class="progress"><span style="width:${Math.max(0, Math.min(100, Number(qualification.qualificationCompletion || 0)))}%"></span></div>
          </div>
          <div class="risk-list">
            ${risks.map((risk) => `<span class="chip ${risk === "资质资料完整" ? "green" : "amber"}">${escapeHtml(risk)}</span>`).join("")}
          </div>
          <div class="form-note">${escapeHtml(qualification.auditRemark || "暂无审核备注。")}</div>
        </div>
      </aside>
    </div>
  `;
}

/**
 * 作用：生成采购方询价管理页面。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成采购方 RFQ 页面。
 */
export function purchaserRfqPanel() {
  const selectedRfq = state.purchaserRfqs.find((item) => item.id === state.selectedRfqId) || state.purchaserRfqs[0] || null;
  const quoteTitle = selectedRfq
    ? `${selectedRfq.materialName} · ${selectedRfq.quantity}`
    : "报价排序";
  return `
    <div class="dashboard-grid">
      ${statCard("询价单", state.purchaserRfqs.length, "多供应商报价")}
      ${statCard("开放中", state.purchaserRfqs.filter((item) => item.status === "OPEN").length, "可继续收报价")}
      ${statCard("报价数", state.purchaserRfqs.reduce((total, item) => total + Number(item.quoteCount || 0), 0), "按综合条件排序")}
      ${statCard("已采纳", state.purchaserRfqs.filter((item) => item.status === "AWARDED").length, "生成采购订单")}
    </div>
    <div class="layout-2 rfq-layout">
      <div class="panel">
        <div class="panel-head">
          <div><h2>发布询价</h2><div class="muted">采购方发布需求后，供应商在报价页选择自己的上架物资并提交报价。</div></div>
        </div>
        <form class="panel-body form-grid" id="rfqForm">
          <div class="form-split">
            <div class="field">
              <label for="rfqMaterialName">物资名称</label>
              <input id="rfqMaterialName" name="materialName" list="rfqMaterialHints" placeholder="例如：瓶装饮用水" />
              <datalist id="rfqMaterialHints">
                <option value="瓶装饮用水"></option>
                <option value="应急帐篷"></option>
                <option value="防汛沙袋"></option>
                <option value="柴油发电机"></option>
              </datalist>
            </div>
            <div class="field">
              <label for="rfqCategory">分类</label>
              <input id="rfqCategory" name="category" list="rfqCategoryHints" placeholder="例如：食品饮水" />
              <datalist id="rfqCategoryHints">
                <option value="食品饮水"></option>
                <option value="应急物资"></option>
                <option value="设备"></option>
                <option value="钢材"></option>
              </datalist>
            </div>
          </div>
          <div class="form-split">
            <div class="field">
              <label for="rfqUnit">单位</label>
              <input id="rfqUnit" name="unit" list="rfqUnitHints" placeholder="例如：箱" />
              <datalist id="rfqUnitHints">
                <option value="箱"></option>
                <option value="件"></option>
                <option value="吨"></option>
                <option value="车"></option>
              </datalist>
            </div>
            <div class="field">
              <label for="rfqQuantity">采购数量</label>
              <input id="rfqQuantity" name="quantity" inputmode="decimal" placeholder="例如：80 箱" />
            </div>
          </div>
          <div class="field">
            <label for="rfqDeliveryAddress">收货地址</label>
            <input id="rfqDeliveryAddress" name="deliveryAddress" placeholder="例如：北京交通大学" />
            <div class="field-help" id="rfqGeocodeStatus">提交后由后端自动获取经纬度，必要时可手动修正。</div>
          </div>
          <div class="form-split">
            <div class="field">
              <label for="rfqLongitude">经度</label>
              <input id="rfqLongitude" name="longitude" inputmode="decimal" placeholder="后端自动获取失败时填写" />
            </div>
            <div class="field">
              <label for="rfqLatitude">纬度</label>
              <input id="rfqLatitude" name="latitude" inputmode="decimal" placeholder="后端自动获取失败时填写" />
            </div>
          </div>
          <div class="field">
            <label for="rfqRemark">需求备注</label>
            <textarea id="rfqRemark" name="remark" rows="3" placeholder="例如：希望今天报价，明天送达。"></textarea>
          </div>
          ${actionButton("create-rfq", "true", "发布询价", "btn-primary", "create-rfq")}
        </form>
      </div>
      <aside class="panel detail-drawer">
        <div class="panel-head">
          <div><h2>我的询价单</h2><div class="muted">点击询价单查看供应商报价排序。</div></div>
          <button class="btn btn-ghost btn-sm" id="refreshData">刷新</button>
        </div>
        <div class="panel-body rfq-list">
          ${state.purchaserRfqs.length ? state.purchaserRfqs.map(renderPurchaserRfqCard).join("") : '<div class="empty">暂无询价单，先从左侧发布一个需求。</div>'}
        </div>
      </aside>
    </div>
    <div class="panel rfq-quotes-panel">
      <div class="panel-head">
        <div><h2>${escapeHtml(quoteTitle)}</h2><div class="muted">报价按单价、交付天数、可供数量和供应商评分综合排序。</div></div>
        ${selectedRfq ? `<span class="chip ${rfqStatusClass(selectedRfq.status)}">${escapeHtml(rfqStatusText(selectedRfq.status))}</span>` : ""}
      </div>
      <div class="panel-body rfq-quote-grid">
        ${
          selectedRfq
            ? state.selectedRfqQuotes.length
              ? state.selectedRfqQuotes.map((quote) => renderRfqQuoteCard(quote, selectedRfq)).join("")
              : '<div class="empty">还没有供应商报价。可以打开供应商页面登录后提交报价。</div>'
            : '<div class="empty">选择一个询价单后查看报价。</div>'
        }
      </div>
    </div>
  `;
}

/**
 * 作用：生成采购方询价单卡片。
 * 输入：
 * - rfq：询价单对象。
 * 输出：返回 HTML 字符串。
 */
export function renderPurchaserRfqCard(rfq) {
  const selected = rfq.id === state.selectedRfqId;
  return `
    <article class="order-card rfq-card ${selected ? "urgent" : ""}">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(rfq.materialName)}</strong>
          <span>${escapeHtml(rfq.quantity)} · ${escapeHtml(rfq.deliveryAddress)}</span>
        </div>
        <span class="chip ${rfqStatusClass(rfq.status)}">${escapeHtml(rfqStatusText(rfq.status))}</span>
      </div>
      <div class="order-meta">
        <span class="chip">${escapeHtml(rfq.category)}</span>
        <span class="chip">${escapeHtml(rfq.unit)}</span>
        <span class="chip blue">${Number(rfq.quoteCount || 0)} 个报价</span>
      </div>
      ${
        rfq.bestQuote
          ? `<div class="rfq-best">当前最低：¥ ${escapeHtml(rfq.bestQuote.unitPrice)} · ${escapeHtml(rfq.bestQuote.supplierName)} · ${escapeHtml(rfq.bestQuote.deliveryDays)} 天</div>`
          : '<div class="muted">等待供应商报价。</div>'
      }
      <div class="order-actions">
        <button class="btn ${selected ? "btn-primary" : "btn-ghost"} btn-sm" data-view-rfq-quotes="${rfq.id}">
          ${selected ? "正在查看" : "查看报价"}
        </button>
      </div>
    </article>
  `;
}

/**
 * 作用：生成单条 RFQ 报价卡片。
 * 输入：
 * - quote：报价对象。
 * - rfq：询价单对象。
 * 输出：返回 HTML 字符串。
 */
export function renderRfqQuoteCard(quote, rfq) {
  const canAccept = rfq.status === "OPEN" && quote.status === "ACTIVE";
  return `
    <article class="task-card rfq-quote-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(quote.supplierName)}</strong>
          <span>${escapeHtml(quote.materialName)} · ${escapeHtml(quote.category)} · ${escapeHtml(quote.unit)}</span>
        </div>
        <span class="price-pill">¥ ${escapeHtml(quote.unitPrice)} / ${escapeHtml(quote.unit)}</span>
      </div>
      <div class="rfq-score">
        <strong>${escapeHtml(quote.recommendScore)}</strong>
        <span>推荐分</span>
      </div>
      <div class="order-meta">
        <span class="chip green">可供 ${escapeHtml(quote.availableQuantity)} ${escapeHtml(quote.unit)}</span>
        <span class="chip amber">${escapeHtml(quote.deliveryDays)} 天交付</span>
        <span class="chip ${quoteStatusClass(quote.status)}">${escapeHtml(quoteStatusText(quote.status))}</span>
      </div>
      ${quote.remark ? `<div class="muted">${escapeHtml(quote.remark)}</div>` : ""}
      <div class="order-actions">
        ${
          canAccept
            ? actionButton("accept-rfq-quote", quote.id, "采纳并生成订单", "btn-primary", `accept-rfq-quote:${quote.id}`)
            : '<span class="chip green">已锁定结果</span>'
        }
      </div>
    </article>
  `;
}

/**
 * 作用：生成供应商询价报价页面。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成供应商 RFQ 页面。
 */
export function supplierRfqPanel() {
  return `
    <div class="dashboard-grid">
      ${statCard("开放询价", state.supplierOpenRfqs.length, "可立即报价")}
      ${statCard("我的报价", state.supplierQuotes.length, "可更新报价")}
      ${statCard("供应货物", state.supplierMaterials.length, "报价物资来源")}
      ${statCard("已采纳", state.supplierQuotes.filter((item) => item.status === "SELECTED").length, "进入订单流程")}
    </div>
    <div class="layout-2 rfq-layout">
      <div class="panel">
        <div class="panel-head">
          <div><h2>开放询价</h2><div class="muted">选择自己的上架物资，填写单价、可供数量和交付天数后提交。</div></div>
          <button class="btn btn-ghost btn-sm" id="refreshData">刷新</button>
        </div>
        <div class="panel-body rfq-list">
          ${
            state.supplierOpenRfqs.length
              ? state.supplierOpenRfqs.map(renderSupplierOpenRfqCard).join("")
              : '<div class="empty">暂无开放询价。采购方发布 RFQ 后会出现在这里。</div>'
          }
        </div>
      </div>
      <aside class="panel detail-drawer">
        <div class="panel-head"><div><h2>我的 RFQ 报价</h2><div class="muted">采购方采纳后会自动生成供货订单。</div></div></div>
        <div class="panel-body rfq-list">
          ${
            state.supplierQuotes.length
              ? state.supplierQuotes.map(renderSupplierQuoteRecord).join("")
              : '<div class="empty">还没有提交过 RFQ 报价。</div>'
          }
        </div>
      </aside>
    </div>
  `;
}

/**
 * 作用：生成供应商可报价询价卡片。
 * 输入：
 * - rfq：开放询价对象。
 * 输出：返回 HTML 字符串。
 */
export function renderSupplierOpenRfqCard(rfq) {
  const matchingMaterial = state.supplierMaterials.find((item) => item.materialName === rfq.materialName || item.category === rfq.category);
  const loading = Boolean(state.actionLoading[`quote-rfq:${rfq.id}`]);
  return `
    <article class="order-card rfq-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(rfq.materialName)}</strong>
          <span>${escapeHtml(rfq.purchaserName)} · ${escapeHtml(rfq.deliveryAddress)}</span>
        </div>
        <span class="chip ${rfqStatusClass(rfq.status)}">${escapeHtml(rfqStatusText(rfq.status))}</span>
      </div>
      <div class="order-meta">
        <span class="chip">${escapeHtml(rfq.category)}</span>
        <span class="chip">${escapeHtml(rfq.quantity)}</span>
        <span class="chip blue">${Number(rfq.quoteCount || 0)} 个报价</span>
      </div>
      ${rfq.remark ? `<div class="muted">${escapeHtml(rfq.remark)}</div>` : ""}
      <form class="inline-form rfq-inline-form" data-rfq-quote-form="true">
        <input type="hidden" name="rfqId" value="${escapeHtml(rfq.id)}" />
        <div class="compact-field">
          <span>供应物资</span>
          <select name="supplierMaterialId" ${state.supplierMaterials.length ? "" : "disabled"}>
            <option value="">选择已上架物资</option>
            ${state.supplierMaterials.map((item) => `
              <option value="${item.id}" ${matchingMaterial?.id === item.id ? "selected" : ""}>
                ${escapeHtml(item.materialName)} / ${escapeHtml(item.unit)}
              </option>
            `).join("")}
          </select>
        </div>
        <div class="compact-field">
          <span>单价</span>
          <input name="unitPrice" inputmode="decimal" placeholder="例如 39.8" />
        </div>
        <div class="compact-field">
          <span>可供数量</span>
          <input name="availableQuantity" inputmode="numeric" placeholder="例如 300" />
        </div>
        <div class="compact-field">
          <span>交付天数</span>
          <input name="deliveryDays" inputmode="numeric" placeholder="例如 1" />
        </div>
        <div class="compact-field">
          <span>备注</span>
          <input name="remark" placeholder="例如 可当天出库" />
        </div>
        <button class="btn btn-primary btn-sm" type="submit" ${loading ? "disabled" : ""}>
          ${loading ? "提交中..." : "提交报价"}
        </button>
      </form>
    </article>
  `;
}

/**
 * 作用：生成供应商已提交报价记录。
 * 输入：
 * - quote：供应商报价对象。
 * 输出：返回 HTML 字符串。
 */
export function renderSupplierQuoteRecord(quote) {
  return `
    <article class="task-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(quote.materialName)}</strong>
          <span>RFQ-${escapeHtml(quote.rfqId)} · ${escapeHtml(quote.createdAt)}</span>
        </div>
        <span class="chip ${quoteStatusClass(quote.status)}">${escapeHtml(quoteStatusText(quote.status))}</span>
      </div>
      <div class="order-meta">
        <span class="chip blue">¥ ${escapeHtml(quote.unitPrice)} / ${escapeHtml(quote.unit)}</span>
        <span class="chip green">可供 ${escapeHtml(quote.availableQuantity)}</span>
        <span class="chip amber">${escapeHtml(quote.deliveryDays)} 天</span>
        <span class="chip">推荐分 ${escapeHtml(quote.recommendScore)}</span>
      </div>
      ${quote.remark ? `<div class="muted">${escapeHtml(quote.remark)}</div>` : ""}
    </article>
  `;
}

/**
 * 作用：生成司机工作台内容。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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
    ${rankingPanel()}
    ${ordersPanel("我的运输订单", "司机接单后在这里推进运输中和已完成状态。", state.driverOrders, false)}
    ${ordersPanel("运输订单大厅", "订单进入平台大厅后，司机可以主动抢单。", state.transportHall, true)}
  `;
}

/**
 * 作用：生成供应商卡片 HTML。
 * 输入：
 * - supplier：供应商对象，里面有公司名称、评分、物资列表等信息。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成采购方可下单的物资卡片 HTML。
 * 输入：
 * - material：物资对象，里面有名称、单位、价格等信息。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成只读物资卡片 HTML。
 * 输入：
 * - material：物资对象，里面有名称、单位、价格等信息。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成供应商可编辑的物资卡片 HTML。
 * 输入：
 * - material：物资对象，里面有名称、单位、价格等信息。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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
        <label class="compact-field">
          <span>价格</span>
          <div class="input-with-unit compact">
            <input aria-label="价格" inputmode="decimal" value="${material.supplyPrice}" data-edit-price="${material.id}" />
            <strong>元/${escapeHtml(material.unit)}</strong>
          </div>
        </label>
        <label class="compact-field">
          <span>库存</span>
          <div class="input-with-unit compact">
            <input aria-label="库存" inputmode="numeric" value="${material.stockQuantity}" data-edit-stock="${material.id}" />
            <strong>${escapeHtml(material.unit)}</strong>
          </div>
        </label>
        <label class="compact-field">
          <span>日产能</span>
          <div class="input-with-unit compact">
            <input aria-label="日产能" inputmode="numeric" value="${material.dailyCapacity}" data-edit-capacity="${material.id}" />
            <strong>${escapeHtml(material.unit)}/日</strong>
          </div>
        </label>
        <label class="compact-field">
          <span>配送半径</span>
          <div class="input-with-unit compact">
            <input aria-label="配送半径 KM" inputmode="decimal" value="${material.deliveryRadiusKm}" data-edit-radius="${material.id}" />
            <strong>KM</strong>
          </div>
        </label>
        <button class="btn btn-ghost btn-sm" data-update-material="${material.id}">保存</button>
        <button class="btn btn-danger btn-sm" data-offline-material="${material.id}">下架</button>
      </div>
    </article>
  `;
}

/**
 * 作用：生成三方履约排行榜面板 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function rankingPanel() {
  const rankings = state.fulfillmentRankings || {};
  const purchasers = rankings.purchasers || [];
  const suppliers = rankings.suppliers?.length
    ? rankings.suppliers
    : state.supplierRanking.map((item) => ({
        participantId: item.supplierId,
        displayName: item.companyName,
        ratingScore: item.ratingScore,
        rank: item.rank,
      }));
  const drivers = rankings.drivers || [];
  return `
    <div class="panel ranking-panel">
      <div class="panel-head"><div><h2>三方履约排行榜</h2><div class="muted">采购方、供应商、司机三类角色都能看到三张履约评分榜。</div></div></div>
      <div class="panel-body ranking-board">
        ${rankingList("采购方履约排行榜", purchasers)}
        ${rankingList("供应商履约排行榜", suppliers)}
        ${rankingList("司机履约排行榜", drivers)}
      </div>
    </div>
  `;
}

/**
 * 作用：生成智能调度推荐面板 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function dispatchRecommendationPanel() {
  const recommendations = state.dispatchRecommendations || { orderId: null, items: [] };
  const items = recommendations.items || [];
  if (!recommendations.orderId && !items.length) {
    return "";
  }
  return `
    <div class="panel dispatch-panel">
      <div class="panel-head">
        <div><h2>智能调度推荐</h2><div class="muted">按在线状态、发货地距离和司机评分综合排序。</div></div>
        <span class="chip blue">${escapeHtml(recommendations.orderId || "待接单订单")}</span>
      </div>
      <div class="panel-body dispatch-board">
        ${
          items.length
            ? items.map((item) => `
                <article class="dispatch-card">
                  <div class="order-top">
                    <div class="order-title">
                      <strong>#${escapeHtml(item.rank)} ${escapeHtml(item.driverName)}</strong>
                      <span>${escapeHtml(item.vehicleNo)} · ${escapeHtml(item.vehicleType)}</span>
                    </div>
                    <span class="chip ${item.online ? "green" : "amber"}">${item.online ? "在线" : "离线"}</span>
                  </div>
                  <div class="order-meta">
                    <span class="chip blue">${escapeHtml(item.distanceToOriginKm)} KM</span>
                    <span class="chip green">${escapeHtml(item.ratingScore)} 分</span>
                    <span class="chip">推荐分 ${escapeHtml(item.recommendScore)}</span>
                  </div>
                  <div class="muted">${escapeHtml(item.reason)}</div>
                </article>
              `).join("")
            : '<div class="empty">暂无可推荐司机。</div>'
        }
      </div>
    </div>
  `;
}

function rankingList(title, items) {
  return `
    <section class="ranking-list ranking-column">
      <h3>${escapeHtml(title)}</h3>
      ${
        items.length
          ? items.map((item) => `
              <div class="ranking-row">
                <span class="rank-no">#${item.rank}</span>
                <strong>${escapeHtml(item.displayName)}</strong>
                <span class="chip green">${escapeHtml(item.ratingScore)} 分</span>
              </div>
            `).join("")
          : '<div class="empty">暂无排行榜数据。</div>'
      }
    </section>
  `;
}

/**
 * 作用：生成附近供应商面板 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成司机出勤面板 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成 MQ 监控面板 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成管理员运营面板 HTML。
 * 输入：
 * - dashboard：管理员大盘数据。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成管理员供应商审核面板 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成管理员供应商审核卡片 HTML。
 * 输入：
 * - supplier：供应商对象，里面有公司名称、评分、物资列表等信息。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function renderAdminSupplierCard(supplier) {
  const riskTags = supplier.riskTags || [];
  return `
    <article class="order-card supplier-card">
      <div class="order-top">
        <div class="order-title">
          <strong>${escapeHtml(supplier.companyName)}</strong>
          <span>${escapeHtml(supplier.address)} · ${escapeHtml(supplier.licenseNo)}</span>
        </div>
        <span class="chip ${qualificationStatusClass(supplier.auditStatusCode)}">${escapeHtml(supplier.auditStatus)}</span>
      </div>
      <div class="supplier-stats">
        <span><strong>${escapeHtml(supplier.ratingScore)}</strong> 履约评分</span>
        <span><strong>${escapeHtml(supplier.materialCount)}</strong> 供货品类</span>
        <span><strong>${escapeHtml(supplier.qualificationCompletion ?? 0)}%</strong> 资料完整度</span>
      </div>
      <div class="progress"><span style="width:${Math.max(0, Math.min(100, Number(supplier.qualificationCompletion || 0)))}%"></span></div>
      <div class="order-meta">
        <span class="chip">联系人 ${escapeHtml(supplier.contactName)}</span>
        <span class="chip">电话 ${escapeHtml(supplier.contactPhone)}</span>
        <span class="chip blue">库存 ${escapeHtml(supplier.stockQuantity)}</span>
      </div>
      <div class="risk-list">
        ${riskTags.map((risk) => `<span class="chip ${risk === "资质资料完整" ? "green" : "amber"}">${escapeHtml(risk)}</span>`).join("")}
      </div>
      <div class="form-note">${escapeHtml(supplier.auditRemark || "暂无审核备注")}</div>
      <div class="order-actions">
        ${actionButton("admin-approve-supplier", supplier.supplierId, "审核通过", "btn-primary", `admin-approve-supplier:${supplier.supplierId}`)}
        ${actionButton("admin-reject-supplier", supplier.supplierId, "驳回/停用", "btn-danger", `admin-reject-supplier:${supplier.supplierId}`)}
      </div>
    </article>
  `;
}

/**
 * 作用：生成通知中心面板 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成供应商筛选栏 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成供应商店铺详情面板 HTML。
 * 输入：
 * - supplier：供应商对象，里面有公司名称、评分、物资列表等信息。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function supplierStorePanel(supplier) {
  if (!supplier) {
    return `
      <div class="store-head">
        <div>
          <h2>暂无匹配供应商</h2>
          <div class="muted">调整关键词或物资分类后再查看店铺菜单。</div>
        </div>
      </div>
      <div class="panel-body"><div class="empty">当前筛选条件下没有可下单的供应商。</div></div>
    `;
  }
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

/**
 * 作用：生成店铺概览信息面板 HTML。
 * 输入：
 * - store：供应商店铺详情数据。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成采购清单面板 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成采购清单中的一行 HTML。
 * 输入：
 * - item：列表中的一项数据。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成评价弹窗 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成采购方验收签收弹窗 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function acceptanceModalTemplate() {
  if (!state.acceptanceModal) return "";
  const order = state.acceptanceModal.order;
  return `
    <div class="modal-backdrop" role="presentation" data-close-acceptance="true">
      <section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="acceptanceTitle">
        <div class="modal-head">
          <div>
            <h2 id="acceptanceTitle">验收签收</h2>
            <div class="muted">${escapeHtml(order.id)} · ${escapeHtml(order.materialName)} · ${escapeHtml(order.quantity)}</div>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭验收弹窗" data-close-acceptance="true">×</button>
        </div>
        <form class="modal-body form-grid" id="acceptanceForm">
          <div class="form-split">
            <div class="field">
              <label for="acceptanceSigner">签收人</label>
              <input id="acceptanceSigner" name="signerName" value="${escapeHtml(state.user.displayName)}" />
            </div>
            <div class="field">
              <label for="acceptanceResult">验收结果</label>
              <select id="acceptanceResult" name="acceptanceResult">
                <option value="ACCEPTED">验收通过</option>
                <option value="EXCEPTION">异常验收</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label for="acceptanceProofUrl">凭证链接</label>
            <input id="acceptanceProofUrl" name="proofUrl" placeholder="https://files.example.com/pod.pdf" />
          </div>
          <div class="field">
            <label for="acceptanceRemark">验收备注</label>
            <textarea id="acceptanceRemark" name="remark" rows="4">数量、外观和交付地点验收通过。</textarea>
          </div>
          <div class="modal-actions">
            <button class="btn btn-ghost" type="button" data-close-acceptance="true">取消</button>
            <button class="btn btn-primary" type="submit" ${state.actionLoading[`accept-order:${order.id}`] ? "disabled" : ""}>
              ${state.actionLoading[`accept-order:${order.id}`] ? "提交中..." : "提交验收"}
            </button>
          </div>
        </form>
      </section>
    </div>
  `;
}

/**
 * 作用：生成采购方付款登记弹窗 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成付款登记弹窗。
 */
export function paymentModalTemplate() {
  if (!state.paymentModal) return "";
  const order = state.paymentModal.order;
  return `
    <div class="modal-backdrop" role="presentation" data-close-payment="true">
      <section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="paymentTitle">
        <div class="modal-head">
          <div>
            <h2 id="paymentTitle">付款登记</h2>
            <div class="muted">${escapeHtml(order.id)} · ${escapeHtml(order.materialName)} · ${escapeHtml(order.amount)}</div>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭付款弹窗" data-close-payment="true">×</button>
        </div>
        <form class="modal-body form-grid" id="paymentForm">
          <div class="form-split">
            <div class="field">
              <label for="paymentAmount">付款金额</label>
              <input id="paymentAmount" name="amount" inputmode="decimal" value="${escapeHtml(normalizeAmount(order.amount))}" />
            </div>
            <div class="field">
              <label for="paymentMethod">付款方式</label>
              <select id="paymentMethod" name="paymentMethod">
                <option value="BANK_TRANSFER">对公转账</option>
                <option value="CORPORATE_CARD">企业卡</option>
                <option value="OFFLINE">线下付款</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label for="paymentReference">付款流水号</label>
            <input id="paymentReference" name="paymentReference" placeholder="BANK-20260608-001" />
          </div>
          <div class="field">
            <label for="paymentProofUrl">付款凭证链接</label>
            <input id="paymentProofUrl" name="proofUrl" placeholder="https://files.example.com/payment.pdf" />
          </div>
          <div class="field">
            <label for="paymentRemark">付款备注</label>
            <textarea id="paymentRemark" name="remark" rows="4">验收通过后对公付款，凭证已上传。</textarea>
          </div>
          <div class="modal-actions">
            <button class="btn btn-ghost" type="button" data-close-payment="true">取消</button>
            <button class="btn btn-primary" type="submit" ${state.actionLoading[`pay-order:${order.id}`] ? "disabled" : ""}>
              ${state.actionLoading[`pay-order:${order.id}`] ? "提交中..." : "提交付款"}
            </button>
          </div>
        </form>
      </section>
    </div>
  `;
}

function normalizeAmount(amount) {
  const normalized = String(amount || "")
    .replace(/[^\d.]/g, "")
    .replace(/^0+(\d)/, "$1");
  return normalized || "";
}

/**
 * 作用：生成订单时间线弹窗 HTML。
 * 输入：
 * - 无输入参数。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成评价对象下拉选项 HTML。
 * 输入：
 * - order：订单对象，里面有订单编号、状态、采购方、供应商等信息。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：根据评价对象生成默认评价文本。
 * 输入：
 * - targetType：评价对象类型，比如供应商、采购方或司机。
 * 输出：返回文本，作为评价输入框的默认内容。
 */
export function defaultReviewText(targetType) {
  return {
    SUPPLIER: "应急物资响应及时，备货稳定，履约协同顺畅。",
    PURCHASER: "需求信息明确，确认流程顺畅，协同效率高。",
    DRIVER: "运输到场及时，回单完整，线路执行稳定。",
  }[targetType] || "履约及时，协同顺畅。";
}

/**
 * 作用：生成订单列表面板 HTML。
 * 输入：
 * - title：面板标题。
 * - desc：面板说明文字。
 * - orders：订单列表。
 * - claimable：是否显示抢单按钮，true 表示可以抢单。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成订单卡片 HTML。
 * 输入：
 * - order：订单对象，里面有订单编号、状态、采购方、供应商等信息。
 * - claimable：是否显示抢单按钮，true 表示可以抢单。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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
        ${order.status === "已完成" ? `<span class="chip ${order.acceptanceStatus === "待验收" ? "amber" : "green"}">${escapeHtml(order.acceptanceStatus || "待验收")}</span>` : ""}
        ${order.status === "已完成" && order.acceptanceStatus && order.acceptanceStatus !== "待验收" ? `<span class="chip ${paymentStatusClass(order.paymentStatus)}">${escapeHtml(order.paymentStatus || "待付款")}</span>` : ""}
      </div>
      ${orderStatusProgress(order.status)}
      <div class="muted">${escapeHtml(order.source)} · ${escapeHtml(order.pushedTo)}</div>
      ${orderRouteLine(order)}
      ${order.status === "已完成" ? `<div class="form-note">${escapeHtml(order.acceptanceSummary || "运输完成后由采购方验收签收")}</div>` : ""}
      ${order.status === "已完成" && order.acceptanceStatus && order.acceptanceStatus !== "待验收" ? `<div class="form-note">${escapeHtml(order.paymentSummary || "验收完成后由采购方登记付款凭证")}</div>` : ""}
      <div class="order-actions">
        ${actions.join("")}
      </div>
    </article>
  `;
}

function orderRouteLine(order) {
  if (!order.originAddress && !order.destinationAddress) {
    return "";
  }
  const origin = order.originAddress || "待确认发货地";
  const destination = order.destinationAddress || "待确认目的地";
  const originPoint = formatPoint(order.originLongitude, order.originLatitude);
  const destinationPoint = formatPoint(order.destinationLongitude, order.destinationLatitude);
  return `
    <div class="route-line">
      <span>${escapeHtml(origin)}</span>
      <span class="route-arrow">-></span>
      <span>${escapeHtml(destination)}</span>
      ${originPoint || destinationPoint ? `<small>${escapeHtml(originPoint)} / ${escapeHtml(destinationPoint)}</small>` : ""}
    </div>
  `;
}

function formatPoint(longitude, latitude) {
  if (longitude === null || longitude === undefined || latitude === null || latitude === undefined) {
    return "";
  }
  return `${longitude}, ${latitude}`;
}

/**
 * 作用：生成订单可操作按钮 HTML。
 * 输入：
 * - order：订单对象，里面有订单编号、状态、采购方、供应商等信息。
 * - claimable：是否显示抢单按钮，true 表示可以抢单。
 * 输出：返回 HTML 字符串，里面是当前订单可以点击的操作按钮。
 */
export function orderActions(order, claimable) {
  const actions = [];
  if (order.pushStatus === "PENDING") {
    actions.push(actionButton("read-push", order.id, "标记已读", "btn-ghost", `read-push:${order.id}`));
  }
  if (state.user.userType === "SUPPLIER" && isSupplierConfirmationStatus(order.status)) {
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
  if (state.user.userType === "PURCHASER" && order.status === "已完成" && (!order.acceptanceStatus || order.acceptanceStatus === "待验收")) {
    actions.push(`<button class="btn btn-primary btn-sm" data-accept-order="${order.id}">验收签收</button>`);
  }
  if (state.user.userType === "PURCHASER" && order.status === "已完成" && order.acceptanceStatus && order.acceptanceStatus !== "待验收" && (!order.paymentStatus || order.paymentStatus === "待付款")) {
    actions.push(`<button class="btn btn-primary btn-sm" data-pay-order="${order.id}">付款登记</button>`);
  }
  if (order.status === "已完成" && state.user.userType !== "ADMIN") {
    actions.push(`<button class="btn btn-ghost btn-sm" data-review-order="${order.id}">评价履约</button>`);
  }
  actions.push(`<button class="btn btn-ghost btn-sm" data-order-timeline="${order.id}">时间线</button>`);
  return actions;
}

function isSupplierConfirmationStatus(status) {
  return status === "待供应商确认" || status === "采购方已抢购";
}

function paymentStatusClass(status) {
  if (status === "支付超时") return "red";
  if (status === "已付款") return "green";
  return "amber";
}

/**
 * 作用：把推送状态转换成页面显示文字。
 * 输入：
 * - status：状态文本。
 * 输出：返回文本，表示给用户看的推送状态。
 */
export function pushStatusLabel(status) {
  return {
    PENDING: "未读",
    READ: "已读",
    CLAIMED: "已接单",
  }[status] || status;
}

/**
 * 作用：把订单状态转换成页面颜色样式。
 * 输入：
 * - status：状态文本。
 * 输出：返回样式名称文本，用来决定订单状态颜色。
 */
export function orderStatusClass(status) {
  if (status === "已拒单") return "red";
  if (status === "已完成" || status === "司机已接单" || status === "运输中") return "green";
  return "amber";
}

/**
 * 作用：把 RFQ 状态转换成页面显示文字。
 * 输入：
 * - status：后端状态。
 * 输出：返回中文状态文本。
 */
export function rfqStatusText(status) {
  return {
    OPEN: "报价中",
    AWARDED: "已采纳",
  }[status] || status;
}

/**
 * 作用：把 RFQ 状态转换成颜色样式。
 * 输入：
 * - status：后端状态。
 * 输出：返回样式名称文本。
 */
export function rfqStatusClass(status) {
  if (status === "AWARDED") return "green";
  return "amber";
}

/**
 * 作用：把 RFQ 报价状态转换成页面显示文字。
 * 输入：
 * - status：后端状态。
 * 输出：返回中文状态文本。
 */
export function quoteStatusText(status) {
  return {
    ACTIVE: "有效报价",
    SELECTED: "已采纳",
  }[status] || status;
}

/**
 * 作用：把 RFQ 报价状态转换成颜色样式。
 * 输入：
 * - status：后端状态。
 * 输出：返回样式名称文本。
 */
export function quoteStatusClass(status) {
  if (status === "SELECTED") return "green";
  return "blue";
}

/**
 * 作用：把供应商资质审核状态转换成颜色样式。
 * 输入：
 * - status：审核状态。
 * 输出：返回样式名称文本。
 */
export function qualificationStatusClass(status) {
  if (status === "APPROVED") return "green";
  if (status === "REJECTED") return "red";
  return "amber";
}

/**
 * 作用：把订单状态转换成进度条百分比。
 * 输入：
 * - status：状态文本。
 * 输出：返回数字，表示订单进度百分比。
 */
export function orderStatusProgress(status) {
  const progressStatus = status === "采购方已抢购" ? "待供应商确认" : status;
  const currentIndex = orderStatusFlow.indexOf(progressStatus);
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

/**
 * 作用：生成一个操作按钮 HTML。
 * 输入：
 * - dataName：按钮上的 data 属性名。
 * - dataValue：按钮上的 data 属性值。
 * - label：按钮或字段显示文字。
 * - variant：按钮样式名称。
 * - actionKey：操作标识，用来判断这个操作是否正在执行。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function actionButton(dataName, dataValue, label, variant = "btn-primary", actionKey = "") {
  const key = actionKey || `${dataName}:${dataValue}`;
  const loading = Boolean(state.actionLoading[key]);
  return `
    <button class="btn ${variant} btn-sm" data-${dataName}="${escapeHtml(dataValue)}" ${loading ? "disabled" : ""}>
      ${loading ? "处理中..." : label}
    </button>
  `;
}

/**
 * 作用：生成司机关注采购方的表格行 HTML。
 * 输入：
 * - withAction：是否显示操作按钮，true 表示显示。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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

/**
 * 作用：生成统计卡片 HTML。
 * 输入：
 * - label：按钮或字段显示文字。
 * - value：页面输入的文本或数字。
 * - trend：统计卡片上的趋势说明。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function statCard(label, value, trend) {
  return `
    <div class="stat-card">
      <span>${label}</span>
      <strong>${value}</strong>
      <div class="trend">${trend}</div>
    </div>
  `;
}

/**
 * 作用：生成用户资料面板 HTML。
 * 输入：
 * - title：面板标题。
 * - desc：面板说明文字。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
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
        <button class="btn btn-danger" type="button" data-logout>退出登录</button>
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

/**
 * 作用：生成详情项 HTML。
 * 输入：
 * - label：按钮或字段显示文字。
 * - value：页面输入的文本或数字。
 * 输出：返回 HTML 字符串，浏览器会把它显示成页面内容。
 */
export function detailItem(label, value) {
  return `<div class="detail-item"><span>${label}</span><strong>${escapeHtml(value)}</strong></div>`;
}

/**
 * 作用：根据用户类型生成默认登录用户名。
 * 输入：
 * - userType：用户类型，比如采购方、供应商、司机或管理员。
 * 输出：返回文本，作为登录表单里的默认用户名。
 */
export function defaultAuthUsername(userType) {
  return {
    SUPPLIER: "supplier01",
    PURCHASER: "purchaser01",
    DRIVER: "driver01",
    ADMIN: "admin01",
  }[userType];
}
