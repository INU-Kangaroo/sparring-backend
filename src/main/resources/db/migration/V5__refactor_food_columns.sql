-- V5: food 테이블 컬럼 정비 (MySQL 5.7/8 호환 + 재실행 안전)

-- 1) 불필요 컬럼 제거
SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'serving_size'),
        'ALTER TABLE food DROP COLUMN serving_size',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'serving_unit'),
        'ALTER TABLE food DROP COLUMN serving_unit',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'portion_label'),
        'ALTER TABLE food DROP COLUMN portion_label',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'portion_amount'),
        'ALTER TABLE food DROP COLUMN portion_amount',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) V2 관련 임시 컬럼 제거
SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'external_food_code'),
        'ALTER TABLE food DROP COLUMN external_food_code',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'source_dataset'),
        'ALTER TABLE food DROP COLUMN source_dataset',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'source_serving_base_raw'),
        'ALTER TABLE food DROP COLUMN source_serving_base_raw',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) rename 대신 신규 컬럼만 보강 (부분 적용 DB에서도 안전)
SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'nutrient_basis'),
        'SELECT 1',
        'ALTER TABLE food ADD COLUMN nutrient_basis VARCHAR(20) NULL COMMENT ''영양성분함량기준량 (100g/100mL)'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'ref_intake_amount'),
        'SELECT 1',
        'ALTER TABLE food ADD COLUMN ref_intake_amount TEXT NULL COMMENT ''1인(회)분량 참고량 / 1회 섭취참고량'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) 신규 컬럼 추가
SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'food_origin'),
        'SELECT 1',
        'ALTER TABLE food ADD COLUMN food_origin VARCHAR(30) NULL COMMENT ''식품기원명 (가정식/외식/가공식품 등)'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'category_small'),
        'SELECT 1',
        'ALTER TABLE food ADD COLUMN category_small VARCHAR(50) NULL COMMENT ''식품소분류명'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'trans_fat'),
        'SELECT 1',
        'ALTER TABLE food ADD COLUMN trans_fat DOUBLE NULL COMMENT ''트랜스지방산(g)'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'importer'),
        'SELECT 1',
        'ALTER TABLE food ADD COLUMN importer VARCHAR(255) NULL COMMENT ''수입업체명'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'distributor'),
        'SELECT 1',
        'ALTER TABLE food ADD COLUMN distributor VARCHAR(255) NULL COMMENT ''유통업체명'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) 잔존 테이블 정리
DROP TABLE IF EXISTS meal_nutrition;
DROP TABLE IF EXISTS food_import_staging;
