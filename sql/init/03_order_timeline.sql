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
