ALTER TABLE food
    ADD COLUMN external_food_code VARCHAR(64) NULL,
    ADD COLUMN source_dataset VARCHAR(40) NULL,
    ADD COLUMN source_serving_base_raw VARCHAR(120) NULL;

CREATE UNIQUE INDEX uk_food_external_food_code ON food (external_food_code);

CREATE TABLE food_import_staging (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(40) NOT NULL,
    external_food_code VARCHAR(64) NULL,
    name VARCHAR(255) NULL,
    representative_name VARCHAR(255) NULL,
    serving_base_raw VARCHAR(120) NULL,
    serving_base_alt VARCHAR(120) NULL,
    portion_label VARCHAR(120) NULL,
    portion_amount VARCHAR(120) NULL,
    manufacturer VARCHAR(255) NULL,
    importer VARCHAR(255) NULL,
    distributor VARCHAR(255) NULL,
    manufacturer_name VARCHAR(255) NULL,
    calories_raw VARCHAR(64) NULL,
    carbs_raw VARCHAR(64) NULL,
    protein_raw VARCHAR(64) NULL,
    fat_raw VARCHAR(64) NULL,
    sugar_raw VARCHAR(64) NULL,
    sodium_raw VARCHAR(64) NULL,
    saturated_fat_raw VARCHAR(64) NULL,
    cholesterol_raw VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_food_import_staging_code (external_food_code),
    KEY idx_food_import_staging_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE meal_nutrition
    MODIFY COLUMN carbs DOUBLE NULL,
    MODIFY COLUMN fat DOUBLE NULL;
