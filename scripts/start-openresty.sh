#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
OPENRESTY_DIR="$RUN_DIR/openresty"
CONF_DIR="$OPENRESTY_DIR/conf"
LOG_DIR="$OPENRESTY_DIR/logs"
PID_FILE="$LOG_DIR/nginx.pid"
TEMPLATE_FILE="$ROOT_DIR/infra/openresty/nginx.conf.template"
CONF_FILE="$CONF_DIR/nginx.conf"

OPENRESTY_PORT="${OPENRESTY_PORT:-8088}"
GATEWAY_HOST="${GATEWAY_HOST:-127.0.0.1}"
GATEWAY_PORT="${GATEWAY_PORT:-8080}"
RATE_LIMIT_AUTH_REPLENISH_RATE="${RATE_LIMIT_AUTH_REPLENISH_RATE:-1}"
RATE_LIMIT_AUTH_BURST_CAPACITY="${RATE_LIMIT_AUTH_BURST_CAPACITY:-3}"
RATE_LIMIT_AUTH_REQUESTED_TOKENS="${RATE_LIMIT_AUTH_REQUESTED_TOKENS:-1}"
RATE_LIMIT_SENSITIVE_REPLENISH_RATE="${RATE_LIMIT_SENSITIVE_REPLENISH_RATE:-1}"
RATE_LIMIT_SENSITIVE_BURST_CAPACITY="${RATE_LIMIT_SENSITIVE_BURST_CAPACITY:-2}"
RATE_LIMIT_SENSITIVE_REQUESTED_TOKENS="${RATE_LIMIT_SENSITIVE_REQUESTED_TOKENS:-1}"
RATE_LIMIT_API_REPLENISH_RATE="${RATE_LIMIT_API_REPLENISH_RATE:-20}"
RATE_LIMIT_API_BURST_CAPACITY="${RATE_LIMIT_API_BURST_CAPACITY:-40}"
RATE_LIMIT_API_REQUESTED_TOKENS="${RATE_LIMIT_API_REQUESTED_TOKENS:-1}"

find_openresty() {
  if command -v openresty >/dev/null 2>&1; then
    command -v openresty
    return 0
  fi

  local candidate
  for candidate in \
    /opt/homebrew/bin/openresty \
    /usr/local/bin/openresty \
    /opt/openresty/bin/openresty; do
    if [[ -x "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  done

  return 1
}

is_port_listening() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

OPENRESTY_BIN="$(find_openresty || true)"
if [[ -z "$OPENRESTY_BIN" ]]; then
  echo "OpenResty is not installed." >&2
  echo "Install it with: brew tap openresty/brew && brew trust openresty/brew && brew install openresty/brew/openresty --without-geoip" >&2
  exit 1
fi

if is_port_listening "$OPENRESTY_PORT"; then
  echo "OpenResty entry port $OPENRESTY_PORT is already listening."
  echo "Visit: http://127.0.0.1:$OPENRESTY_PORT"
  exit 0
fi

mkdir -p "$CONF_DIR" "$LOG_DIR"

sed \
  -e "s#__ROOT_DIR__#$ROOT_DIR#g" \
  -e "s#__OPENRESTY_PORT__#$OPENRESTY_PORT#g" \
  -e "s#__GATEWAY_HOST__#$GATEWAY_HOST#g" \
  -e "s#__GATEWAY_PORT__#$GATEWAY_PORT#g" \
  -e "s#__RATE_LIMIT_AUTH_REPLENISH_RATE__#$RATE_LIMIT_AUTH_REPLENISH_RATE#g" \
  -e "s#__RATE_LIMIT_AUTH_BURST_CAPACITY__#$RATE_LIMIT_AUTH_BURST_CAPACITY#g" \
  -e "s#__RATE_LIMIT_AUTH_REQUESTED_TOKENS__#$RATE_LIMIT_AUTH_REQUESTED_TOKENS#g" \
  -e "s#__RATE_LIMIT_SENSITIVE_REPLENISH_RATE__#$RATE_LIMIT_SENSITIVE_REPLENISH_RATE#g" \
  -e "s#__RATE_LIMIT_SENSITIVE_BURST_CAPACITY__#$RATE_LIMIT_SENSITIVE_BURST_CAPACITY#g" \
  -e "s#__RATE_LIMIT_SENSITIVE_REQUESTED_TOKENS__#$RATE_LIMIT_SENSITIVE_REQUESTED_TOKENS#g" \
  -e "s#__RATE_LIMIT_API_REPLENISH_RATE__#$RATE_LIMIT_API_REPLENISH_RATE#g" \
  -e "s#__RATE_LIMIT_API_BURST_CAPACITY__#$RATE_LIMIT_API_BURST_CAPACITY#g" \
  -e "s#__RATE_LIMIT_API_REQUESTED_TOKENS__#$RATE_LIMIT_API_REQUESTED_TOKENS#g" \
  "$TEMPLATE_FILE" >"$CONF_FILE"

"$OPENRESTY_BIN" -p "$OPENRESTY_DIR/" -c "$CONF_FILE" -t
"$OPENRESTY_BIN" -p "$OPENRESTY_DIR/" -c "$CONF_FILE"

echo "OpenResty started."
echo "- Entry:   http://127.0.0.1:$OPENRESTY_PORT"
echo "- Gateway: http://$GATEWAY_HOST:$GATEWAY_PORT"
echo "- Config:  $CONF_FILE"
echo "- PID:     $PID_FILE"
