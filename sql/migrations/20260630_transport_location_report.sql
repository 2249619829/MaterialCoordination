CREATE TABLE IF NOT EXISTS transport_location_report (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(40) NOT NULL,
    driver_id BIGINT NOT NULL,
    longitude DECIMAL(10,6) NOT NULL,
    latitude DECIMAL(10,6) NOT NULL,
    remark VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_transport_location_order_time (order_id, create_time),
    KEY idx_transport_location_driver_time (driver_id, create_time),
    KEY idx_transport_location_point (longitude, latitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
