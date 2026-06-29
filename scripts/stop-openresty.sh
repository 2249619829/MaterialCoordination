#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OPENRESTY_DIR="$ROOT_DIR/.run/openresty"
CONF_FILE="$OPENRESTY_DIR/conf/nginx.conf"
PID_FILE="$OPENRESTY_DIR/logs/nginx.pid"

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

OPENRESTY_BIN="$(find_openresty || true)"

if [[ -n "$OPENRESTY_BIN" && -f "$CONF_FILE" ]]; then
  "$OPENRESTY_BIN" -p "$OPENRESTY_DIR/" -c "$CONF_FILE" -s stop || true
elif [[ -f "$PID_FILE" ]]; then
  pid="$(cat "$PID_FILE")"
  if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
    kill "$pid" >/dev/null 2>&1 || true
  fi
else
  echo "OpenResty is not running from this project."
  exit 0
fi

rm -f "$PID_FILE"
echo "OpenResty stopped."
