SET @perf_account_count = IFNULL(@perf_account_count, 1000);
SET @perf_account_prefix = CONVERT(IFNULL(@perf_account_prefix, 'perf_purchaser_') USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @perf_order_id = CONVERT(IFNULL(@perf_order_id, 'PO-PERF-PANIC-0001') USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @perf_password_hash = (
    SELECT password_hash
    FROM purchaser_account
    WHERE username = 'purchaser01'
    LIMIT 1
);
SET @perf_supplier_id = (
    SELECT id
    FROM supplier_account
    WHERE username = 'supplier01'
    LIMIT 1
);
SET @perf_material_id = (
    SELECT id
    FROM material
    ORDER BY id
    LIMIT 1
);

INSERT INTO purchaser_account (username, password_hash, status)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @perf_account_count
)
SELECT
    CONCAT(@perf_account_prefix, LPAD(n, 4, '0')),
    @perf_password_hash,
    1
FROM seq
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    status = 1,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO purchaser_profile (
    purchaser_id,
    company_name,
    contact_name,
    contact_phone,
    address
)
SELECT
    account.id,
    CONCAT('压测采购方 ', account.username),
    'JMeter',
    '13900000000',
    '上海市应急采购压测中心'
FROM purchaser_account account
WHERE account.username LIKE CONCAT(@perf_account_prefix, '%')
ON DUPLICATE KEY UPDATE
    company_name = VALUES(company_name),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    address = VALUES(address),
    update_time = CURRENT_TIMESTAMP;

DELETE FROM order_review WHERE order_id = @perf_order_id;
DELETE FROM order_timeline WHERE order_id = @perf_order_id;
DELETE FROM order_acceptance WHERE order_id = @perf_order_id;
DELETE FROM order_payment WHERE order_id = @perf_order_id;
DELETE FROM order_push_record WHERE order_id = @perf_order_id;

INSERT INTO purchase_order (
    id,
    purchaser_id,
    purchaser_name,
    supplier_id,
    supplier_name,
    material_id,
    material_name,
    category,
    quantity,
    amount,
    status,
    source,
    pushed_to,
    driver_id,
    create_time,
    update_time
)
VALUES (
    @perf_order_id,
    (SELECT id FROM purchaser_account WHERE username = 'purchaser01' LIMIT 1),
    'Shanghai Material Purchaser Co., Ltd.',
    @perf_supplier_id,
    'Shanghai Reliable Supplier Co., Ltd.',
    @perf_material_id,
    (SELECT material_name FROM material WHERE id = @perf_material_id),
    (SELECT category FROM material WHERE id = @perf_material_id),
    '1000 吨',
    '¥ 500000',
    '待抢购',
    'JMeter 高并发抢购压测',
    '压测订单：Redis Lua 原子扣减 + RabbitMQ 异步落库',
    NULL,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    purchaser_id = VALUES(purchaser_id),
    purchaser_name = VALUES(purchaser_name),
    supplier_id = VALUES(supplier_id),
    supplier_name = VALUES(supplier_name),
    material_id = VALUES(material_id),
    material_name = VALUES(material_name),
    category = VALUES(category),
    quantity = VALUES(quantity),
    amount = VALUES(amount),
    status = '待抢购',
    source = VALUES(source),
    pushed_to = VALUES(pushed_to),
    driver_id = NULL,
    update_time = NOW();
