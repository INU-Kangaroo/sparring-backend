-- =====================================================
-- V3: 식단 추천 시스템
-- - food 테이블 재설계 (meal_nutrition 흡수, CSV 컬럼 보존)
-- - recommend_food 신규 (추천용)
-- - user_food_feedback 신규 (EMA 피드백)
-- - cluster_food_weight 신규 (KNHANES 콜드스타트)
-- - food_recommendation 신규 (추천 로그)
-- =====================================================


-- -----------------------------------------------------
-- 1. food 테이블 컬럼 추가
--    meal_nutrition 영양소 흡수 + CSV 원본 컬럼 보존
-- -----------------------------------------------------

ALTER TABLE food
    -- 식품 분류 (CSV 원본)
    ADD COLUMN food_code        VARCHAR(64)     NULL        COMMENT '식품코드 (CSV 원본)',
    ADD COLUMN data_type        VARCHAR(20)     NULL        COMMENT '데이터구분명 (음식/가공식품/원재료)',
    ADD COLUMN category_large   VARCHAR(50)     NULL        COMMENT '식품대분류명',
    ADD COLUMN category_medium  VARCHAR(50)     NULL        COMMENT '식품중분류명',
    ADD COLUMN rep_food_name    VARCHAR(100)    NULL        COMMENT '대표식품명',
    ADD COLUMN basis_amount     VARCHAR(20)     NULL        COMMENT '영양성분함량기준량 (100g/100ml)',
    ADD COLUMN ref_serving_size VARCHAR(50)     NULL        COMMENT '1인(회)분량 참고량',

    -- 영양소 (100g/100ml 기준)
    ADD COLUMN calories         DOUBLE          NULL        COMMENT '에너지(kcal)',
    ADD COLUMN water            DOUBLE          NULL        COMMENT '수분(g)',
    ADD COLUMN protein          DOUBLE          NULL        COMMENT '단백질(g)',
    ADD COLUMN fat              DOUBLE          NULL        COMMENT '지방(g)',
    ADD COLUMN carbs            DOUBLE          NULL        COMMENT '탄수화물(g)',
    ADD COLUMN sugar            DOUBLE          NULL        COMMENT '당류(g)',
    ADD COLUMN fiber            DOUBLE          NULL        COMMENT '식이섬유(g)',
    ADD COLUMN calcium          DOUBLE          NULL        COMMENT '칼슘(mg)',
    ADD COLUMN iron             DOUBLE          NULL        COMMENT '철(mg)',
    ADD COLUMN potassium        DOUBLE          NULL        COMMENT '칼륨(mg)',
    ADD COLUMN sodium           DOUBLE          NULL        COMMENT '나트륨(mg)',
    ADD COLUMN cholesterol      DOUBLE          NULL        COMMENT '콜레스테롤(mg)',
    ADD COLUMN saturated_fat    DOUBLE          NULL        COMMENT '포화지방산(g)',

    ADD UNIQUE INDEX uq_food_food_code (food_code),
    ADD INDEX idx_food_category_large (category_large);


-- -----------------------------------------------------
-- 2. meal_nutrition → food 데이터 이관
-- -----------------------------------------------------

UPDATE food f
    INNER JOIN meal_nutrition mn ON f.id = mn.food_id
SET
    f.calories      = mn.calories,
    f.carbs         = mn.carbs,
    f.protein       = mn.protein,
    f.fat           = mn.fat,
    f.sodium        = mn.sodium,
    f.sugar         = mn.sugar,
    f.cholesterol   = mn.cholesterol,
    f.saturated_fat = mn.saturated_fat;


-- -----------------------------------------------------
-- 3. meal_nutrition, food_import_staging 테이블 제거
-- -----------------------------------------------------

DROP TABLE IF EXISTS meal_nutrition;
DROP TABLE IF EXISTS food_import_staging;


-- -----------------------------------------------------
-- 4. food 테이블 불필요 컬럼 제거 (V2에서 추가된 것들)
-- -----------------------------------------------------

ALTER TABLE food
    DROP INDEX uk_food_external_food_code,
    DROP COLUMN external_food_code,
    DROP COLUMN source_dataset,
    DROP COLUMN source_serving_base_raw;


-- -----------------------------------------------------
-- 5. recommend_food 테이블 신규 생성 (추천용)
--    food와 동일 구조. 빵/음료 제외 ~5,000건 적재 예정
-- -----------------------------------------------------

