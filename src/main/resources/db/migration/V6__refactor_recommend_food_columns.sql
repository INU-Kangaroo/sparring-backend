-- V6: recommend_food 테이블 컬럼 정비 (food 테이블과 동일 구조로 통일)

ALTER TABLE recommend_food
    ADD COLUMN food_origin      VARCHAR(30)  NULL COMMENT '식품기원명' AFTER data_type,
    ADD COLUMN category_small   VARCHAR(50)  NULL COMMENT '식품소분류명' AFTER category_medium,
    ADD COLUMN trans_fat        DOUBLE       NULL COMMENT '트랜스지방산(g)' AFTER saturated_fat,
    ADD COLUMN importer         VARCHAR(255) NULL COMMENT '수입업체명' AFTER manufacturer,
    ADD COLUMN distributor      VARCHAR(255) NULL COMMENT '유통업체명' AFTER importer;

ALTER TABLE recommend_food
    CHANGE COLUMN basis_amount     nutrient_basis     VARCHAR(20)  NULL COMMENT '영양성분함량기준량 (100g/100mL)',
    CHANGE COLUMN ref_serving_size ref_intake_amount  TEXT         NULL COMMENT '1인(회)분량 참고량 / 1회 섭취참고량';

ALTER TABLE recommend_food
    DROP COLUMN water;
