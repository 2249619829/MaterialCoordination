SET SESSION cte_max_recursion_depth = 60000;

SET @bulk_purchaser_count = IFNULL(@bulk_purchaser_count, 10000);
SET @bulk_supplier_count = IFNULL(@bulk_supplier_count, 1000);
SET @bulk_material_count = IFNULL(@bulk_material_count, 3000);
SET @bulk_supplier_material_count = IFNULL(@bulk_supplier_material_count, 10000);
SET @bulk_order_count = IFNULL(@bulk_order_count, 50000);

SET @bulk_purchaser_prefix = CONVERT(IFNULL(@bulk_purchaser_prefix, 'bulk_purchaser_') USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @bulk_supplier_prefix = CONVERT(IFNULL(@bulk_supplier_prefix, 'bulk_supplier_') USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @bulk_material_prefix = CONVERT(IFNULL(@bulk_material_prefix, 'BULK-MAT-') USING utf8mb4) COLLATE utf8mb4_unicode_ci;
SET @bulk_order_prefix = CONVERT(IFNULL(@bulk_order_prefix, 'PO-BULK-') USING utf8mb4) COLLATE utf8mb4_unicode_ci;

SET @seed_purchaser_password_hash = (
    SELECT password_hash
    FROM purchaser_account
    WHERE username = 'purchaser01'
    LIMIT 1
);

SET @seed_supplier_password_hash = (
    SELECT password_hash
    FROM supplier_account
    WHERE username = 'supplier01'
    LIMIT 1
);

DELETE FROM order_review WHERE order_id LIKE CONCAT(@bulk_order_prefix, '%');
DELETE FROM order_timeline WHERE order_id LIKE CONCAT(@bulk_order_prefix, '%');
DELETE FROM order_acceptance WHERE order_id LIKE CONCAT(@bulk_order_prefix, '%');
DELETE FROM order_payment WHERE order_id LIKE CONCAT(@bulk_order_prefix, '%');
DELETE FROM order_push_record WHERE order_id LIKE CONCAT(@bulk_order_prefix, '%');
DELETE FROM purchase_order WHERE id LIKE CONCAT(@bulk_order_prefix, '%');

DELETE sm
FROM supplier_material sm
JOIN supplier_account sa ON sm.supplier_id = sa.id
WHERE sa.username LIKE CONCAT(@bulk_supplier_prefix, '%');

DELETE sm
FROM supplier_material sm
JOIN material m ON sm.material_id = m.id
WHERE m.material_code LIKE CONCAT(@bulk_material_prefix, '%');

DELETE sp
FROM supplier_profile sp
JOIN supplier_account sa ON sp.supplier_id = sa.id
WHERE sa.username LIKE CONCAT(@bulk_supplier_prefix, '%');

DELETE pp
FROM purchaser_profile pp
JOIN purchaser_account pa ON pp.purchaser_id = pa.id
WHERE pa.username LIKE CONCAT(@bulk_purchaser_prefix, '%');

DELETE FROM supplier_account WHERE username LIKE CONCAT(@bulk_supplier_prefix, '%');
DELETE FROM purchaser_account WHERE username LIKE CONCAT(@bulk_purchaser_prefix, '%');
DELETE FROM material WHERE material_code LIKE CONCAT(@bulk_material_prefix, '%');

INSERT INTO purchaser_account (username, password_hash, status)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @bulk_purchaser_count
)
SELECT
    CONCAT(@bulk_purchaser_prefix, LPAD(n, 5, '0')),
    @seed_purchaser_password_hash,
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
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @bulk_purchaser_count
)
SELECT
    account.id,
    CONCAT('Bulk Purchaser Co. ', LPAD(seq.n, 5, '0')),
    CONCAT('Buyer ', LPAD(seq.n, 5, '0')),
    CONCAT('139', LPAD(seq.n % 100000000, 8, '0')),
    CONCAT('Bulk emergency purchasing center ', LPAD(seq.n, 5, '0'))
