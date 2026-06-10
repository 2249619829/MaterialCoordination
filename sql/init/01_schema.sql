CREATE TABLE IF NOT EXISTS purchaser_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchaser_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Relationship IDs intentionally use indexes/unique constraints without DB-level
-- foreign keys to keep service boundaries and local initialization loose.
CREATE TABLE IF NOT EXISTS purchaser_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    purchaser_id BIGINT NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6) DEFAULT NULL,
    latitude DECIMAL(10,6) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchaser_profile_purchaser_id (purchaser_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS supplier_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS supplier_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    license_no VARCHAR(64) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6) DEFAULT NULL,
    latitude DECIMAL(10,6) DEFAULT NULL,
    rating_score DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    business_license_url VARCHAR(255) DEFAULT NULL,
    safety_cert_url VARCHAR(255) DEFAULT NULL,
    insurance_cert_url VARCHAR(255) DEFAULT NULL,
    audit_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    audit_remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_profile_supplier_id (supplier_id),
    CONSTRAINT chk_supplier_profile_rating_score CHECK (rating_score >= 0.00 AND rating_score <= 5.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS driver_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_driver_account_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS driver_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    driver_id BIGINT NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    vehicle_no VARCHAR(32) NOT NULL,
    vehicle_type VARCHAR(64) NOT NULL,
    longitude DECIMAL(10,6) DEFAULT NULL,
    latitude DECIMAL(10,6) DEFAULT NULL,
    attendance_status TINYINT NOT NULL DEFAULT 0,
    rating_score DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_driver_profile_driver_id (driver_id),
    CONSTRAINT chk_driver_profile_rating_score CHECK (rating_score >= 0.00 AND rating_score <= 5.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS material (
    id BIGINT NOT NULL AUTO_INCREMENT,
    material_code VARCHAR(64) NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_code (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS supplier_material (
    id BIGINT NOT NULL AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    supply_price DECIMAL(10,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    daily_capacity INT NOT NULL DEFAULT 0,
    delivery_radius_km DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_material_supplier_material (supplier_id, material_id),
    KEY idx_supplier_material_material_id (material_id),
    CONSTRAINT chk_supplier_material_supply_price CHECK (supply_price >= 0.00),
    CONSTRAINT chk_supplier_material_stock_quantity CHECK (stock_quantity >= 0),
    CONSTRAINT chk_supplier_material_daily_capacity CHECK (daily_capacity >= 0),
    CONSTRAINT chk_supplier_material_delivery_radius CHECK (delivery_radius_km >= 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_order (
    id VARCHAR(40) NOT NULL,
    purchaser_id BIGINT NOT NULL,
    purchaser_name VARCHAR(128) NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_name VARCHAR(128) NOT NULL,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    quantity VARCHAR(64) NOT NULL,
    amount VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source VARCHAR(128) NOT NULL,
    pushed_to VARCHAR(128) NOT NULL,
    driver_id BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_purchase_order_purchaser_id (purchaser_id),
    KEY idx_purchase_order_supplier_id (supplier_id),
    KEY idx_purchase_order_status (status),
    KEY idx_purchase_order_driver_id (driver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_rfq (
    id BIGINT NOT NULL AUTO_INCREMENT,
    purchaser_id BIGINT NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    quantity VARCHAR(64) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6) DEFAULT NULL,
    latitude DECIMAL(10,6) DEFAULT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    selected_quote_id BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_purchase_rfq_purchaser_status (purchaser_id, status),
    KEY idx_purchase_rfq_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_rfq_quote (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rfq_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_material_id BIGINT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    delivery_days INT NOT NULL DEFAULT 0,
    remark VARCHAR(255) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_rfq_quote_supplier (rfq_id, supplier_id),
    KEY idx_purchase_rfq_quote_rfq (rfq_id),
    KEY idx_purchase_rfq_quote_supplier (supplier_id),
    CONSTRAINT chk_purchase_rfq_quote_unit_price CHECK (unit_price >= 0.00),
    CONSTRAINT chk_purchase_rfq_quote_available_quantity CHECK (available_quantity >= 0),
    CONSTRAINT chk_purchase_rfq_quote_delivery_days CHECK (delivery_days >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS driver_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    driver_id BIGINT NOT NULL,
    purchaser_id BIGINT NOT NULL,
    follow_type VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_driver_follow_relation (driver_id, purchaser_id, follow_type),
    KEY idx_driver_follow_driver_id (driver_id),
    KEY idx_driver_follow_purchaser_id (purchaser_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_push_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(40) NOT NULL,
    driver_id BIGINT NOT NULL,
    purchaser_id BIGINT NOT NULL,
    push_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_push_order_driver (order_id, driver_id),
    KEY idx_order_push_driver_status (driver_id, status),
    KEY idx_order_push_order_id (order_id),
    KEY idx_order_push_purchaser_id (purchaser_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(40) NOT NULL,
    reviewer_type VARCHAR(32) NOT NULL,
    reviewer_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    score TINYINT NOT NULL,
    content VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_review_once (order_id, reviewer_type, reviewer_id, target_type, target_id),
    KEY idx_order_review_order_id (order_id),
    KEY idx_order_review_target (target_type, target_id),
    CONSTRAINT chk_order_review_score CHECK (score >= 1 AND score <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_acceptance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(40) NOT NULL,
    purchaser_id BIGINT NOT NULL,
    signer_name VARCHAR(64) NOT NULL,
    acceptance_result VARCHAR(32) NOT NULL DEFAULT 'ACCEPTED',
    proof_url VARCHAR(255) DEFAULT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_acceptance_order (order_id),
    KEY idx_order_acceptance_purchaser (purchaser_id),
    KEY idx_order_acceptance_result (acceptance_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(40) NOT NULL,
    purchaser_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL,
    payment_reference VARCHAR(80) NOT NULL,
    proof_url VARCHAR(255) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(255) DEFAULT NULL,
    paid_time DATETIME DEFAULT NULL,
    expires_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_payment_order (order_id),
    KEY idx_order_payment_purchaser (purchaser_id),
    KEY idx_order_payment_status (status),
    KEY idx_order_payment_expires_at (expires_at),
    KEY idx_order_payment_paid_time (paid_time),
    CONSTRAINT chk_order_payment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_timeline (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    operator_type VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    remark VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_timeline_order_id (order_id),
    KEY idx_order_timeline_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
