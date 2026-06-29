import assert from "node:assert/strict";
import test from "node:test";

const localItems = new Map([
  ["material_token", "local-token"],
  ["material_user", JSON.stringify({ id: 1, userType: "SUPPLIER", username: "supplier01" })],
]);
const sessionItems = new Map([
  ["material_token", "session-token"],
  ["material_user", JSON.stringify({ id: 2, userType: "DRIVER", username: "driver01" })],
]);

function storageFrom(map) {
  return {
    getItem(key) {
      return map.has(key) ? map.get(key) : null;
    },
    setItem(key, value) {
      map.set(key, String(value));
    },
    removeItem(key) {
      map.delete(key);
    },
  };
}

globalThis.localStorage = storageFrom(localItems);
globalThis.sessionStorage = storageFrom(sessionItems);
globalThis.window = {
  MATERIAL_API_BASE: "",
  location: {
    protocol: "http:",
    port: "5173",
  },
};

const { state } = await import("./state.js");
const { defaultPurchaseQuantity } = await import("./utils.js");
const { ensureSelectedSupplierId, supplierFromList } = await import("./selectors.js");

test("state loads active login from per-tab session storage", () => {
  assert.equal(state.token, "session-token");
  assert.equal(state.user.userType, "DRIVER");
});

test("ensureSelectedSupplierId moves away from missing seed supplier id", () => {
  state.selectedSupplierId = 1;
  state.suppliers = [{ id: 7, materials: [], certifications: [] }];

  ensureSelectedSupplierId();

  assert.equal(state.selectedSupplierId, 7);
});

test("supplierFromList returns null for an empty visible supplier list", () => {
  state.selectedSupplierId = 7;

  assert.equal(supplierFromList([], state.selectedSupplierId), null);
});

test("defaultPurchaseQuantity follows the material unit", () => {
  assert.equal(defaultPurchaseQuantity({ unit: "箱" }), "100 箱");
  assert.equal(defaultPurchaseQuantity({}), "100 件");
});
