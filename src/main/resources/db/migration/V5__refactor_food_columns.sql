-- V5: food 테이블 컬럼 정비 (부분 적용 상태에서도 재실행 가능하도록 방어적으로 처리)

-- 1. 불필요 컬럼 제거
ALTER TABLE food
    DROP COLUMN IF EXISTS serving_size,
    DROP COLUMN IF EXISTS serving_unit,
    DROP COLUMN IF EXISTS portion_label,
    DROP COLUMN IF EXISTS portion_amount;

-- 2. V2에서 추가된 컬럼 제거
ALTER TABLE food
    DROP COLUMN IF EXISTS external_food_code,
    DROP COLUMN IF EXISTS source_dataset,
    DROP COLUMN IF EXISTS source_serving_base_raw;

-- 3. 기존 컬럼 정리
-- basis_amount -> nutrient_basis
SET @has_basis_amount := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'basis_amount'
);
SET @has_nutrient_basis := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'nutrient_basis'
);
SET @sql := IF(
    @has_basis_amount = 1 AND @has_nutrient_basis = 0,
    'ALTER TABLE food CHANGE COLUMN basis_amount nutrient_basis VARCHAR(20) NULL COMMENT ''영양성분함량기준량 (100g/100mL)''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ref_serving_size -> ref_intake_amount
SET @has_ref_serving_size := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'ref_serving_size'
);
SET @has_ref_intake_amount := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'food' AND column_name = 'ref_intake_amount'
);
SET @sql := IF(
    @has_ref_serving_size = 1 AND @has_ref_intake_amount = 0,
    'ALTER TABLE food CHANGE COLUMN ref_serving_size ref_intake_amount TEXT NULL COMMENT ''1인(회)분량 참고량 / 1회 섭취참고량''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 신규 컬럼 추가
ALTER TABLE food
    ADD COLUMN IF NOT EXISTS food_origin    VARCHAR(30)  NULL COMMENT '식품기원명 (가정식/외식/가공식품 등)' AFTER data_type,
    ADD COLUMN IF NOT EXISTS category_small VARCHAR(50)  NULL COMMENT '식품소분류명' AFTER category_medium,
    ADD COLUMN IF NOT EXISTS trans_fat      DOUBLE       NULL COMMENT '트랜스지방산(g)' AFTER saturated_fat,
    ADD COLUMN IF NOT EXISTS importer       VARCHAR(255) NULL COMMENT '수입업체명' AFTER manufacturer,
    ADD COLUMN IF NOT EXISTS distributor    VARCHAR(255) NULL COMMENT '유통업체명' AFTER importer;

-- 5. food_code 타입 정리
ALTER TABLE food
    MODIFY COLUMN food_code VARCHAR(64) NULL;

-- 6. meal_nutrition 데이터 food로 이관 (meal_nutrition이 남아있는 경우만)
SET @has_meal_nutrition := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'meal_nutrition'
);
SET @sql := IF(
    @has_meal_nutrition = 1,
    'UPDATE food f
      INNER JOIN meal_nutrition mn ON f.id = mn.food_id
     SET f.cholesterol = COALESCE(f.cholesterol, mn.cholesterol),
         f.saturated_fat = COALESCE(f.saturated_fat, mn.saturated_fat)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TABLE IF EXISTS meal_nutrition;
DROP TABLE IF EXISTS food_import_staging;
