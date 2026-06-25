-- =============================================================
-- MasakGramPrompt — Seed Data
-- Project : BITP 3123 Distributed Application Development
-- System  : Nutritional LLM Analysis System
-- Tables  : llm_model, prompt_technique
-- Version : 2.0
-- Date    : 2026-06-18
-- Author  : Emaliana Kasmuri
--           Fakulti Teknologi Maklumat dan Komunikasi
--           Universiti Teknikal Malaysia Melaka
-- =============================================================
--
-- This script reseeds the two reference tables used by the
-- experiment layer. It first deletes any existing rows so it
-- can be re-run safely, then inserts the current set of five
-- LLM models and four prompt techniques.
--
-- WARNING: The DELETE block below removes all rows from
-- llm_model and prompt_technique. Run this only before any
-- experiment results have been recorded. If the experiment
-- table already references these rows, re-run the experiments
-- afterwards, since the model_id and technique_id values are
-- regenerated from 1.
-- =============================================================


-- -------------------------------------------------------------
-- STEP 1: Delete existing seed data
-- -------------------------------------------------------------
-- Foreign key checks are disabled only for the duration of the
-- delete so the reset does not fail if dependent rows exist.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM llm_model;
DELETE FROM prompt_technique;

ALTER TABLE llm_model       AUTO_INCREMENT = 1;
ALTER TABLE prompt_technique AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;


-- -------------------------------------------------------------
-- STEP 2: Insert LLM models (5 models)
-- -------------------------------------------------------------
-- model_id and created_at are omitted and auto-populated by MySQL.
-- model_tag stores the exact Ollama pull tag, case-sensitive.

INSERT INTO llm_model (model_name, model_tag, provider, description) VALUES
(
    'Llama 3.2 3B Instruct',
    'llama3.2:3b',
    'Meta',
    'Meta Llama 3.2 3B instruction-tuned model. Selected as a compact general-purpose baseline that runs comfortably on medium-specification machines while retaining solid multilingual comprehension for EN-MS code-switched nutritional extraction.'
),
(
    'Phi-4-mini 3.8B Instruct',
    'phi4-mini',
    'Microsoft',
    'Microsoft Phi-4-mini 3.8B instruction-tuned model. Selected as an efficiency benchmark, offering strong reasoning density per parameter at a small footprint suitable for CPU-based batch processing of cooking transcripts.'
),
(
    'Qwen 2.5 3B Instruct',
    'qwen2.5:3b',
    'Alibaba',
    'Alibaba Qwen 2.5 3B instruction-tuned model. Selected for its broad multilingual coverage including Malay and English, providing a general multilingual reference point for code-switched gastronomy transcript analysis.'
),
(
    'Gemma-SEA-LION v4 4B',
    'aisingapore/Gemma-SEA-LION-v4-4B-VL',
    'AI Singapore',
    'AI Singapore Gemma-SEA-LION v4 4B model, post-trained on Southeast Asian languages including Malay on a Gemma 3 base. Selected to test whether regional language adaptation improves extraction accuracy on EN-MS code-switched cooking transcripts compared with general multilingual models.'
),
(
    'MedGemma 4B',
    'medgemma:4b',
    'Google',
    'Google MedGemma 4B model adapted for medical and biomedical text on a Gemma 3 base. Selected to evaluate whether domain-specific pretraining improves nutritional information extraction accuracy relative to general-purpose instruction-tuned models.'
);


-- -------------------------------------------------------------
-- STEP 3: Insert prompt techniques (4 techniques)
-- -------------------------------------------------------------
-- technique_id and created_at are omitted and auto-populated by MySQL.
-- system_prompt_file and user_prompt_file store relative paths.

INSERT INTO prompt_technique (technique_name, system_prompt_file, user_prompt_file, prompt_version, description) VALUES
(
    'zero-shot',
    'prompts/zero_shot_system.txt',
    'prompts/zero_shot_user.txt',
    '1.0',
    'Zero-shot prompting provides the model with task instructions and output format requirements only, without any examples. The model relies entirely on its pretrained knowledge to extract nutritional information from EN-MS code-switched cooking transcripts.'
),
(
    'few-shot',
    'prompts/few_shot_system.txt',
    'prompts/few_shot_user.txt',
    '1.0',
    'Few-shot prompting supplies the model with a small number of annotated input-output examples before the target transcript. This guides the model toward the expected JSON output structure and handling of informal Malay culinary expressions.'
),
(
    'chain-of-thought',
    'prompts/chain_of_thought_system.txt',
    'prompts/chain_of_thought_user.txt',
    '1.0',
    'Chain-of-thought prompting instructs the model to reason step-by-step before producing the final JSON output. This encourages explicit intermediate reasoning over ingredient identification, quantity interpretation, and language tagging in code-switched text.'
),
(
    'structured-output',
    'prompts/structured_output_system.txt',
    'prompts/structured_output_user.txt',
    '1.0',
    'Structured-output prompting enforces a strict JSON schema in the prompt instructions, constraining the model response to a predefined format. This reduces hallucination risk and improves consistency of the extracted nutritional fields across all five LLMs.'
);
