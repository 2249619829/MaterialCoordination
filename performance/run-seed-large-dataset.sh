#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MYSQL_USERNAME="${MYSQL_USERNAME:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_DATABASE="${MYSQL_DATABASE:-material_coordination}"

PURCHASERS="${PURCHASERS:-10000}"
SUPPLIERS="${SUPPLIERS:-1000}"
MATERIALS="${MATERIALS:-3000}"
SUPPLIER_MATERIALS="${SUPPLIER_MATERIALS:-10000}"
ORDERS="${ORDERS:-50000}"

mysql_args=(-u"$MYSQL_USERNAME" "$MYSQL_DATABASE")
if [[ -n "$MYSQL_PASSWORD" ]]; then
  mysql_args=(-u"$MYSQL_USERNAME" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE")
fi

started_at="$(date '+%Y-%m-%d %H:%M:%S')"
echo "large_dataset_seed_started_at=$started_at"
echo "purchasers=$PURCHASERS suppliers=$SUPPLIERS materials=$MATERIALS supplier_materials=$SUPPLIER_MATERIALS orders=$ORDERS"

mysql \
  --init-command="SET @bulk_purchaser_count=${PURCHASERS}; SET @bulk_supplier_count=${SUPPLIERS}; SET @bulk_material_count=${MATERIALS}; SET @bulk_supplier_material_count=${SUPPLIER_MATERIALS}; SET @bulk_order_count=${ORDERS};" \
  "${mysql_args[@]}" < "$BASE_DIR/performance/sql/prepare-large-dataset.sql"

mysql "${mysql_args[@]}" -N -e "
SELECT 'purchaser_account_total', COUNT(*) FROM purchaser_account;
SELECT 'bulk_purchaser_count', COUNT(*) FROM purchaser_account WHERE username LIKE 'bulk_purchaser_%';
SELECT 'supplier_account_total', COUNT(*) FROM supplier_account;
SELECT 'bulk_supplier_count', COUNT(*) FROM supplier_account WHERE username LIKE 'bulk_supplier_%';
SELECT 'material_total', COUNT(*) FROM material;
SELECT 'bulk_material_count', COUNT(*) FROM material WHERE material_code LIKE 'BULK-MAT-%';
SELECT 'supplier_material_total', COUNT(*) FROM supplier_material;
SELECT 'bulk_supplier_material_count', COUNT(*) FROM supplier_material sm JOIN supplier_account sa ON sm.supplier_id = sa.id WHERE sa.username LIKE 'bulk_supplier_%';
SELECT 'purchase_order_total', COUNT(*) FROM purchase_order;
SELECT 'bulk_order_count', COUNT(*) FROM purchase_order WHERE id LIKE 'PO-BULK-%';
"

echo "large_dataset_seed_finished_at=$(date '+%Y-%m-%d %H:%M:%S')"
