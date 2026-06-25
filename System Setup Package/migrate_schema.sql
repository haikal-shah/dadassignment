USE masakgramprompt;

-- ============================================================
-- Priority 1: Schema Fixes
-- Add missing columns required by metrics_evaluation_queries.sql
-- ============================================================

-- Add video_id to transcript (referenced in all metric queries)
ALTER TABLE transcript
    ADD COLUMN video_id VARCHAR(50) NULL
        COMMENT 'Instagram Reel ID, mirrors reel.reel_id_instagram for quick lookup'
        AFTER transcript_id;

-- Add language_tag and video_duration to reel
ALTER TABLE reel
    ADD COLUMN video_duration_sec INT NULL
        COMMENT 'Duration of the video in seconds'
        AFTER identified_date,
    ADD COLUMN language_tag VARCHAR(20) NULL DEFAULT 'code-switched'
        COMMENT 'Language of the reel: EN, MY, or code-switched'
        AFTER video_duration_sec;

-- Add is_hallucinated to ingredient_result (needed for Layers 3B, 3C, 5)
ALTER TABLE ingredient_result
    ADD COLUMN is_hallucinated BOOLEAN NULL DEFAULT FALSE
        COMMENT 'True if this ingredient was NOT mentioned in the transcript (hallucination)'
        AFTER potassium_mg;

-- Add unit_original and unit_en to ground_truth_ingredient if not already there
-- (they are already in schema but named differently; aliased in queries)
-- Add energy_kcal alias columns to ground_truth_ingredient for Layer 2B compatibility
ALTER TABLE ground_truth_ingredient
    ADD COLUMN energy_kcal FLOAT NULL
        COMMENT 'Alias for calories column, used in metric queries'
        AFTER calories,
    ADD COLUMN fat_g FLOAT NULL
        COMMENT 'Alias for total_fat_g, used in metric queries'
        AFTER total_fat_g,
    ADD COLUMN carbohydrate_g FLOAT NULL
        COMMENT 'Alias for total_carbohydrate_g, used in metric queries'
        AFTER total_carbohydrate_g,
    ADD COLUMN unit_original VARCHAR(100) NULL
        COMMENT 'Unit as spoken in transcript e.g. sudu besar'
        AFTER quantity_unit_culinary,
    ADD COLUMN unit_en VARCHAR(100) NULL
        COMMENT 'English translation of the unit'
        AFTER unit_original;

-- Add energy_kcal alias to ingredient_result for Layer 2B compatibility  
ALTER TABLE ingredient_result
    ADD COLUMN energy_kcal FLOAT NULL
        COMMENT 'Alias for calories, used in metric queries'
        AFTER calories,
    ADD COLUMN fat_g FLOAT NULL
        COMMENT 'Alias for total_fat_g, used in metric queries'
        AFTER total_fat_g,
    ADD COLUMN carbohydrate_g FLOAT NULL
        COMMENT 'Alias for total_carbohydrate_g, used in metric queries'
        AFTER total_carbohydrate_g;

-- Add total_energy_kcal and total_carbohydrate_g alias to nutrition_result
ALTER TABLE nutrition_result
    ADD COLUMN total_energy_kcal FLOAT NULL
        COMMENT 'Alias for total_calories, used in metric queries'
        AFTER total_calories,
    ADD COLUMN total_protein_g FLOAT NULL
        COMMENT 'Copy of protein total for metric query compatibility'
        AFTER total_energy_kcal;

SELECT 'Schema migration complete.' AS status;