FROM seq
JOIN purchaser_account account
    ON account.username = CONCAT(@bulk_purchaser_prefix, LPAD(seq.n, 5, '0'))
ON DUPLICATE KEY UPDATE
    company_name = VALUES(company_name),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    address = VALUES(address),
    update_time = CURRENT_TIMESTAMP;

INSERT INTO supplier_account (username, password_hash, status)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @bulk_supplier_count
)
SELECT
    CONCAT(@bulk_supplier_prefix, LPAD(n, 4, '0')),
    @seed_supplier_password_hash,
    1
FROM seq
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    status = 1,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO supplier_profile (
    supplier_id,
    company_name,
    contact_name,
    contact_phone,
    license_no,
    address,
    longitude,
    latitude,
    rating_score,
    business_license_url,
    safety_cert_url,
    insurance_cert_url,
    audit_status,
    audit_remark
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @bulk_supplier_count
)
SELECT
    account.id,
    CONCAT('Bulk Supplier Co. ', LPAD(seq.n, 4, '0')),
    CONCAT('Supplier ', LPAD(seq.n, 4, '0')),
    CONCAT('138', LPAD(seq.n % 100000000, 8, '0')),
    CONCAT('BULK-LICENSE-', LPAD(seq.n, 6, '0')),
    CONCAT('Bulk supplier warehouse ', LPAD(seq.n, 4, '0')),
    121.000000 + ((seq.n % 1000) / 10000),
    31.000000 + ((seq.n % 1000) / 10000),
    4.00 + ((seq.n % 100) / 100),
    CONCAT('https://example.com/licenses/bulk-', LPAD(seq.n, 4, '0')),
    CONCAT('https://example.com/safety/bulk-', LPAD(seq.n, 4, '0')),
    CONCAT('https://example.com/insurance/bulk-', LPAD(seq.n, 4, '0')),
    'APPROVED',
    'Bulk generated approved supplier'
FROM seq
JOIN supplier_account account
    ON account.username = CONCAT(@bulk_supplier_prefix, LPAD(seq.n, 4, '0'))
ON DUPLICATE KEY UPDATE
    company_name = VALUES(company_name),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    license_no = VALUES(license_no),
    address = VALUES(address),
    longitude = VALUES(longitude),
    latitude = VALUES(latitude),
    rating_score = VALUES(rating_score),
    business_license_url = VALUES(business_license_url),
    safety_cert_url = VALUES(safety_cert_url),
    insurance_cert_url = VALUES(insurance_cert_url),
    audit_status = VALUES(audit_status),
    audit_remark = VALUES(audit_remark),
    update_time = CURRENT_TIMESTAMP;

INSERT INTO material (
    material_code,
    material_name,
    category,
    unit,
    description,
    status
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @bulk_material_count
)
SELECT
    CONCAT(@bulk_material_prefix, LPAD(n, 5, '0')),
    CONCAT('Bulk Material ', LPAD(n, 5, '0')),
    CASE n % 6
        WHEN 0 THEN 'steel'
        WHEN 1 THEN 'cement'
        WHEN 2 THEN 'emergency'
        WHEN 3 THEN 'wood'
        WHEN 4 THEN 'stone'
        ELSE 'logistics'
    END,
    CASE n % 5
        WHEN 0 THEN 'ton'
        WHEN 1 THEN 'piece'
        WHEN 2 THEN 'roll'
        WHEN 3 THEN 'truck'
        ELSE 'box'
    END,
    CONCAT('Bulk generated material for pressure test ', LPAD(n, 5, '0')),
    1
