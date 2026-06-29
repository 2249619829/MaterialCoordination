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

INSERT INTO purchaser_profile (purchaser_id, company_name, contact_name, contact_phone, address, longitude, latitude)
VALUES (@purchaser_id, 'Shanghai Material Purchaser Co., Ltd.', 'Purchaser Contact', '13800000001', 'Shanghai, China', 121.470000, 31.230000)
ON DUPLICATE KEY UPDATE
    company_name = 'Shanghai Material Purchaser Co., Ltd.',
    contact_name = 'Purchaser Contact',
    contact_phone = '13800000001',
    address = 'Shanghai, China',
    longitude = 121.470000,
    latitude = 31.230000;

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
    rating_score,
    business_license_url,
    safety_cert_url,
    insurance_cert_url,
    audit_status,
    audit_remark
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
    4.80,
    'https://files.example.com/supplier01-license.pdf',
    'https://files.example.com/supplier01-safety.pdf',
    'https://files.example.com/supplier01-insurance.pdf',
    'APPROVED',
    '种子供应商资质已通过'
)
ON DUPLICATE KEY UPDATE
    company_name = 'Shanghai Reliable Supplier Co., Ltd.',
    contact_name = 'Supplier Contact',
    contact_phone = '13800000002',
    license_no = 'LIC-SUPPLIER-0001',
    address = 'Pudong New Area, Shanghai, China',
    longitude = 121.544000,
    latitude = 31.221000,
    rating_score = 4.80,
    business_license_url = 'https://files.example.com/supplier01-license.pdf',
    safety_cert_url = 'https://files.example.com/supplier01-safety.pdf',
    insurance_cert_url = 'https://files.example.com/supplier01-insurance.pdf',
    audit_status = 'APPROVED',
    audit_remark = '种子供应商资质已通过';

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
VALUES ('MAT-CEMENT-001', '散装水泥', '水泥', '吨', '通用工程散装水泥', 1)
ON DUPLICATE KEY UPDATE
    material_name = '散装水泥',
    category = '水泥',
    unit = '吨',
    description = '通用工程散装水泥',
    status = 1;

INSERT INTO material (material_code, material_name, category, unit, description, status)
VALUES ('MAT-STEEL-001', '抗震钢筋', '钢材', '吨', '抗震热轧带肋钢筋', 1)
ON DUPLICATE KEY UPDATE
    material_name = '抗震钢筋',
    category = '钢材',
    unit = '吨',
    description = '抗震热轧带肋钢筋',
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

INSERT INTO purchaser_profile (purchaser_id, company_name, contact_name, contact_phone, address, longitude, latitude)
VALUES (@purchaser2_id, 'Jiangsu Emergency Construction Group', '周主管', '13800000011', '江苏省南京市江宁应急物资中心', 118.820000, 31.950000)
ON DUPLICATE KEY UPDATE
    company_name = 'Jiangsu Emergency Construction Group',
    contact_name = '周主管',
    contact_phone = '13800000011',
    address = '江苏省南京市江宁应急物资中心',
    longitude = 118.820000,
    latitude = 31.950000;

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
    rating_score,
    business_license_url,
    safety_cert_url,
    insurance_cert_url,
    audit_status,
    audit_remark
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
    4.62,
    'https://files.example.com/supplier02-license.pdf',
    'https://files.example.com/supplier02-safety.pdf',
    'https://files.example.com/supplier02-insurance.pdf',
    'APPROVED',
    '种子供应商资质已通过'
)
ON DUPLICATE KEY UPDATE
    company_name = 'Jiangsu Emergency Materials Group',
    contact_name = '李主管',
    contact_phone = '13800000012',
    license_no = 'LIC-SUPPLIER-0002',
    address = '江苏省南京市江宁仓',
    longitude = 118.840000,
    latitude = 31.950000,
    rating_score = 4.62,
    business_license_url = 'https://files.example.com/supplier02-license.pdf',
    safety_cert_url = 'https://files.example.com/supplier02-safety.pdf',
    insurance_cert_url = 'https://files.example.com/supplier02-insurance.pdf',
    audit_status = 'APPROVED',
    audit_remark = '种子供应商资质已通过';

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
    '抗震钢筋',
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

UPDATE purchase_order po
JOIN supplier_profile sp ON sp.supplier_id = po.supplier_id
JOIN purchaser_profile pp ON pp.purchaser_id = po.purchaser_id
SET po.origin_address = sp.address,
    po.origin_longitude = sp.longitude,
    po.origin_latitude = sp.latitude,
    po.destination_address = pp.address,
    po.destination_longitude = pp.longitude,
    po.destination_latitude = pp.latitude