CREATE TABLE recommend_food (
    id              BIGINT          NOT NULL AUTO_INCREMENT,

    -- 식품 식별/분류
    food_code       VARCHAR(64)     NOT NULL    COMMENT '식품코드 (CSV 원본)',
    data_type       VARCHAR(20)     NULL        COMMENT '데이터구분명',
    category_large  VARCHAR(50)     NULL        COMMENT '식품대분류명',
    category_medium VARCHAR(50)     NULL        COMMENT '식품중분류명',
    rep_food_name   VARCHAR(100)    NULL        COMMENT '대표식품명',
    name            VARCHAR(255)    NOT NULL    COMMENT '식품명',
    basis_amount    VARCHAR(20)     NULL        COMMENT '영양성분함량기준량',
    ref_serving_size VARCHAR(50)    NULL        COMMENT '1인(회)분량 참고량',
    food_weight     VARCHAR(50)     NULL        COMMENT '식품중량 (예: 240g)',
    manufacturer    VARCHAR(255)    NULL        COMMENT '업체명',

    -- 영양소 (100g/100ml 기준)
    calories        DOUBLE          NULL        COMMENT '에너지(kcal)',
    water           DOUBLE          NULL        COMMENT '수분(g)',
    protein         DOUBLE          NULL        COMMENT '단백질(g)',
    fat             DOUBLE          NULL        COMMENT '지방(g)',
    carbs           DOUBLE          NULL        COMMENT '탄수화물(g)',
    sugar           DOUBLE          NULL        COMMENT '당류(g)',
    fiber           DOUBLE          NULL        COMMENT '식이섬유(g)',
    calcium         DOUBLE          NULL        COMMENT '칼슘(mg)',
    iron            DOUBLE          NULL        COMMENT '철(mg)',
    potassium       DOUBLE          NULL        COMMENT '칼륨(mg)',
    sodium          DOUBLE          NULL        COMMENT '나트륨(mg)',
    cholesterol     DOUBLE          NULL        COMMENT '콜레스테롤(mg)',
    saturated_fat   DOUBLE          NULL        COMMENT '포화지방산(g)',

    -- 메타
    is_deleted      BIT(1)          NOT NULL    DEFAULT 0,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_recommend_food_code (food_code),
    INDEX idx_recommend_food_category (category_large),
    INDEX idx_recommend_food_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='추천용 음식 데이터 (전체 CSV 적재, meal_time별 필터링은 AI 서버에서 처리)';


-- -----------------------------------------------------
-- 6. user_food_feedback 테이블 신규 생성
--    식전/식후 혈당 기반 EMA 피드백 누적
-- -----------------------------------------------------

CREATE TABLE user_food_feedback (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL,
    food_code           VARCHAR(64)     NOT NULL    COMMENT 'recommend_food.food_code',

    -- 혈당 측정
    meal_logged_at      DATETIME(6)     NOT NULL    COMMENT '식사 시각',
    blood_sugar_before  INT             NULL        COMMENT '식전 혈당 (mg/dL)',
    blood_sugar_after   INT             NULL        COMMENT '식후 혈당 (mg/dL, 1~2시간 후)',
    glucose_delta       INT             NULL        COMMENT '식후 - 식전',
    measurement_gap_min INT             NULL        COMMENT '측정 간격 (분)',

    -- EMA 가중치
    feedback_weight     DOUBLE          NOT NULL    DEFAULT 0.0 COMMENT '누적 EMA 가중치 (-1.0 ~ +1.0)',
    sample_count        INT             NOT NULL    DEFAULT 0   COMMENT '누적 샘플 수',

    -- 메타
    is_deleted          BIT(1)          NOT NULL    DEFAULT 0,
    created_at          DATETIME(6)     NOT NULL,
    updated_at          DATETIME(6)     NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_feedback_user_food (user_id, food_code),
    INDEX idx_feedback_user_logged (user_id, meal_logged_at),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자별 음식 혈당 반응 피드백 (EMA)';


-- -----------------------------------------------------
-- 7. cluster_food_weight 테이블 신규 생성
--    KNHANES K-Means 클러스터링 결과
--    신규 사용자 콜드 스타트용 초기 가중치
-- -----------------------------------------------------

CREATE TABLE cluster_food_weight (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    cluster_id      INT             NOT NULL    COMMENT 'K-Means 클러스터 ID',
    food_code       VARCHAR(64)     NOT NULL    COMMENT 'recommend_food.food_code',
    frequency_score DOUBLE          NOT NULL    COMMENT '해당 클러스터에서 음식 빈도 기반 점수',

    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_cluster_food (cluster_id, food_code),
    INDEX idx_cluster_food_weight_cluster (cluster_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='KNHANES 클러스터별 음식 초기 가중치 (콜드 스타트)';


-- -----------------------------------------------------
-- 8. food_recommendation 테이블 신규 생성
--    추천 시 적용된 제약/기여도/이유 코드 저장
-- -----------------------------------------------------

CREATE TABLE food_recommendation (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT          NOT NULL,
    recommended_at          DATETIME(6)     NOT NULL,
    meal_time               VARCHAR(10)     NOT NULL    COMMENT 'BREAKFAST/LUNCH/DINNER/SNACK',

    -- Fallback
    fallback_level          INT             NOT NULL    DEFAULT 0 COMMENT '0=정상, 1=당류완화, 2=나트륨완화, 3=중복허용',

    -- 추천 메타 (JSON)
    applied_constraints     JSON            NULL        COMMENT '적용된 제약 목록 ex) ["sugar_limit","sodium_limit"]',
    feature_contrib         JSON            NULL        COMMENT '영양소별 점수 기여도 ex) {"carbs":0.42,"fiber":0.28}',
    reason_codes            JSON            NULL        COMMENT '추천 이유 코드 ex) ["LOW_CARB","HIGH_FIBER"]',
    food_codes              JSON            NULL        COMMENT '추천된 food_code 목록',

    created_at              DATETIME(6)     NOT NULL,
    updated_at              DATETIME(6)     NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_food_rec_user_time (user_id, recommended_at),
    CONSTRAINT fk_food_rec_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='식단 추천 로그 (디버깅/분석용)';
