-- =============================================================
-- MasakGramPrompt — Create Tables
-- Project : BITP 3123 Distributed Application Development
-- System  : Nutritional LLM Analysis System
-- Tables  : 11 tables across 3 functional groups
-- Version : 1.0
-- Date    : 2026-06-10
-- Author  : Emaliana Kasmuri
--           Fakulti Teknologi Maklumat dan Komunikasi
--           Universiti Teknikal Malaysia Melaka
-- =============================================================

-- Recommended: create and select a dedicated database first
CREATE DATABASE IF NOT EXISTS masakgramprompt CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE masakgramprompt;

-- Drop tables in reverse dependency order to avoid FK constraint errors
DROP TABLE IF EXISTS ingredient_result;
DROP TABLE IF EXISTS nutrition_result;
DROP TABLE IF EXISTS experiment;
DROP TABLE IF EXISTS prompt_technique;
DROP TABLE IF EXISTS llm_model;
DROP TABLE IF EXISTS ground_truth_ingredient;
DROP TABLE IF EXISTS ground_truth_reel;
DROP TABLE IF EXISTS transcript;
DROP TABLE IF EXISTS audio_file;
DROP TABLE IF EXISTS reel;
DROP TABLE IF EXISTS influencer;


-- =============================================================
-- GROUP 1: DATA COLLECTION
-- =============================================================

