-- V6: recommend_food 테이블 컬럼 정비 (MySQL 5.7/8 호환 + 재실행 안전)

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'recommend_food' AND column_name = 'food_origin'),
        'SELECT 1',
        'ALTER TABLE recommend_food ADD COLUMN food_origin VARCHAR(30) NULL COMMENT ''식품기원명'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'recommend_food' AND column_name = 'category_small'),
        'SELECT 1',
        'ALTER TABLE recommend_food ADD COLUMN category_small VARCHAR(50) NULL COMMENT ''식품소분류명'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'recommend_food' AND column_name = 'trans_fat'),
        'SELECT 1',
        'ALTER TABLE recommend_food ADD COLUMN trans_fat DOUBLE NULL COMMENT ''트랜스지방산(g)'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'recommend_food' AND column_name = 'importer'),
        'SELECT 1',
        'ALTER TABLE recommend_food ADD COLUMN importer VARCHAR(255) NULL COMMENT ''수입업체명'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'recommend_food' AND column_name = 'distributor'),
        'SELECT 1',
        'ALTER TABLE recommend_food ADD COLUMN distributor VARCHAR(255) NULL COMMENT ''유통업체명'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'recommend_food' AND column_name = 'nutrient_basis'),
        'SELECT 1',
        'ALTER TABLE recommend_food ADD COLUMN nutrient_basis VARCHAR(20) NULL COMMENT ''영양성분함량기준량 (100g/100mL)'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'recommend_food' AND column_name = 'ref_intake_amount'),
        'SELECT 1',
        'ALTER TABLE recommend_food ADD COLUMN ref_intake_amount TEXT NULL COMMENT ''1인(회)분량 참고량 / 1회 섭취참고량'''
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
