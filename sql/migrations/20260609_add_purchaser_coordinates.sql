ALTER TABLE purchaser_profile
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(10,6) DEFAULT NULL AFTER address,
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,6) DEFAULT NULL AFTER longitude;

UPDATE purchaser_profile
SET longitude = 121.470000,
    latitude = 31.230000
WHERE company_name = 'Shanghai Material Purchaser Co., Ltd.';

UPDATE purchaser_profile
SET longitude = 118.820000,
    latitude = 31.950000
WHERE company_name = 'Jiangsu Emergency Construction Group';

UPDATE purchaser_profile
SET longitude = COALESCE(longitude, 121.470000),
    latitude = COALESCE(latitude, 31.230000)
WHERE longitude IS NULL
   OR latitude IS NULL;
