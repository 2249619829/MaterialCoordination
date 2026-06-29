#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.run/pids"

stop_pid_file() {
  local pid_file="$1"
  local name
  name="$(basename "$pid_file" .pid)"

  if [[ ! -f "$pid_file" ]]; then
    return 0
  fi

  local pid
  pid="$(cat "$pid_file")"
  if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
    echo "Stopping $name (pid $pid)..."
    kill "$pid" >/dev/null 2>&1 || true
  fi
  rm -f "$pid_file"
}

if [[ -d "$PID_DIR" ]]; then
  for pid_file in "$PID_DIR"/*.pid; do
    [[ -e "$pid_file" ]] || continue
    stop_pid_file "$pid_file"
  done
fi

sleep 2

remaining="$(lsof -nP -iTCP -sTCP:LISTEN | grep -E ':8080|:8081|:5173' || true)"
if [[ -n "$remaining" ]]; then
  echo
  echo "Some project ports are still listening:"
  echo "$remaining"
  echo "If these are old manual runs, stop them from their terminal or kill the listed PIDs."
else
  echo "Local platform stopped."
fi
