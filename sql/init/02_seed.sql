SET @seed_password_hash = '$2a$10$bSRmtHthUchMxrzvIb8riuf6.yWOKfbR4lLYd4wwqkzMDudMFpphS';

INSERT INTO admin_account (username, password_hash, display_name, status)
VALUES ('admin01', @seed_password_hash, '平台运营管理员', 1)
ON DUPLICATE KEY UPDATE
    password_hash = @seed_password_hash,
    display_name = '平台运营管理员',
    status = 1;

INSERT INTO purchaser_account (username, password_hash, status)
VALUES ('purchaser01', @seed_password_hash, 1)
ON DUPLICATE KEY UPDATE
    password_hash = @seed_password_hash,
    status = 1;

SET @purchaser_id = (SELECT id FROM purchaser_account WHERE username = 'purchaser01');

INSERT INTO purchaser_profile (purchaser_id, company_name, contact_name, contact_phone, address)
VALUES (@purchaser_id, 'Shanghai Material Purchaser Co., Ltd.', 'Purchaser Contact', '13800000001', 'Shanghai, China')
ON DUPLICATE KEY UPDATE
    company_name = 'Shanghai Material Purchaser Co., Ltd.',
    contact_name = 'Purchaser Contact',
    contact_phone = '13800000001',
    address = 'Shanghai, China';

INSERT INTO supplier_account (username, password_hash, status)
VALUES ('supplier01', @seed_password_hash, 1)
ON DUPLICATE KEY UPDATE
    password_hash = @seed_password_hash,
    status = 1;

SET @supplier_id = (SELECT id FROM supplier_account WHERE username = 'supplier01');

INSERT INTO supplier_profile (
    supplier_id,
    company_name,
    contact_name,
    contact_phone,
    license_no,
    address,
    longitude,
    latitude,
    rating_score
)
VALUES (
    @supplier_id,
    'Shanghai Reliable Supplier Co., Ltd.',
    'Supplier Contact',
    '13800000002',
    'LIC-SUPPLIER-0001',
    'Pudong New Area, Shanghai, China',
    121.544000,
    31.221000,
    4.80
)
ON DUPLICATE KEY UPDATE
    company_name = 'Shanghai Reliable Supplier Co., Ltd.',
    contact_name = 'Supplier Contact',
    contact_phone = '13800000002',
    license_no = 'LIC-SUPPLIER-0001',
    address = 'Pudong New Area, Shanghai, China',
    longitude = 121.544000,
    latitude = 31.221000,
    rating_score = 4.80;

INSERT INTO driver_account (username, password_hash, status)
VALUES ('driver01', @seed_password_hash, 1)
ON DUPLICATE KEY UPDATE
    password_hash = @seed_password_hash,
    status = 1;

SET @driver_id = (SELECT id FROM driver_account WHERE username = 'driver01');

INSERT INTO driver_profile (
    driver_id,
    real_name,
    contact_phone,
    vehicle_no,
    vehicle_type,
    longitude,
    latitude,
    attendance_status,
    rating_score
)
VALUES (
    @driver_id,
    'Driver One',
    '13800000003',
    'SH-A12345',
    'Flatbed Truck',
    121.480000,
    31.230000,
    1,
    4.70
)
ON DUPLICATE KEY UPDATE
    real_name = 'Driver One',
    contact_phone = '13800000003',
    vehicle_no = 'SH-A12345',
    vehicle_type = 'Flatbed Truck',
    longitude = 121.480000,
    latitude = 31.230000,
    attendance_status = 1,
    rating_score = 4.70;

INSERT INTO material (material_code, material_name, category, unit, description, status)
VALUES ('MAT-CEMENT-001', 'P.O42.5 散装水泥', '水泥', '吨', '通用工程散装水泥', 1)
ON DUPLICATE KEY UPDATE
    material_name = 'P.O42.5 散装水泥',
    category = '水泥',
    unit = '吨',
    description = '通用工程散装水泥',
    status = 1;

INSERT INTO material (material_code, material_name, category, unit, description, status)
VALUES ('MAT-STEEL-001', 'HRB400E 抗震钢筋', '钢材', '吨', 'HRB400E 抗震热轧带肋钢筋', 1)
ON DUPLICATE KEY UPDATE
    material_name = 'HRB400E 抗震钢筋',
    category = '钢材',
    unit = '吨',
    description = 'HRB400E 抗震热轧带肋钢筋',
    status = 1;