FROM seq
ON DUPLICATE KEY UPDATE
    material_name = VALUES(material_name),
    category = VALUES(category),
    unit = VALUES(unit),
    description = VALUES(description),
    status = 1,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO supplier_material (
    supplier_id,
    material_id,
    supply_price,
    stock_quantity,
    daily_capacity,
    delivery_radius_km,
    status
)
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @bulk_supplier_material_count
),
relation_source AS (
    SELECT
        n,
        ((n - 1) % @bulk_supplier_count) + 1 AS supplier_no,
        (FLOOR((n - 1) / @bulk_supplier_count) % @bulk_material_count) + 1 AS material_no
    FROM seq
)
SELECT
    supplier.id,
    material.id,
    100.00 + (relation_source.n % 900),
    1000 + (relation_source.n % 5000),
    50 + (relation_source.n % 300),
    50.00 + (relation_source.n % 450),
    1
FROM relation_source
JOIN supplier_account supplier
    ON supplier.username = CONCAT(@bulk_supplier_prefix, LPAD(relation_source.supplier_no, 4, '0'))
JOIN material material
    ON material.material_code = CONCAT(@bulk_material_prefix, LPAD(relation_source.material_no, 5, '0'))
ON DUPLICATE KEY UPDATE
    supply_price = VALUES(supply_price),
    stock_quantity = VALUES(stock_quantity),
    daily_capacity = VALUES(daily_capacity),
    delivery_radius_km = VALUES(delivery_radius_km),
    status = 1,
    update_time = CURRENT_TIMESTAMP;

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
WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @bulk_order_count
),
order_source AS (
    SELECT
        n,
        ((n - 1) % @bulk_purchaser_count) + 1 AS purchaser_no,
        ((n - 1) % @bulk_supplier_material_count) + 1 AS relation_no
    FROM seq
),
relation_source AS (
    SELECT
        n,
        ((n - 1) % @bulk_supplier_count) + 1 AS supplier_no,
        (FLOOR((n - 1) / @bulk_supplier_count) % @bulk_material_count) + 1 AS material_no
    FROM seq
    WHERE n <= @bulk_supplier_material_count
)
SELECT
    CONCAT(@bulk_order_prefix, LPAD(order_source.n, 8, '0')),
    purchaser_account.id,
    purchaser_profile.company_name,
    supplier_account.id,
    supplier_profile.company_name,
    material.id,
    material.material_name,
    material.category,
    CONCAT(10 + (order_source.n % 500), ' ', material.unit),
    CONCAT('CNY ', (10 + (order_source.n % 500)) * (100 + (order_source.relation_no % 900))),
    CASE order_source.n % 6
        WHEN 0 THEN '待抢购'
        WHEN 1 THEN '待供应商确认'
        WHEN 2 THEN '待司机接单'
        WHEN 3 THEN '运输中'
        WHEN 4 THEN '已完成'
        ELSE '待验收'
    END,
    'bulk historical order data',
    'bulk generated push target',
    NULL,
    DATE_SUB(NOW(), INTERVAL (order_source.n % 180) DAY),
    DATE_SUB(NOW(), INTERVAL (order_source.n % 180) DAY)
FROM order_source
JOIN relation_source
    ON relation_source.n = order_source.relation_no
JOIN purchaser_account
    ON purchaser_account.username = CONCAT(@bulk_purchaser_prefix, LPAD(order_source.purchaser_no, 5, '0'))
JOIN purchaser_profile
    ON purchaser_profile.purchaser_id = purchaser_account.id
JOIN supplier_account
    ON supplier_account.username = CONCAT(@bulk_supplier_prefix, LPAD(relation_source.supplier_no, 4, '0'))
JOIN supplier_profile
    ON supplier_profile.supplier_id = supplier_account.id
JOIN material
    ON material.material_code = CONCAT(@bulk_material_prefix, LPAD(relation_source.material_no, 5, '0'))
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
    status = VALUES(status),
    source = VALUES(source),
    pushed_to = VALUES(pushed_to),
    driver_id = VALUES(driver_id),
    create_time = VALUES(create_time),
    update_time = VALUES(update_time);
