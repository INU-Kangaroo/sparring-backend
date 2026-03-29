-- V5: food 테이블 컬럼 정비
-- serving_size, serving_unit, portion_label 제거
-- trans_fat, food_origin, category_small, ref_intake_amount, nutrient_basis, importer, distributor 추가
-- portion_amount, basis_amount, ref_serving_size, food_weight → food_weight 유지, 나머지 제거

-- 1. 불필요 컬럼 제거
ALTER TABLE food
    DROP COLUMN serving_size,
    DROP COLUMN serving_unit,
    DROP COLUMN portion_label,
    DROP COLUMN portion_amount;

-- 2. V2에서 추가된 컬럼 제거 (없으면 무시)
ALTER TABLE food
    DROP COLUMN IF EXISTS external_food_code,
    DROP COLUMN IF EXISTS source_dataset,
    DROP COLUMN IF EXISTS source_serving_base_raw;

-- 3. 기존 컬럼 정리 (basis_amount → nutrient_basis rename, ref_serving_size → ref_intake_amount rename)
ALTER TABLE food
    CHANGE COLUMN basis_amount    nutrient_basis     VARCHAR(20)  NULL COMMENT '영양성분함량기준량 (100g/100mL)',
    CHANGE COLUMN ref_serving_size ref_intake_amount TEXT         NULL COMMENT '1인(회)분량 참고량 / 1회 섭취참고량';

-- 4. 신규 컬럼 추가
ALTER TABLE food
    ADD COLUMN food_origin      VARCHAR(30)  NULL COMMENT '식품기원명 (가정식/외식/가공식품 등)' AFTER data_type,
    ADD COLUMN category_small   VARCHAR(50)  NULL COMMENT '식품소분류명' AFTER category_medium,
    ADD COLUMN trans_fat        DOUBLE       NULL COMMENT '트랜스지방산(g)' AFTER saturated_fat,
    ADD COLUMN importer         VARCHAR(255) NULL COMMENT '수입업체명' AFTER manufacturer,
    ADD COLUMN distributor      VARCHAR(255) NULL COMMENT '유통업체명' AFTER importer;

-- 5. food_code 인덱스 (없으면 추가)
ALTER TABLE food
    MODIFY COLUMN food_code VARCHAR(64) NULL;

-- 6. meal_nutrition 데이터 food로 이관 후 제거
UPDATE food f
    INNER JOIN meal_nutrition mn ON f.id = mn.food_id
SET
    f.cholesterol   = COALESCE(f.cholesterol,   mn.cholesterol),
    f.saturated_fat = COALESCE(f.saturated_fat, mn.saturated_fat);

DROP TABLE IF EXISTS meal_nutrition;
DROP TABLE IF EXISTS food_import_staging;