SET @cement_material_id = (SELECT id FROM material WHERE material_code = 'MAT-CEMENT-001');
SET @steel_material_id = (SELECT id FROM material WHERE material_code = 'MAT-STEEL-001');

INSERT INTO supplier_material (
    supplier_id,
    material_id,
    supply_price,
    stock_quantity,
    daily_capacity,
    delivery_radius_km,
    status
)
VALUES (@supplier_id, @cement_material_id, 520.00, 1000, 200, 80.00, 1)
ON DUPLICATE KEY UPDATE
    supply_price = 520.00,
    stock_quantity = 1000,
    daily_capacity = 200,
    delivery_radius_km = 80.00,
    status = 1;

INSERT INTO supplier_material (
    supplier_id,
    material_id,
    supply_price,
    stock_quantity,
    daily_capacity,
    delivery_radius_km,
    status
)
VALUES (@supplier_id, @steel_material_id, 3980.00, 500, 120, 60.00, 1)
ON DUPLICATE KEY UPDATE
    supply_price = 3980.00,
    stock_quantity = 500,
    daily_capacity = 120,
    delivery_radius_km = 60.00,
    status = 1;

INSERT INTO driver_follow (driver_id, purchaser_id, follow_type)
VALUES (@driver_id, @purchaser_id, 'DRIVER_FOLLOW_PURCHASER')
ON DUPLICATE KEY UPDATE
    follow_type = 'DRIVER_FOLLOW_PURCHASER';

INSERT INTO driver_follow (driver_id, purchaser_id, follow_type)
VALUES (@driver_id, @purchaser_id, 'PURCHASER_FOLLOW_DRIVER')
ON DUPLICATE KEY UPDATE
    follow_type = 'PURCHASER_FOLLOW_DRIVER';

INSERT INTO purchaser_account (username, password_hash, status)
VALUES ('purchaser02', @seed_password_hash, 1)
ON DUPLICATE KEY UPDATE
    password_hash = @seed_password_hash,
    status = 1;

SET @purchaser2_id = (SELECT id FROM purchaser_account WHERE username = 'purchaser02');

INSERT INTO purchaser_profile (purchaser_id, company_name, contact_name, contact_phone, address)
VALUES (@purchaser2_id, 'Jiangsu Emergency Construction Group', '周主管', '13800000011', '江苏省南京市江宁应急物资中心')
ON DUPLICATE KEY UPDATE
    company_name = 'Jiangsu Emergency Construction Group',
    contact_name = '周主管',
    contact_phone = '13800000011',
    address = '江苏省南京市江宁应急物资中心';

INSERT INTO supplier_account (username, password_hash, status)
VALUES ('supplier02', @seed_password_hash, 1)
ON DUPLICATE KEY UPDATE
    password_hash = @seed_password_hash,
    status = 1;

SET @supplier2_id = (SELECT id FROM supplier_account WHERE username = 'supplier02');

INSERT INTO supplier_profile (
    supplier_id,
    company_name,
    contact_name,
    contact_phone,
    license_no,
    address,
    longitude,
    latitude,
    rating_score
)
VALUES (
    @supplier2_id,
    'Jiangsu Emergency Materials Group',
    '李主管',
    '13800000012',
    'LIC-SUPPLIER-0002',
    '江苏省南京市江宁仓',
    118.840000,
    31.950000,
    4.62
)
ON DUPLICATE KEY UPDATE
    company_name = 'Jiangsu Emergency Materials Group',
    contact_name = '李主管',
    contact_phone = '13800000012',
    license_no = 'LIC-SUPPLIER-0002',
    address = '江苏省南京市江宁仓',
    longitude = 118.840000,
    latitude = 31.950000,
    rating_score = 4.62;

INSERT INTO driver_account (username, password_hash, status)
VALUES ('driver02', @seed_password_hash, 1)
ON DUPLICATE KEY UPDATE
    password_hash = @seed_password_hash,
    status = 1;

SET @driver2_id = (SELECT id FROM driver_account WHERE username = 'driver02');

