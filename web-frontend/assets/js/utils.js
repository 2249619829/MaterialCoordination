import { cartStorageKey } from "./config.js";

/**
 * 作用：从文本中提取数字。
 * 输入：
 * - value：页面输入的文本或数字。
 * 输出：返回数字；如果文本里没有数字，就返回 0。
 */
export function parseNumber(value) {
  const matched = String(value ?? "").match(/[0-9.]+/);
  return matched ? Number(matched[0]) : 0;
}

/**
 * 作用：根据物资单位生成默认采购数量。
 * 输入：
 * - material：物资对象，里面有名称、单位、价格等信息。
 * 输出：返回文本，比如 100 吨，用作默认采购数量。
 */
export function defaultPurchaseQuantity(material) {
  const unit = String(material?.unit || "").trim() || "件";
  return `100 ${unit}`;
}

/**
 * 作用：从浏览器本地存储读取采购清单。
 * 输入：
 * - 无输入参数。
 * 输出：返回数组，表示浏览器本地存储里的采购清单。
 */
export function loadStoredCart() {
  try {
    const cart = JSON.parse(localStorage.getItem(cartStorageKey) || "[]");
    return Array.isArray(cart) ? cart : [];
  } catch (error) {
    return [];
  }
}

export function loadSessionUser(storageKey) {
  try {
    return JSON.parse(sessionStorage.getItem(storageKey) || "null") || null;
  } catch (error) {
    sessionStorage.removeItem(storageKey);
    return null;
  }
}

/**
 * 作用：把文本中的特殊字符转义，避免被浏览器当成 HTML 执行。
 * 输入：
 * - value：页面输入的文本或数字。
 * 输出：返回安全文本，特殊字符会被替换成 HTML 实体。
 */
export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

/**
 * 作用：记录当前正在输入的表单元素和光标位置。
 * 输入：
 * - 无输入参数。
 * 输出：返回焦点对象或 null；对象里记录元素 id 和光标位置。
 */
export function captureFocus() {
  const element = document.activeElement;
  if (!element?.id || !["INPUT", "TEXTAREA", "SELECT"].includes(element.tagName)) return null;
  return {
    id: element.id,
    start: typeof element.selectionStart === "number" ? element.selectionStart : null,
    end: typeof element.selectionEnd === "number" ? element.selectionEnd : null,
  };
}

/**
 * 作用：把焦点和光标位置恢复到上次记录的元素。
 * 输入：
 * - focus：上次记录的焦点信息，包含元素 id 和光标位置。
 * 输出：无显式返回值。执行后会尝试把光标放回原来的输入框。
 */
export function restoreFocus(focus) {
  if (!focus) return;
  const element = document.getElementById(focus.id);
  if (!element || element.disabled) return;
  element.focus();
  if (typeof element.setSelectionRange === "function" && focus.start !== null && focus.end !== null) {
    element.setSelectionRange(focus.start, focus.end);
  }
}
