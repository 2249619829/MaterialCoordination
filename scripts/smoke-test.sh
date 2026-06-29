#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:5173}"

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name" >&2
    exit 1
  fi
}

json_query() {
  local expression="$1"
  python3 -c '
import json
import sys

data = json.load(sys.stdin)
expression = sys.argv[1]
try:
    value = eval(expression, {"__builtins__": {}}, {"data": data})
except Exception as exc:
    raise SystemExit(f"json query failed: {exc}")
if value is None:
    raise SystemExit("json query returned null")
if isinstance(value, (dict, list)):
    print(json.dumps(value, ensure_ascii=False))
else:
    print(value)
' "$expression"
}

require_command curl
require_command python3

echo "Checking frontend..."
curl -fsS -I "$FRONTEND_URL" >/dev/null

echo "Logging in as driver01..."
login_response="$(
  curl -fsS -X POST "$API_BASE/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"driver01","password":"123456","userType":"DRIVER"}'
)"
token="$(printf '%s' "$login_response" | json_query 'data["data"]["token"]')"

echo "Checking pushed transport orders..."
push_response="$(
  curl -fsS "$API_BASE/api/transport-orders/push" \
    -H "Authorization: Bearer $token"
)"
printf '%s' "$push_response" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
orders = payload.get("data") or []
if not orders:
    raise SystemExit("expected at least one pushed transport order")
order = orders[0]
required = [
    "originAddress",
    "originLongitude",
    "originLatitude",
    "destinationAddress",
    "destinationLongitude",
    "destinationLatitude",
]
missing = [field for field in required if order.get(field) in (None, "")]
if missing:
    raise SystemExit(f"transport order is missing route fields: {missing}")
print("Route fields OK on pushed order " + str(order["id"]))
'

echo "Checking owned transport tracking..."
mine_response="$(
  curl -fsS "$API_BASE/api/transport-orders/mine" \
    -H "Authorization: Bearer $token"
)"
owned_order_id="$(printf '%s' "$mine_response" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
orders = payload.get("data") or []
print(orders[0]["id"] if orders else "")
')"

if [[ -n "$owned_order_id" ]]; then
  tracking_response="$(
    curl -fsS "$API_BASE/api/transport-orders/$owned_order_id/tracking" \
      -H "Authorization: Bearer $token"
  )"
  printf '%s' "$tracking_response" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
tracking = payload.get("data") or {}
required = [
    "originAddress",
    "originLongitude",
    "originLatitude",
    "destinationAddress",
    "destinationLongitude",
    "destinationLatitude",
]
missing = [field for field in required if tracking.get(field) in (None, "")]
if missing:
    raise SystemExit(f"tracking response is missing route fields: {missing}")
print("Tracking route fields OK on order " + str(tracking["orderId"]))
'
else
  echo "No owned transport order found; skipped tracking check."
fi

echo "Smoke test passed."
