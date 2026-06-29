const defaultApiBase = window.location.protocol === "file:" || window.location.port === "5173"
  ? "http://localhost:8080"
  : "";

export const apiBase = window.MATERIAL_API_BASE || defaultApiBase;
export const cartStorageKey = "material_purchase_cart";
export const sessionTokenStorageKey = "material_token";
export const sessionUserStorageKey = "material_user";
export const savedLoginStorageKey = "material_saved_login";
export const orderStatusFlow = ["待供应商确认", "待司机接单", "司机已接单", "运输中", "已完成"];

export const roleMeta = {
  PURCHASER: {
    label: "采购方",
    nav: [
      ["home", "供应商大厅"],
      ["rfqs", "询价 RFQ"],
      ["orders", "我的采购订单"],
      ["profile", "采购方资料"],
    ],
  },
  SUPPLIER: {
    label: "供应商",
    nav: [
      ["home", "供货工作台"],
      ["materials", "供应货物"],
      ["rfqs", "询价报价"],
      ["orders", "我的供货订单"],
      ["profile", "企业资质"],
    ],
  },
  DRIVER: {
    label: "司机",
    nav: [
      ["home", "运输大厅"],
      ["push", "推送订单"],
      ["follows", "关注采购方"],
      ["profile", "车辆资料"],
    ],
  },
  ADMIN: {
    label: "平台管理员",
    nav: [
      ["home", "运营大盘"],
      ["suppliers", "供应商审核"],
      ["orders", "订单监控"],
      ["profile", "管理员资料"],
    ],
  },
};

export const icons = {
  menu:
    '<svg class="nav-icon" viewBox="0 0 24 24" fill="none"><path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
};