INSERT INTO driver_profile (
    driver_id,
    real_name,
    contact_phone,
    vehicle_no,
    vehicle_type,
    longitude,
    latitude,
    attendance_status,
    rating_score
)
VALUES (
    @driver2_id,
    'Driver Two',
    '13800000013',
    '苏A-E7601',
    '厢式货车',
    118.810000,
    31.960000,
    1,
    4.55
)
ON DUPLICATE KEY UPDATE
    real_name = 'Driver Two',
    contact_phone = '13800000013',
    vehicle_no = '苏A-E7601',
    vehicle_type = '厢式货车',
    longitude = 118.810000,
    latitude = 31.960000,
    attendance_status = 1,
    rating_score = 4.55;

INSERT INTO material (material_code, material_name, category, unit, description, status)
VALUES ('MAT-WOOD-001', '工程木方', '木材', '车', '应急围挡和临建用工程木方', 1)
ON DUPLICATE KEY UPDATE
    material_name = '工程木方',
    category = '木材',
    unit = '车',
    description = '应急围挡和临建用工程木方',
    status = 1;

INSERT INTO material (material_code, material_name, category, unit, description, status)
VALUES ('MAT-ALUMINUM-001', '铝型材', '金属型材', '吨', '装配式围护结构用铝型材', 1)
ON DUPLICATE KEY UPDATE
    material_name = '铝型材',
    category = '金属型材',
    unit = '吨',
    description = '装配式围护结构用铝型材',
    status = 1;

INSERT INTO material (material_code, material_name, category, unit, description, status)
VALUES ('MAT-GEOTEXTILE-001', '防汛土工布', '应急物资', '卷', '防汛抢险和临时覆盖用土工布', 1)
ON DUPLICATE KEY UPDATE
    material_name = '防汛土工布',
    category = '应急物资',
    unit = '卷',
    description = '防汛抢险和临时覆盖用土工布',
    status = 1;

SET @wood_material_id = (SELECT id FROM material WHERE material_code = 'MAT-WOOD-001');
SET @aluminum_material_id = (SELECT id FROM material WHERE material_code = 'MAT-ALUMINUM-001');
SET @geotextile_material_id = (SELECT id FROM material WHERE material_code = 'MAT-GEOTEXTILE-001');

INSERT INTO supplier_material (
    supplier_id,
    material_id,
    supply_price,
    stock_quantity,
    daily_capacity,
    delivery_radius_km,
    status
)
VALUES (@supplier2_id, @wood_material_id, 18800.00, 12, 4, 120.00, 1)
ON DUPLICATE KEY UPDATE
    supply_price = 18800.00,
    stock_quantity = 12,
    daily_capacity = 4,
    delivery_radius_km = 120.00,
    status = 1;

INSERT INTO supplier_material (
    supplier_id,
    material_id,
    supply_price,
    stock_quantity,
    daily_capacity,
    delivery_radius_km,
    status
)
VALUES (@supplier2_id, @aluminum_material_id, 4020.00, 96, 24, 100.00, 1)
ON DUPLICATE KEY UPDATE
    supply_price = 4020.00,
    stock_quantity = 96,
    daily_capacity = 24,
    delivery_radius_km = 100.00,
    status = 1;

INSERT INTO supplier_material (
    supplier_id,
    material_id,
    supply_price,
    stock_quantity,
    daily_capacity,
    delivery_radius_km,
    status
)
VALUES (@supplier2_id, @geotextile_material_id, 860.00, 300, 80, 150.00, 1)
ON DUPLICATE KEY UPDATE
    supply_price = 860.00,
    stock_quantity = 300,
    daily_capacity = 80,
    delivery_radius_km = 150.00,
    status = 1;

INSERT INTO driver_follow (driver_id, purchaser_id, follow_type)
VALUES (@driver_id, @purchaser2_id, 'DRIVER_FOLLOW_PURCHASER')
ON DUPLICATE KEY UPDATE
    follow_type = 'DRIVER_FOLLOW_PURCHASER';