WHERE po.id IN ('PO-SEED-20260603-0001', 'PO-SEED-20260603-0002', 'PO-SEED-20260603-0003');

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

-- 扩展采购商与供应商演示数据：共 8 个采购商、20 个供应商，每个新增供应商至少 2 个中文物资。
INSERT INTO purchaser_account (username, password_hash, status)
VALUES
    ('purchaser03', @seed_password_hash, 1),
    ('purchaser04', @seed_password_hash, 1),
    ('purchaser05', @seed_password_hash, 1),
    ('purchaser06', @seed_password_hash, 1),
    ('purchaser07', @seed_password_hash, 1),
    ('purchaser08', @seed_password_hash, 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    status = VALUES(status);

INSERT INTO purchaser_profile (purchaser_id, company_name, contact_name, contact_phone, address, longitude, latitude)
SELECT pa.id, seed.company_name, seed.contact_name, seed.contact_phone, seed.address, seed.longitude, seed.latitude
FROM (
    SELECT 'purchaser03' AS username, '华东应急建设采购中心' AS company_name, '赵主任' AS contact_name, '13800000021' AS contact_phone, '浙江省杭州市萧山区临时安置中心' AS address, 120.250000 AS longitude, 30.185000 AS latitude
    UNION ALL SELECT 'purchaser04', '江海防汛工程采购部', '钱主任', '13800000022', '江苏省苏州市吴江区防汛指挥仓', 120.645000, 31.160000
    UNION ALL SELECT 'purchaser05', '中原交通抢险采购中心', '孙主任', '13800000031', '河南省郑州市航空港区抢险基地', 113.850000, 34.520000
    UNION ALL SELECT 'purchaser06', '华南临建保障采购部', '李主任', '13800000032', '广东省广州市增城区临建保障站', 113.810000, 23.260000
    UNION ALL SELECT 'purchaser07', '西南灾后重建采购中心', '周主任', '13800000033', '四川省成都市双流区应急集散中心', 104.050000, 30.480000
    UNION ALL SELECT 'purchaser08', '西北能源保障采购部', '吴主任', '13800000034', '陕西省西安市灞桥区能源保障仓', 109.090000, 34.270000
) seed
JOIN purchaser_account pa ON pa.username = seed.username
ON DUPLICATE KEY UPDATE
    company_name = VALUES(company_name),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    address = VALUES(address),
    longitude = VALUES(longitude),
    latitude = VALUES(latitude);

INSERT INTO supplier_account (username, password_hash, status)
VALUES
    ('supplier03', @seed_password_hash, 1),
    ('supplier04', @seed_password_hash, 1),
    ('supplier05', @seed_password_hash, 1),
    ('supplier06', @seed_password_hash, 1),
    ('supplier07', @seed_password_hash, 1),
    ('supplier08', @seed_password_hash, 1),
    ('supplier09', @seed_password_hash, 1),
    ('supplier10', @seed_password_hash, 1),
    ('supplier11', @seed_password_hash, 1),
    ('supplier12', @seed_password_hash, 1),
    ('supplier13', @seed_password_hash, 1),
    ('supplier14', @seed_password_hash, 1),
    ('supplier15', @seed_password_hash, 1),
    ('supplier16', @seed_password_hash, 1),
    ('supplier17', @seed_password_hash, 1),
    ('supplier18', @seed_password_hash, 1),
    ('supplier19', @seed_password_hash, 1),
    ('supplier20', @seed_password_hash, 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    status = VALUES(status);

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
SELECT
    sa.id,
    seed.company_name,
    seed.contact_name,
    seed.contact_phone,
    seed.license_no,
    seed.address,
    seed.longitude,
    seed.latitude,
    seed.rating_score,
    CONCAT('https://files.example.com/', seed.username, '-license.pdf'),
    CONCAT('https://files.example.com/', seed.username, '-safety.pdf'),
    CONCAT('https://files.example.com/', seed.username, '-insurance.pdf'),
    'APPROVED',
    '扩展供应商资质已通过'
FROM (
    SELECT 'supplier03' AS username, '上海安储应急物资有限公司' AS company_name, '陈经理' AS contact_name, '13900010003' AS contact_phone, 'LIC-SUPPLIER-0003' AS license_no, '上海市嘉定区应急物资仓' AS address, 121.265000 AS longitude, 31.375000 AS latitude, 4.91 AS rating_score
    UNION ALL SELECT 'supplier04', '苏州华储救援物资有限公司', '吴经理', '13900010004', 'LIC-SUPPLIER-0004', '江苏省苏州市吴中区保障仓', 120.641000, 31.270000, 4.74
    UNION ALL SELECT 'supplier05', '杭州民安保障供应链有限公司', '赵经理', '13900010005', 'LIC-SUPPLIER-0005', '浙江省杭州市萧山区应急仓', 120.265000, 30.183000, 4.68
    UNION ALL SELECT 'supplier06', '宁波海港应急装备有限公司', '林经理', '13900010006', 'LIC-SUPPLIER-0006', '浙江省宁波市北仑区港区仓', 121.840000, 29.910000, 4.55
    UNION ALL SELECT 'supplier07', '合肥众安工程材料有限公司', '孙经理', '13900010007', 'LIC-SUPPLIER-0007', '安徽省合肥市肥东县工程物资库', 117.476000, 31.887000, 4.47
    UNION ALL SELECT 'supplier08', '武汉洪安防汛物资有限公司', '胡经理', '13900010008', 'LIC-SUPPLIER-0008', '湖北省武汉市东西湖区防汛仓', 114.140000, 30.620000, 4.83
    UNION ALL SELECT 'supplier09', '郑州中原应急保障有限公司', '郭经理', '13900010009', 'LIC-SUPPLIER-0009', '河南省郑州市经开区保障仓', 113.760000, 34.720000, 4.36
    UNION ALL SELECT 'supplier10', '济南齐鲁物资供应有限公司', '王经理', '13900010010', 'LIC-SUPPLIER-0010', '山东省济南市历城区应急仓', 117.160000, 36.710000, 4.29
    UNION ALL SELECT 'supplier11', '青岛海盾应急科技有限公司', '刘经理', '13900010011', 'LIC-SUPPLIER-0011', '山东省青岛市黄岛区救援装备仓', 120.180000, 35.960000, 4.52
    UNION ALL SELECT 'supplier12', '福州闽安救援装备有限公司', '何经理', '13900010012', 'LIC-SUPPLIER-0012', '福建省福州市仓山区物资库', 119.310000, 26.020000, 4.41
    UNION ALL SELECT 'supplier13', '厦门海陆通物资有限公司', '郑经理', '13900010013', 'LIC-SUPPLIER-0013', '福建省厦门市集美区应急仓', 118.100000, 24.570000, 4.25
    UNION ALL SELECT 'supplier14', '广州粤安应急供应链有限公司', '黄经理', '13900010014', 'LIC-SUPPLIER-0014', '广东省广州市黄埔区保障仓', 113.450000, 23.110000, 4.79
    UNION ALL SELECT 'supplier15', '深圳鹏城救援物资有限公司', '梁经理', '13900010015', 'LIC-SUPPLIER-0015', '广东省深圳市龙岗区救援仓', 114.250000, 22.720000, 4.63
    UNION ALL SELECT 'supplier16', '南昌赣江工程物资有限公司', '邹经理', '13900010016', 'LIC-SUPPLIER-0016', '江西省南昌市新建区工程物资库', 115.800000, 28.690000, 4.18
    UNION ALL SELECT 'supplier17', '长沙湘安保障物资有限公司', '彭经理', '13900010017', 'LIC-SUPPLIER-0017', '湖南省长沙市雨花区应急仓', 113.020000, 28.150000, 4.57
    UNION ALL SELECT 'supplier18', '成都川安应急装备有限公司', '唐经理', '13900010018', 'LIC-SUPPLIER-0018', '四川省成都市龙泉驿区装备库', 104.270000, 30.570000, 4.66
    UNION ALL SELECT 'supplier19', '重庆山城救援物资有限公司', '蒋经理', '13900010019', 'LIC-SUPPLIER-0019', '重庆市九龙坡区救援仓', 106.480000, 29.510000, 4.33
    UNION ALL SELECT 'supplier20', '西安秦安物资保障有限公司', '马经理', '13900010020', 'LIC-SUPPLIER-0020', '陕西省西安市未央区物资保障库', 108.950000, 34.330000, 4.22
) seed
JOIN supplier_account sa ON sa.username = seed.username
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
    audit_remark = VALUES(audit_remark);

INSERT INTO material (material_code, material_name, category, unit, description, status)
VALUES
    ('MAT-DEMO-001', '应急帐篷', '应急安置', '顶', '临时安置和现场指挥使用', 1),
    ('MAT-DEMO-002', '折叠床', '应急安置', '张', '临时安置点快速铺设使用', 1),
    ('MAT-DEMO-003', '救援棉被', '应急安置', '床', '受灾群众保暖安置使用', 1),
    ('MAT-DEMO-004', '饮用水', '生活保障', '箱', '应急现场生活饮水保障', 1),
    ('MAT-DEMO-005', '方便食品', '生活保障', '箱', '应急现场快速餐食保障', 1),
    ('MAT-DEMO-006', '医用口罩', '医疗防护', '箱', '救援现场基础防护使用', 1),
    ('MAT-DEMO-007', '消毒液', '医疗防护', '桶', '临时安置区消杀使用', 1),
    ('MAT-DEMO-008', '手持照明灯', '照明通信', '台', '夜间抢险照明使用', 1),
    ('MAT-DEMO-009', '移动电源', '照明通信', '个', '通信和照明设备供电使用', 1),
    ('MAT-DEMO-010', '编织袋', '防汛排涝', '包', '防汛沙袋装填使用', 1),
    ('MAT-DEMO-011', '防洪沙袋', '防汛排涝', '条', '堤坝加固和挡水使用', 1),
    ('MAT-DEMO-012', '抽水泵', '防汛排涝', '台', '积水点快速排涝使用', 1),
    ('MAT-DEMO-013', '雨衣雨鞋套装', '施工安全', '套', '雨天抢险人员防护使用', 1),
    ('MAT-DEMO-014', '安全头盔', '施工安全', '顶', '抢险施工头部防护使用', 1),
    ('MAT-DEMO-015', '反光背心', '施工安全', '件', '夜间抢险识别防护使用', 1),
    ('MAT-DEMO-016', '警戒围栏', '施工安全', '米', '危险区域临时隔离使用', 1),
    ('MAT-DEMO-017', '临时配电箱', '电力保障', '台', '应急现场临时配电使用', 1),
    ('MAT-DEMO-018', '电缆卷盘', '电力保障', '盘', '临时供电线路延展使用', 1),
    ('MAT-DEMO-019', '救援绳索', '救援装备', '卷', '抢险救援牵引和固定使用', 1),
    ('MAT-DEMO-020', '防滑钢板', '工程材料', '块', '泥泞道路临时通行铺设', 1),
    ('MAT-DEMO-021', '装配式围挡', '临建材料', '平方米', '应急施工现场围挡使用', 1),
    ('MAT-DEMO-022', '预制活动板房', '临建材料', '间', '现场临时办公和安置使用', 1),
    ('MAT-DEMO-023', '防水篷布', '临建材料', '卷', '物资覆盖和临时防雨使用', 1),
    ('MAT-DEMO-024', '保温棉毡', '临建材料', '卷', '低温环境物资保温使用', 1),
    ('MAT-DEMO-025', '净水设备', '医疗净水', '台', '临时安置点饮水净化使用', 1),
    ('MAT-DEMO-026', '应急药箱', '医疗净水', '套', '救援现场基础医疗处置使用', 1),
    ('MAT-DEMO-027', '发电机组', '能源保障', '台', '断电场景临时供电使用', 1),
    ('MAT-DEMO-028', '油料储桶', '能源保障', '桶', '发电和工程车辆油料周转使用', 1),
    ('MAT-DEMO-029', '装卸托盘', '仓储周转', '个', '仓库和现场装卸周转使用', 1),
    ('MAT-DEMO-030', '塑料周转箱', '仓储周转', '箱', '小件物资分拣配送使用', 1),
    ('MAT-DEMO-031', '低温保温箱', '冷链保障', '箱', '药品和特殊物资冷链周转使用', 1),
    ('MAT-DEMO-032', '冷链冰排', '冷链保障', '块', '冷链箱降温保冷使用', 1),
    ('MAT-DEMO-033', '消防水带', '消防救援', '卷', '应急灭火和供水使用', 1),
    ('MAT-DEMO-034', '灭火器', '消防救援', '具', '现场初期火情处置使用', 1),
    ('MAT-DEMO-035', '道路锥桶', '交通管制', '个', '抢险道路临时分流使用', 1),
    ('MAT-DEMO-036', '指挥对讲机', '照明通信', '台', '现场调度通信保障使用', 1)
ON DUPLICATE KEY UPDATE
    material_name = VALUES(material_name),
    category = VALUES(category),
    unit = VALUES(unit),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO supplier_material (
    supplier_id,
    material_id,
    supply_price,
    stock_quantity,
    daily_capacity,
    delivery_radius_km,
    status
)
SELECT sa.id, m.id, seed.supply_price, seed.stock_quantity, seed.daily_capacity, seed.delivery_radius_km, 1
FROM (
    SELECT 'supplier03' AS username, 'MAT-DEMO-001' AS material_code, 1280.00 AS supply_price, 260 AS stock_quantity, 40 AS daily_capacity, 180.00 AS delivery_radius_km
    UNION ALL SELECT 'supplier03', 'MAT-DEMO-002', 185.00, 1200, 180, 180.00
    UNION ALL SELECT 'supplier04', 'MAT-DEMO-003', 96.00, 1800, 260, 160.00
    UNION ALL SELECT 'supplier04', 'MAT-DEMO-004', 42.00, 3200, 500, 140.00
    UNION ALL SELECT 'supplier05', 'MAT-DEMO-005', 58.00, 2600, 420, 150.00
    UNION ALL SELECT 'supplier05', 'MAT-DEMO-006', 78.00, 1800, 360, 150.00
    UNION ALL SELECT 'supplier06', 'MAT-DEMO-007', 126.00, 600, 120, 130.00
    UNION ALL SELECT 'supplier06', 'MAT-DEMO-008', 310.00, 420, 80, 130.00
    UNION ALL SELECT 'supplier07', 'MAT-DEMO-009', 260.00, 700, 120, 160.00
    UNION ALL SELECT 'supplier07', 'MAT-DEMO-010', 390.00, 900, 180, 160.00
    UNION ALL SELECT 'supplier08', 'MAT-DEMO-011', 12.00, 50000, 8000, 200.00
    UNION ALL SELECT 'supplier08', 'MAT-DEMO-012', 4800.00, 90, 18, 220.00
    UNION ALL SELECT 'supplier09', 'MAT-DEMO-013', 145.00, 850, 150, 170.00
    UNION ALL SELECT 'supplier09', 'MAT-DEMO-014', 42.00, 1600, 300, 170.00
    UNION ALL SELECT 'supplier10', 'MAT-DEMO-015', 28.00, 3000, 600, 150.00
    UNION ALL SELECT 'supplier10', 'MAT-DEMO-016', 36.00, 2200, 350, 150.00
    UNION ALL SELECT 'supplier11', 'MAT-DEMO-017', 1680.00, 120, 24, 180.00
    UNION ALL SELECT 'supplier11', 'MAT-DEMO-018', 520.00, 260, 50, 180.00
    UNION ALL SELECT 'supplier12', 'MAT-DEMO-019', 220.00, 500, 90, 160.00
    UNION ALL SELECT 'supplier12', 'MAT-DEMO-020', 860.00, 300, 60, 160.00
    UNION ALL SELECT 'supplier13', 'MAT-DEMO-021', 95.00, 6000, 900, 140.00
    UNION ALL SELECT 'supplier13', 'MAT-DEMO-022', 18600.00, 30, 6, 140.00
    UNION ALL SELECT 'supplier14', 'MAT-DEMO-023', 680.00, 460, 90, 180.00
    UNION ALL SELECT 'supplier14', 'MAT-DEMO-024', 520.00, 520, 100, 180.00
    UNION ALL SELECT 'supplier15', 'MAT-DEMO-025', 9800.00, 45, 8, 160.00
    UNION ALL SELECT 'supplier15', 'MAT-DEMO-026', 360.00, 380, 70, 160.00
    UNION ALL SELECT 'supplier16', 'MAT-DEMO-027', 12500.00, 38, 6, 190.00
    UNION ALL SELECT 'supplier16', 'MAT-DEMO-028', 220.00, 460, 80, 190.00
    UNION ALL SELECT 'supplier17', 'MAT-DEMO-029', 88.00, 2800, 500, 170.00
    UNION ALL SELECT 'supplier17', 'MAT-DEMO-030', 46.00, 4200, 720, 170.00
    UNION ALL SELECT 'supplier18', 'MAT-DEMO-031', 680.00, 240, 48, 150.00
    UNION ALL SELECT 'supplier18', 'MAT-DEMO-032', 18.00, 6000, 1000, 150.00
    UNION ALL SELECT 'supplier19', 'MAT-DEMO-033', 320.00, 620, 120, 160.00
    UNION ALL SELECT 'supplier19', 'MAT-DEMO-034', 118.00, 900, 160, 160.00
    UNION ALL SELECT 'supplier20', 'MAT-DEMO-035', 32.00, 3600, 650, 180.00
    UNION ALL SELECT 'supplier20', 'MAT-DEMO-036', 420.00, 520, 100, 180.00
) seed
JOIN supplier_account sa ON sa.username = seed.username
JOIN material m ON m.material_code = seed.material_code
ON DUPLICATE KEY UPDATE
    supply_price = VALUES(supply_price),
    stock_quantity = VALUES(stock_quantity),
    daily_capacity = VALUES(daily_capacity),
    delivery_radius_km = VALUES(delivery_radius_km),
    status = VALUES(status);
