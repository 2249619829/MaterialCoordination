import { cartStorageKey } from "./config.js";

export function parseNumber(value) {
  const matched = String(value ?? "").match(/[0-9.]+/);
  return matched ? Number(matched[0]) : 0;
}

export function loadStoredCart() {
  try {
    const cart = JSON.parse(localStorage.getItem(cartStorageKey) || "[]");
    return Array.isArray(cart) ? cart : [];
  } catch (error) {
    return [];
  }
}

export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

export function captureFocus() {
  const element = document.activeElement;
  if (!element?.id || !["INPUT", "TEXTAREA", "SELECT"].includes(element.tagName)) return null;
  return {
    id: element.id,
    start: typeof element.selectionStart === "number" ? element.selectionStart : null,
    end: typeof element.selectionEnd === "number" ? element.selectionEnd : null,
  };
}

export function restoreFocus(focus) {
  if (!focus) return;
  const element = document.getElementById(focus.id);
  if (!element || element.disabled) return;
  element.focus();
  if (typeof element.setSelectionRange === "function" && focus.start !== null && focus.end !== null) {
    element.setSelectionRange(focus.start, focus.end);
  }
}
