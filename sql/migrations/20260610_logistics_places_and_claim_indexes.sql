DELIMITER //

CREATE PROCEDURE migrate_logistics_places_and_claim_indexes()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'purchaser_profile'
          AND column_name = 'longitude'
    ) THEN
        ALTER TABLE purchaser_profile
            ADD COLUMN longitude DECIMAL(10,6) DEFAULT NULL AFTER address;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'purchaser_profile'
          AND column_name = 'latitude'
    ) THEN
        ALTER TABLE purchaser_profile
            ADD COLUMN latitude DECIMAL(10,6) DEFAULT NULL AFTER longitude;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'purchase_order'
          AND column_name = 'origin_address'
    ) THEN
        ALTER TABLE purchase_order
            ADD COLUMN origin_address VARCHAR(255) DEFAULT NULL AFTER pushed_to;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'purchase_order'
          AND column_name = 'origin_longitude'
    ) THEN
        ALTER TABLE purchase_order
            ADD COLUMN origin_longitude DECIMAL(10,6) DEFAULT NULL AFTER origin_address;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'purchase_order'
          AND column_name = 'origin_latitude'
    ) THEN
        ALTER TABLE purchase_order
            ADD COLUMN origin_latitude DECIMAL(10,6) DEFAULT NULL AFTER origin_longitude;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'purchase_order'
          AND column_name = 'destination_address'
    ) THEN
        ALTER TABLE purchase_order
            ADD COLUMN destination_address VARCHAR(255) DEFAULT NULL AFTER origin_latitude;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'purchase_order'
          AND column_name = 'destination_longitude'
    ) THEN
        ALTER TABLE purchase_order
            ADD COLUMN destination_longitude DECIMAL(10,6) DEFAULT NULL AFTER destination_address;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'purchase_order'
          AND column_name = 'destination_latitude'
    ) THEN
        ALTER TABLE purchase_order
            ADD COLUMN destination_latitude DECIMAL(10,6) DEFAULT NULL AFTER destination_longitude;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'purchase_order'
          AND index_name = 'idx_purchase_order_status_driver'
    ) THEN
        CREATE INDEX idx_purchase_order_status_driver ON purchase_order (status, driver_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'purchase_order'
          AND index_name = 'idx_purchase_order_destination'
    ) THEN
        CREATE INDEX idx_purchase_order_destination ON purchase_order (destination_longitude, destination_latitude);
    END IF;
END//

CALL migrate_logistics_places_and_claim_indexes()//
DROP PROCEDURE migrate_logistics_places_and_claim_indexes//

DELIMITER ;

UPDATE purchaser_profile
SET longitude = CASE
        WHEN address LIKE '%南京%' OR company_name LIKE '%Jiangsu%' THEN 118.820000
        ELSE 121.470000
    END,
    latitude = CASE
        WHEN address LIKE '%南京%' OR company_name LIKE '%Jiangsu%' THEN 31.950000
        ELSE 31.230000
    END
WHERE longitude IS NULL
   OR latitude IS NULL;

UPDATE supplier_profile
SET longitude = CASE
        WHEN address LIKE '%南京%' OR company_name LIKE '%Jiangsu%' THEN 118.840000
        ELSE 121.544000
    END,
    latitude = CASE
        WHEN address LIKE '%南京%' OR company_name LIKE '%Jiangsu%' THEN 31.950000
        ELSE 31.221000
    END
WHERE longitude IS NULL
   OR latitude IS NULL;

UPDATE purchase_order po
JOIN supplier_profile sp ON sp.supplier_id = po.supplier_id
JOIN purchaser_profile pp ON pp.purchaser_id = po.purchaser_id
SET po.origin_address = sp.address,
    po.origin_longitude = sp.longitude,
    po.origin_latitude = sp.latitude,
    po.destination_address = pp.address,
    po.destination_longitude = pp.longitude,
    po.destination_latitude = pp.latitude
WHERE po.origin_address IS NULL
   OR po.origin_longitude IS NULL
   OR po.origin_latitude IS NULL
   OR po.destination_address IS NULL
   OR po.destination_longitude IS NULL
   OR po.destination_latitude IS NULL;
