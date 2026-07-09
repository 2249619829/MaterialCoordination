#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
THREADS="${THREADS:-100}"
RAMP_UP="${RAMP_UP:-2}"
LOOPS="${LOOPS:-1}"
STOCK="${STOCK:-1}"
ORDER_ID="${ORDER_ID:-PO-PERF-PANIC-0001}"
ACCOUNT_PREFIX="${ACCOUNT_PREFIX:-perf_purchaser_}"
ACCOUNT_COUNT="${ACCOUNT_COUNT:-$((THREADS * LOOPS))}"
HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-8080}"
MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-material_coordination}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_CLUSTER_PORTS="${REDIS_CLUSTER_PORTS:-6379 6380 6381 6382 6383 6384}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

RUN_ID="$(date +%Y%m%d%H%M%S)-t${THREADS}-s${STOCK}"
CSV_PATH="$BASE_DIR/performance/data/purchasers.csv"
RESULT_DIR="$BASE_DIR/performance/results/$RUN_ID"
JTL_PATH="$RESULT_DIR/result.jtl"
HTML_DIR="$RESULT_DIR/html"
SUMMARY_PATH="$RESULT_DIR/summary.txt"

mkdir -p "$BASE_DIR/performance/data" "$RESULT_DIR"

mysql_args=(-u"$MYSQL_USERNAME" "$MYSQL_DATABASE")
if [[ -n "$MYSQL_PASSWORD" ]]; then
  mysql_args=(-u"$MYSQL_USERNAME" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE")
fi

redis_args=(-c -h "$REDIS_HOST" -p "$REDIS_PORT")
if [[ -n "$REDIS_PASSWORD" ]]; then
  redis_args=(-c -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD")
fi
redis_stock_key="panic:{${ORDER_ID}}:stock"
redis_buyer_pattern="panic:{${ORDER_ID}}:buyer:*"

printf 'username,password\n' > "$CSV_PATH"
for i in $(seq 1 "$ACCOUNT_COUNT"); do
  printf '%s%04d,123456\n' "$ACCOUNT_PREFIX" "$i" >> "$CSV_PATH"
done

mysql \
  --init-command="SET @perf_account_count=${ACCOUNT_COUNT}; SET @perf_account_prefix='${ACCOUNT_PREFIX}'; SET @perf_order_id='${ORDER_ID}';" \
  "${mysql_args[@]}" < "$BASE_DIR/performance/sql/prepare-panic-buy.sql"

redis-cli "${redis_args[@]}" DEL "$redis_stock_key" >/dev/null
for scan_port in $REDIS_CLUSTER_PORTS; do
  scan_args=(-c -h "$REDIS_HOST" -p "$scan_port")
  if [[ -n "$REDIS_PASSWORD" ]]; then
    scan_args=(-c -h "$REDIS_HOST" -p "$scan_port" -a "$REDIS_PASSWORD")
  fi
  redis-cli "${scan_args[@]}" --scan --pattern "$redis_buyer_pattern"
done | while read -r key; do
  [[ -n "$key" ]] && redis-cli "${redis_args[@]}" DEL "$key" >/dev/null
done
redis-cli "${redis_args[@]}" SETEX "$redis_stock_key" 7200 "$STOCK" >/dev/null

rm -rf "$HTML_DIR"

jmeter -n \
  -t "$BASE_DIR/performance/jmeter/panic-buy.jmx" \
  -l "$JTL_PATH" \
  -e -o "$HTML_DIR" \
  -JHOST="$HOST" \
  -JPORT="$PORT" \
  -JORDER_ID="$ORDER_ID" \
  -JTHREADS="$THREADS" \
  -JRAMP_UP="$RAMP_UP" \
  -JLOOPS="$LOOPS" \
  -JCSV_PATH="$CSV_PATH"

panic_rows="$(awk -F, '$3 == "panic-buy" { count++ } END { print count + 0 }' "$JTL_PATH")"
business_success="$(awk -F, '$3 == "panic-buy" && $5 == "BUSINESS_SUCCESS" { count++ } END { print count + 0 }' "$JTL_PATH")"
sold_out="$(awk -F, '$3 == "panic-buy" && $5 == "SOLD_OUT" { count++ } END { print count + 0 }' "$JTL_PATH")"
duplicate_buyer="$(awk -F, '$3 == "panic-buy" && $5 == "DUPLICATE_BUYER" { count++ } END { print count + 0 }' "$JTL_PATH")"
not_buying="$(awk -F, '$3 == "panic-buy" && $5 == "NOT_BUYING" { count++ } END { print count + 0 }' "$JTL_PATH")"
avg_ms="$(awk -F, '$3 == "panic-buy" { sum += $2; count++ } END { if (count == 0) print 0; else printf "%.2f", sum / count }' "$JTL_PATH")"
p95_ms="$(awk -F, '$3 == "panic-buy" { print $2 }' "$JTL_PATH" | sort -n | awk '{ values[NR] = $1 } END { if (NR == 0) print 0; else { idx = int(NR * 0.95); if (idx < 1) idx = 1; print values[idx] } }')"
p99_ms="$(awk -F, '$3 == "panic-buy" { print $2 }' "$JTL_PATH" | sort -n | awk '{ values[NR] = $1 } END { if (NR == 0) print 0; else { idx = int(NR * 0.99); if (idx < 1) idx = 1; print values[idx] } }')"
first_ts="$(awk -F, '$3 == "panic-buy" { if (min == 0 || $1 < min) min = $1 } END { print min + 0 }' "$JTL_PATH")"
last_ts="$(awk -F, '$3 == "panic-buy" { if ($1 > max) max = $1 } END { print max + 0 }' "$JTL_PATH")"
duration_ms=$((last_ts - first_ts + 1))
qps="$(awk -v total="$panic_rows" -v duration="$duration_ms" 'BEGIN { if (duration <= 0) print 0; else printf "%.2f", total * 1000 / duration }')"

{
  echo "run_id=$RUN_ID"
  echo "threads=$THREADS"
  echo "ramp_up=$RAMP_UP"
  echo "loops=$LOOPS"
  echo "stock=$STOCK"
  echo "order_id=$ORDER_ID"
  echo "panic_requests=$panic_rows"
  echo "business_success=$business_success"
  echo "sold_out=$sold_out"
  echo "duplicate_buyer=$duplicate_buyer"
  echo "not_buying=$not_buying"
  echo "avg_ms=$avg_ms"
  echo "p95_ms=$p95_ms"
  echo "p99_ms=$p99_ms"
  echo "qps=$qps"
  echo "jtl=$JTL_PATH"
  echo "html=$HTML_DIR/index.html"
} | tee "$SUMMARY_PATH"