-- -------------------------------------------------------------
-- TABLE: influencer
-- Stores the profile of a single Malaysian gastronomy
-- influencer selected by the group. Contains exactly one record.
-- -------------------------------------------------------------
CREATE TABLE influencer (
    influencer_id   INT             NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL COMMENT 'Full name of the influencer',
    instagram_account VARCHAR(100)  NOT NULL COMMENT 'Instagram handle e.g. khairulaming',
    instagram_url   VARCHAR(500)    NOT NULL COMMENT 'Full URL to the Instagram profile',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',

    CONSTRAINT pk_influencer PRIMARY KEY (influencer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Profile of the selected Malaysian gastronomy influencer';


-- -------------------------------------------------------------
-- TABLE: reel
-- Stores each Instagram Reel identified during data collection.
-- Each group member must identify exactly 10 reels, resulting
-- in 40–50 records depending on group size.
-- -------------------------------------------------------------
CREATE TABLE reel (
    reel_id                 INT             NOT NULL AUTO_INCREMENT,
    influencer_id           INT             NOT NULL COMMENT 'FK → influencer.influencer_id',
    reel_id_instagram       VARCHAR(50)     NOT NULL COMMENT 'Instagram reel ID extracted from URL e.g. DV7uZzBE47j',
    reel_url                VARCHAR(500)    NOT NULL COMMENT 'Full Instagram Reel URL',
    identified_by_matric    VARCHAR(20)     NOT NULL COMMENT 'Matric number of team member who identified the reel',
    identified_by_name      VARCHAR(100)    NOT NULL COMMENT 'Name of team member who identified the reel',
    identified_date         DATE            NOT NULL COMMENT 'Date the reel was identified',
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',

    CONSTRAINT pk_reel          PRIMARY KEY (reel_id),
    CONSTRAINT fk_reel_influencer FOREIGN KEY (influencer_id)
        REFERENCES influencer (influencer_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Instagram Reels identified during data collection';


-- -------------------------------------------------------------
-- TABLE: audio_file
-- Stores file metadata and human verification results for each
-- audio file extracted from a reel. One record per reel.
-- -------------------------------------------------------------
CREATE TABLE audio_file (
    audio_id                INT             NOT NULL AUTO_INCREMENT,
    reel_id                 INT             NOT NULL COMMENT 'FK → reel.reel_id',
    file_name               VARCHAR(200)    NOT NULL COMMENT 'Audio file name e.g. DV7uZzBE47j.mp3',
    file_path               VARCHAR(500)    NOT NULL COMMENT 'Full path to the audio file on disk',
    file_created_at         TIMESTAMP       NULL     COMMENT 'File creation timestamp from filesystem metadata',
    file_size_bytes         BIGINT          NULL     COMMENT 'Audio file size in bytes',
    file_format             VARCHAR(10)     NOT NULL COMMENT 'Audio format e.g. mp3',
    reel_audio_consistent   BOOLEAN         NULL     COMMENT 'True if audio matches the Instagram Reel content',
    verified_by_matric      VARCHAR(20)     NULL     COMMENT 'Matric number of the audio verifier',
    verified_by_name        VARCHAR(100)    NULL     COMMENT 'Name of the audio verifier',
    verified_at             TIMESTAMP       NULL     COMMENT 'Timestamp when audio verification was completed',

    CONSTRAINT pk_audio_file    PRIMARY KEY (audio_id),
    CONSTRAINT fk_audio_reel    FOREIGN KEY (reel_id)
        REFERENCES reel (reel_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Audio files extracted from each reel with verification records';


-- -------------------------------------------------------------
-- TABLE: transcript
-- Stores verified transcript text produced by Faster-Whisper,
-- together with execution metadata and verification results.
-- One record per reel and per audio file.
-- -------------------------------------------------------------
CREATE TABLE transcript (
    transcript_id               INT             NOT NULL AUTO_INCREMENT,
    reel_id                     INT             NOT NULL COMMENT 'FK → reel.reel_id',
    audio_id                    INT             NOT NULL COMMENT 'FK → audio_file.audio_id',
    file_name                   VARCHAR(200)    NOT NULL COMMENT 'Transcript file name e.g. transcription_20260406_081309.txt',
    file_path                   VARCHAR(500)    NOT NULL COMMENT 'Full path to the transcript file on disk',
    file_created_at             TIMESTAMP       NULL     COMMENT 'File creation timestamp from filesystem metadata',
    file_size_bytes             BIGINT          NULL     COMMENT 'Transcript file size in bytes',
    file_format                 VARCHAR(10)     NOT NULL COMMENT 'File format e.g. txt',
    audio_transcript_consistent BOOLEAN         NULL     COMMENT 'True if transcript matches the audio content',
    verified_by_matric          VARCHAR(20)     NULL     COMMENT 'Matric number of the transcript verifier',
    verified_by_name            VARCHAR(100)    NULL     COMMENT 'Name of the transcript verifier',
    verified_at                 TIMESTAMP       NULL     COMMENT 'Timestamp when transcript verification was completed',

    CONSTRAINT pk_transcript        PRIMARY KEY (transcript_id),
    CONSTRAINT fk_transcript_reel   FOREIGN KEY (reel_id)
        REFERENCES reel (reel_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_transcript_audio  FOREIGN KEY (audio_id)
        REFERENCES audio_file (audio_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Faster-Whisper transcripts with verification records';


-- =============================================================
-- GROUP 2: GROUND TRUTH
-- =============================================================

-- -------------------------------------------------------------
-- TABLE: ground_truth_reel
-- Stores reel-level ground truth annotation produced by human
-- annotators. One record per transcript.
-- -------------------------------------------------------------
CREATE TABLE ground_truth_reel (
    gt_reel_id      INT             NOT NULL AUTO_INCREMENT,
    transcript_id   INT             NOT NULL COMMENT 'FK → transcript.transcript_id',
    annotator_matric VARCHAR(20)    NOT NULL COMMENT 'Matric number of the annotator',
    annotator_name  VARCHAR(100)    NOT NULL COMMENT 'Name of the annotator',
    annotated_at    TIMESTAMP       NULL     COMMENT 'Timestamp when annotation was completed',

    CONSTRAINT pk_gt_reel           PRIMARY KEY (gt_reel_id),
    CONSTRAINT fk_gt_reel_transcript FOREIGN KEY (transcript_id)
        REFERENCES transcript (transcript_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reel-level ground truth annotations by human annotators';


-- -------------------------------------------------------------
-- TABLE: ground_truth_ingredient
-- Stores ingredient-level ground truth annotations. One reel
-- may contain multiple ingredients, so this table has more
-- records than ground_truth_reel.
-- -------------------------------------------------------------
CREATE TABLE ground_truth_ingredient (
    gt_ingredient_id        INT             NOT NULL AUTO_INCREMENT,
    gt_reel_id              INT             NOT NULL COMMENT 'FK → ground_truth_reel.gt_reel_id',
    name_original           VARCHAR(200)    NOT NULL COMMENT 'Ingredient name exactly as spoken in the transcript (Identified Name)',
    language_mentioned      VARCHAR(5)      NOT NULL COMMENT 'Language of the ingredient name: MY, EN, or OT',
    name_en                 VARCHAR(200)    NULL     COMMENT 'English equivalent of the ingredient name (English Name)',
    quantity_expression     VARCHAR(200)    NULL     COMMENT 'Faithful transcription of how the quantity was expressed e.g. dua sudu besar, sikit je',
    quantity_category       VARCHAR(50)     NULL     COMMENT 'Category from controlled vocabulary in Appendix A',
    quantity_unit_culinary  VARCHAR(100)    NULL     COMMENT 'Standardised culinary unit from controlled vocabulary in Appendix B; NULL for vague/taste-based/not_mentioned categories',
    quantity_value_culinary FLOAT           NULL     COMMENT 'Numeric count or measurement extracted from transcript e.g. 0.5, 1.5, 2.5',
    estimated_weight_g      FLOAT           NULL     COMMENT 'Ingredient weight in grams derived via Layer 2 unit conversion',
    calories                FLOAT           NULL     COMMENT 'Ground truth caloric value in kilocalories (kcal)',
    total_fat_g             FLOAT           NULL     COMMENT 'Ground truth total fat content in grams',
    saturated_fat_g         FLOAT           NULL     COMMENT 'Ground truth saturated fat content in grams',
    cholesterol_mg          FLOAT           NULL     COMMENT 'Ground truth cholesterol content in milligrams',
    sodium_mg               FLOAT           NULL     COMMENT 'Ground truth sodium content in milligrams',
    total_carbohydrate_g    FLOAT           NULL     COMMENT 'Ground truth total carbohydrate content in grams',
    dietary_fiber_g         FLOAT           NULL     COMMENT 'Ground truth dietary fiber content in grams',
    total_sugars_g          FLOAT           NULL     COMMENT 'Ground truth total sugars content in grams',
    protein_g               FLOAT           NULL     COMMENT 'Ground truth protein content in grams',
    vitamin_d_mcg           FLOAT           NULL     COMMENT 'Ground truth vitamin D content in micrograms',
    calcium_mg              FLOAT           NULL     COMMENT 'Ground truth calcium content in milligrams',
    iron_mg                 FLOAT           NULL     COMMENT 'Ground truth iron content in milligrams',
    potassium_mg            FLOAT           NULL     COMMENT 'Ground truth potassium content in milligrams',
    annotation_layer        VARCHAR(10)     NOT NULL COMMENT 'layer1 = faithful transcription; layer2 = standardised unit conversion',
    annotator_matric        VARCHAR(20)     NOT NULL COMMENT 'Matric number of the annotator who completed this record',
    annotator_name          VARCHAR(100)    NOT NULL COMMENT 'Full name of the annotator who completed this record',
    annotated_at            TIMESTAMP       NULL     COMMENT 'Timestamp recording when the annotation was submitted',

    CONSTRAINT pk_gt_ingredient     PRIMARY KEY (gt_ingredient_id),
    CONSTRAINT fk_gt_ingredient_reel FOREIGN KEY (gt_reel_id)
        REFERENCES ground_truth_reel (gt_reel_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ingredient-level ground truth annotations with nutritional values';


-- =============================================================
-- GROUP 3: EXPERIMENT & RESULTS
-- =============================================================

-- -------------------------------------------------------------
-- TABLE: llm_model
-- Reference table for the four LLMs used in the study.
-- Contains exactly four records; static after system setup.
-- -------------------------------------------------------------
CREATE TABLE llm_model (
    model_id    INT             NOT NULL AUTO_INCREMENT,
    model_name  VARCHAR(100)    NOT NULL COMMENT 'Display name e.g. Llama 3.1 8B Instruct',
    model_tag   VARCHAR(100)    NOT NULL COMMENT 'Ollama model tag e.g. llama3.1:8b',
    provider    VARCHAR(100)    NOT NULL COMMENT 'Model provider e.g. Meta, Mistral AI, Alibaba, Saama AI',
    description TEXT            NULL     COMMENT 'Brief description of the model and its relevance to the study',
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',

    CONSTRAINT pk_llm_model PRIMARY KEY (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reference table for the four LLMs evaluated in the study';


-- -------------------------------------------------------------
-- TABLE: prompt_technique
-- Reference table for the four prompt engineering techniques.
-- Contains exactly four records; static after system setup.
-- Prompt content is stored in external text files; this table
-- stores only relative file paths.
-- -------------------------------------------------------------
CREATE TABLE prompt_technique (
    technique_id        INT             NOT NULL AUTO_INCREMENT,
    technique_name      VARCHAR(50)     NOT NULL COMMENT 'Technique name e.g. zero-shot, few-shot, chain-of-thought, structured-output',
    system_prompt_file  VARCHAR(500)    NOT NULL COMMENT 'Relative path to system prompt file e.g. prompts/zero_shot_system.txt',
    user_prompt_file    VARCHAR(500)    NOT NULL COMMENT 'Relative path to user prompt template file e.g. prompts/zero_shot_user.txt',
    prompt_version      VARCHAR(10)     NOT NULL COMMENT 'Version of the prompt files e.g. 1.0',
    description         TEXT            NULL     COMMENT 'Brief description of the technique and its characteristics',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',

    CONSTRAINT pk_prompt_technique PRIMARY KEY (technique_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Reference table for the four prompt engineering techniques';


-- -------------------------------------------------------------
-- TABLE: experiment
-- Central linking table for one experimental run. Each row
-- captures a unique combination of transcript × LLM × technique.
-- Phase 1: 16 conditions per transcript (4 models × 4 techniques).
-- -------------------------------------------------------------
CREATE TABLE experiment (
    experiment_id   INT             NOT NULL AUTO_INCREMENT,
    transcript_id   INT             NOT NULL COMMENT 'FK → transcript.transcript_id',
    model_id        INT             NOT NULL COMMENT 'FK → llm_model.model_id',
    technique_id    INT             NOT NULL COMMENT 'FK → prompt_technique.technique_id',
    rag_enabled     BOOLEAN         NOT NULL DEFAULT FALSE COMMENT 'False for Phase 1 (prompt engineering only); True for Phase 2 (RAG)',
    status          VARCHAR(20)     NOT NULL DEFAULT 'pending' COMMENT 'Execution status: pending, running, completed, failed',
    executed_at     TIMESTAMP       NULL     COMMENT 'Timestamp when the experiment was executed',

    CONSTRAINT pk_experiment            PRIMARY KEY (experiment_id),
    CONSTRAINT fk_experiment_transcript FOREIGN KEY (transcript_id)
        REFERENCES transcript (transcript_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_experiment_model      FOREIGN KEY (model_id)
        REFERENCES llm_model (model_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_experiment_technique  FOREIGN KEY (technique_id)
        REFERENCES prompt_technique (technique_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Experimental runs: each row = one transcript × LLM × prompt technique combination';


-- -------------------------------------------------------------
-- TABLE: nutrition_result
-- Stores the structured nutritional output produced by the LLM
-- for each experiment run. One record per experiment.
-- Nutrition values recorded at both per-serving and whole-recipe
-- levels. Raw LLM response preserved for debugging.
-- -------------------------------------------------------------
CREATE TABLE nutrition_result (
    result_id               INT             NOT NULL AUTO_INCREMENT,
    experiment_id           INT             NOT NULL COMMENT 'FK → experiment.experiment_id',
    recipe_name             VARCHAR(200)    NULL     COMMENT 'Recipe name extracted by the LLM',
    servings_estimated      INT             NULL     COMMENT 'Number of servings estimated by the LLM',

    -- Per-serving values
    serving_calories        FLOAT           NULL     COMMENT 'Calories per serving (kcal)',
    serving_total_fat_g     FLOAT           NULL     COMMENT 'Total fat per serving in grams',
    serving_saturated_fat_g FLOAT           NULL     COMMENT 'Saturated fat per serving in grams',
    serving_cholesterol_mg  FLOAT           NULL     COMMENT 'Cholesterol per serving in milligrams',
    serving_sodium_mg       FLOAT           NULL     COMMENT 'Sodium per serving in milligrams',
    serving_carbohydrate_g  FLOAT           NULL     COMMENT 'Total carbohydrate per serving in grams',
    serving_fiber_g         FLOAT           NULL     COMMENT 'Dietary fiber per serving in grams',
    serving_sugars_g        FLOAT           NULL     COMMENT 'Total sugars per serving in grams',
    serving_protein_g       FLOAT           NULL     COMMENT 'Protein per serving in grams',
    serving_vitamin_d_mcg   FLOAT           NULL     COMMENT 'Vitamin D per serving in micrograms',
    serving_calcium_mg      FLOAT           NULL     COMMENT 'Calcium per serving in milligrams',
    serving_iron_mg         FLOAT           NULL     COMMENT 'Iron per serving in milligrams',
    serving_potassium_mg    FLOAT           NULL     COMMENT 'Potassium per serving in milligrams',

    -- Whole-recipe totals
    total_calories          FLOAT           NULL     COMMENT 'Total calories for the full recipe (kcal)',
    total_fat_g             FLOAT           NULL     COMMENT 'Total fat for the full recipe in grams',
    total_saturated_fat_g   FLOAT           NULL     COMMENT 'Total saturated fat for the full recipe in grams',
    total_cholesterol_mg    FLOAT           NULL     COMMENT 'Total cholesterol for the full recipe in milligrams',
    total_sodium_mg         FLOAT           NULL     COMMENT 'Total sodium for the full recipe in milligrams',
    total_carbohydrate_g    FLOAT           NULL     COMMENT 'Total carbohydrate for the full recipe in grams',
    total_fiber_g           FLOAT           NULL     COMMENT 'Total dietary fiber for the full recipe in grams',
    total_sugars_g          FLOAT           NULL     COMMENT 'Total sugars for the full recipe in grams',
    total_protein_g         FLOAT           NULL     COMMENT 'Total protein for the full recipe in grams',
    total_vitamin_d_mcg     FLOAT           NULL     COMMENT 'Total vitamin D for the full recipe in micrograms',
    total_calcium_mg        FLOAT           NULL     COMMENT 'Total calcium for the full recipe in milligrams',
    total_iron_mg           FLOAT           NULL     COMMENT 'Total iron for the full recipe in milligrams',
    total_potassium_mg      FLOAT           NULL     COMMENT 'Total potassium for the full recipe in milligrams',

    -- LLM output metadata
    raw_json_output         TEXT            NULL     COMMENT 'Original raw JSON response from the LLM for debugging',
    json_valid              BOOLEAN         NULL     COMMENT 'True if the LLM output was valid parseable JSON',
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',

    CONSTRAINT pk_nutrition_result      PRIMARY KEY (result_id),
    CONSTRAINT fk_nutrition_experiment  FOREIGN KEY (experiment_id)
        REFERENCES experiment (experiment_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM-generated structured nutritional output per experiment run';


-- -------------------------------------------------------------
-- TABLE: ingredient_result
-- Stores each individual ingredient extracted by the LLM as
-- part of a nutrition result. One nutrition result may contain
-- multiple ingredient records.
-- -------------------------------------------------------------
CREATE TABLE ingredient_result (
    ingredient_id       INT             NOT NULL AUTO_INCREMENT,
    result_id           INT             NOT NULL COMMENT 'FK → nutrition_result.result_id',
    name_original       VARCHAR(200)    NULL     COMMENT 'Ingredient name as spoken in the transcript',
    name_en             VARCHAR(200)    NULL     COMMENT 'English translation of the ingredient name',
    quantity_value      FLOAT           NULL     COMMENT 'Numeric quantity as extracted by LLM',
    unit_original       VARCHAR(100)    NULL     COMMENT 'Unit as extracted by LLM e.g. sudu besar',
    unit_en             VARCHAR(100)    NULL     COMMENT 'English translation of the unit',
    estimated_weight_g  FLOAT           NULL     COMMENT 'Estimated weight in grams as computed by LLM',
    calories            FLOAT           NULL     COMMENT 'Estimated calories in kilocalories (kcal)',
    total_fat_g         FLOAT           NULL     COMMENT 'Estimated total fat in grams',
    saturated_fat_g     FLOAT           NULL     COMMENT 'Estimated saturated fat in grams',
    cholesterol_mg      FLOAT           NULL     COMMENT 'Estimated cholesterol in milligrams',
    sodium_mg           FLOAT           NULL     COMMENT 'Estimated sodium in milligrams',
    total_carbohydrate_g FLOAT          NULL     COMMENT 'Estimated total carbohydrate in grams',
    dietary_fiber_g     FLOAT           NULL     COMMENT 'Estimated dietary fiber in grams',
    total_sugars_g      FLOAT           NULL     COMMENT 'Estimated total sugars in grams',
    protein_g           FLOAT           NULL     COMMENT 'Estimated protein in grams',
    vitamin_d_mcg       FLOAT           NULL     COMMENT 'Estimated vitamin D in micrograms',
    calcium_mg          FLOAT           NULL     COMMENT 'Estimated calcium in milligrams',
    iron_mg             FLOAT           NULL     COMMENT 'Estimated iron in milligrams',
    potassium_mg        FLOAT           NULL     COMMENT 'Estimated potassium in milligrams',

    CONSTRAINT pk_ingredient_result     PRIMARY KEY (ingredient_id),
    CONSTRAINT fk_ingredient_nutrition  FOREIGN KEY (result_id)
        REFERENCES nutrition_result (result_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ingredient-level nutritional values extracted by the LLM per experiment run';


-- =============================================================
-- End of script
-- Total tables created: 11
-- Creation order respects all foreign key dependencies
-- =============================================================