INSERT INTO driver_follow (driver_id, purchaser_id, follow_type)
VALUES (@driver2_id, @purchaser_id, 'DRIVER_FOLLOW_PURCHASER')
ON DUPLICATE KEY UPDATE
    follow_type = 'DRIVER_FOLLOW_PURCHASER';

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
    'PO-SEED-20260603-0001',
    @purchaser_id,
    'Shanghai Material Purchaser Co., Ltd.',
    @supplier_id,
    'Shanghai Reliable Supplier Co., Ltd.',
    @steel_material_id,
    'HRB400E 抗震钢筋',
    '钢材',
    '80 吨',
    '¥ 318400',
    '待抢购',
    '采购方确认购货后进入平台大厅',
    '关注采购方的司机 / 采购方关注的司机',
    NULL,
    DATE_SUB(NOW(), INTERVAL 2 HOUR),
    DATE_SUB(NOW(), INTERVAL 2 HOUR)
)
ON DUPLICATE KEY UPDATE
    status = '待抢购',
    driver_id = NULL,
    pushed_to = '关注采购方的司机 / 采购方关注的司机',
    update_time = VALUES(update_time);

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
    'PO-SEED-20260603-0002',
    @purchaser2_id,
    'Jiangsu Emergency Construction Group',
    @supplier2_id,
    'Jiangsu Emergency Materials Group',
    @geotextile_material_id,
    '防汛土工布',
    '应急物资',
    '60 卷',
    '¥ 51600',
    '待抢购',
    '采购方确认购货后进入平台大厅',
    '关注采购方的司机 / 采购方关注的司机',
    NULL,
    DATE_SUB(NOW(), INTERVAL 70 MINUTE),
    DATE_SUB(NOW(), INTERVAL 70 MINUTE)
)
ON DUPLICATE KEY UPDATE
    status = '待抢购',
    driver_id = NULL,
    pushed_to = '关注采购方的司机 / 采购方关注的司机',
    update_time = VALUES(update_time);

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
    'PO-SEED-20260603-0003',
    @purchaser_id,
    'Shanghai Material Purchaser Co., Ltd.',
    @supplier2_id,
    'Jiangsu Emergency Materials Group',
    @wood_material_id,
    '工程木方',
    '木材',
    '3 车',
    '¥ 56400',
    '司机已接单',
    '采购方确认购货后进入平台大厅',
    '司机 1 已抢单',
    @driver_id,
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_SUB(NOW(), INTERVAL 18 HOUR)
)
ON DUPLICATE KEY UPDATE
    status = '司机已接单',
    driver_id = @driver_id,
    pushed_to = '司机 1 已抢单',
    update_time = VALUES(update_time);

INSERT INTO order_push_record (order_id, driver_id, purchaser_id, push_type, status, retry_count)
VALUES ('PO-SEED-20260603-0001', @driver_id, @purchaser_id, 'FOLLOW_RELATION', 'PENDING', 0)
ON DUPLICATE KEY UPDATE
    status = 'PENDING',
    retry_count = 0;

INSERT INTO order_push_record (order_id, driver_id, purchaser_id, push_type, status, retry_count)
VALUES ('PO-SEED-20260603-0002', @driver_id, @purchaser2_id, 'FOLLOW_RELATION', 'PENDING', 0)
ON DUPLICATE KEY UPDATE
    status = 'PENDING',
    retry_count = 0;

INSERT INTO order_push_record (order_id, driver_id, purchaser_id, push_type, status, retry_count)
VALUES ('PO-SEED-20260603-0003', @driver_id, @purchaser_id, 'FOLLOW_RELATION', 'CLAIMED', 0)
ON DUPLICATE KEY UPDATE
    status = 'CLAIMED',
    retry_count = 0;

INSERT INTO order_review (
    order_id,
    reviewer_type,
    reviewer_id,
    target_type,
    target_id,
    score,
    content
)
VALUES
    ('PO-SEED-20260603-0003', 'PURCHASER', @purchaser_id, 'SUPPLIER', @supplier2_id, 5, '应急订单响应及时，备货稳定'),
    ('PO-SEED-20260603-0003', 'SUPPLIER', @supplier2_id, 'PURCHASER', @purchaser_id, 5, '需求明确，确认流程顺畅'),
    ('PO-SEED-20260603-0003', 'PURCHASER', @purchaser_id, 'DRIVER', @driver_id, 4, '运输到场及时，回单完整')
ON DUPLICATE KEY UPDATE
    score = VALUES(score),
    content = VALUES(content),
    update_time = NOW();
