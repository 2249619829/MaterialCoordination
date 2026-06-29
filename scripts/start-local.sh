#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$RUN_DIR/logs"
PID_DIR="$RUN_DIR/pids"
NACOS_DISCOVERY_IP="${NACOS_DISCOVERY_IP:-127.0.0.1}"
KEEP_ALIVE=false
STARTED_PIDS=()

if [[ "${1:-}" == "--keep-alive" ]]; then
  KEEP_ALIVE=true
fi

mkdir -p "$LOG_DIR" "$PID_DIR"

is_port_listening() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

wait_for_port() {
  local port="$1"
  local name="$2"
  local attempts="${3:-180}"
  for _ in $(seq 1 "$attempts"); do
    if is_port_listening "$port"; then
      echo "$name is listening on port $port"
      return 0
    fi
    sleep 1
  done
  echo "$name did not start on port $port. Check logs in $LOG_DIR" >&2
  return 1
}

start_java_service() {
  local module="$1"
  local port="$2"
  local pid_file="$PID_DIR/$module.pid"
  local log_file="$LOG_DIR/$module.log"

  if is_port_listening "$port"; then
    echo "$module already has a process listening on port $port"
    return 0
  fi

  echo "Starting $module on port $port..."
  nohup bash -lc '
    root_dir="$1"
    nacos_discovery_ip="$2"
    module="$3"
    cd "$root_dir"
    source "$root_dir/use-java21.sh"
    export NACOS_DISCOVERY_IP="$nacos_discovery_ip"
    exec mvn -q -pl "$module" spring-boot:run
  ' bash "$ROOT_DIR" "$NACOS_DISCOVERY_IP" "$module" >"$log_file" 2>&1 </dev/null &
  local pid=$!
  echo "$pid" >"$pid_file"
  STARTED_PIDS+=("$pid")
  wait_for_port "$port" "$module"
}

start_frontend() {
  local port=5173
  local pid_file="$PID_DIR/web-frontend.pid"
  local log_file="$LOG_DIR/web-frontend.log"

  if is_port_listening "$port"; then
    echo "web-frontend already has a process listening on port $port"
    return 0
  fi

  echo "Starting web-frontend on port $port..."
  nohup bash -lc '
    root_dir="$1"
    port="$2"
    cd "$root_dir/web-frontend"
    exec python3 -m http.server "$port"
  ' bash "$ROOT_DIR" "$port" >"$log_file" 2>&1 </dev/null &
  local pid=$!
  echo "$pid" >"$pid_file"
  STARTED_PIDS+=("$pid")
  wait_for_port "$port" "web-frontend" 20
}

start_java_service "auth-service" 8081
start_java_service "gateway-service" 8080
start_frontend

echo
echo "Local platform is starting/running:"
echo "- Frontend: http://localhost:5173"
echo "- Gateway:  http://localhost:8080"
echo "- Auth:     http://localhost:8081"
echo "- Logs:     $LOG_DIR"
echo
echo "Run scripts/smoke-test.sh to verify the main demo path."

if [[ "$KEEP_ALIVE" == true ]]; then
  echo
  echo "Keep-alive mode is active. Press Ctrl+C to stop services started by this script."
  trap "$ROOT_DIR/scripts/stop-local.sh; exit 0" INT TERM
  wait "${STARTED_PIDS[@]}"
fi
