package edu.utem.ftmk.server;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * ExportEngine
 *
 * Runs the 10 defined metric SQL queries (mapped by label) and writes the
 * results to UTF-8 encoded CSV files in the specified output folder.
 *
 * Query labels:
 *   LAYER1A, LAYER1B, LAYER2A, LAYER2B, LAYER2C,
 *   LAYER3A, LAYER3B, LAYER3C, LAYER4, LAYER5
 */
public class ExportEngine {

    // ---------------------------------------------------------------------------
    // Configuration constants
    // ---------------------------------------------------------------------------

    /** Base path where the SQL query files are stored (informational reference). */
    public static final String BASE_QUERY_PATH =
            "src/main/resources/sql/metrics_evaluation_queries.sql";

    /** Default base path for CSV export output. */
    public static final String BASE_EXPORT_PATH = "exports";

    // ---------------------------------------------------------------------------
    // SQL query registry
    // ---------------------------------------------------------------------------

    /** Maps each query label to its hard-coded SQL string. */
    private static final Map<String, String> QUERY_MAP = new HashMap<>();

    /** Maps each query label to the CSV filename that will be produced. */
    private static final Map<String, String> FILENAME_MAP = new HashMap<>();

    static {
        // ---- LAYER1A ----
        QUERY_MAP.put("LAYER1A",
                "SELECT e.experiment_id, t.video_id, m.model_name, pt.technique_name, " +
                "gti.name_original AS gt_name_original, gti.name_en AS gt_name_en, " +
                "gti.unit_original AS gt_unit_original, gti.unit_en AS gt_unit_en, " +
                "ir.name_original AS pred_name_original, ir.name_en AS pred_name_en, " +
                "ir.unit_original AS pred_unit_original, ir.unit_en AS pred_unit_en " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY e.experiment_id, gti.gt_ingredient_id");
        FILENAME_MAP.put("LAYER1A", "layer1a_name_unit_matching.csv");

        // ---- LAYER1B ----
        QUERY_MAP.put("LAYER1B",
                "SELECT e.experiment_id, t.video_id, m.model_name, pt.technique_name, " +
                "gti.name_original AS gt_name_original, gti.name_en AS gt_name_en, " +
                "ir.name_original AS pred_name_original, ir.name_en AS pred_name_en " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY e.experiment_id, gti.gt_ingredient_id");
        FILENAME_MAP.put("LAYER1B", "layer1b_name_matching_only.csv");

        // ---- LAYER2A ----
        QUERY_MAP.put("LAYER2A",
                "SELECT e.experiment_id, t.video_id, m.model_name, pt.technique_name, " +
                "gti.quantity_value_culinary AS gt_quantity_value, gti.estimated_weight_g AS gt_weight_g, " +
                "ir.quantity_value AS pred_quantity_value, ir.estimated_weight_g AS pred_weight_g " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE e.status = 'completed' AND gti.annotation_layer = 'layer2' " +
                "ORDER BY e.experiment_id, gti.gt_ingredient_id");
        FILENAME_MAP.put("LAYER2A", "layer2a_quantity_weight.csv");

        // ---- LAYER2B ----
        QUERY_MAP.put("LAYER2B",
                "SELECT e.experiment_id, t.video_id, m.model_name, pt.technique_name, " +
                "gti.calories AS gt_energy_kcal, gti.protein_g AS gt_protein_g, " +
                "gti.total_fat_g AS gt_fat_g, gti.total_carbohydrate_g AS gt_carbohydrate_g, " +
                "ir.calories AS pred_energy_kcal, ir.protein_g AS pred_protein_g, " +
                "ir.total_fat_g AS pred_fat_g, ir.total_carbohydrate_g AS pred_carbohydrate_g " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE e.status = 'completed' AND gti.annotation_layer = 'layer2' " +
                "ORDER BY e.experiment_id, gti.gt_ingredient_id");
        FILENAME_MAP.put("LAYER2B", "layer2b_per_ingredient_nutrition.csv");

        // ---- LAYER2C ----
        QUERY_MAP.put("LAYER2C",
                "SELECT e.experiment_id, t.video_id, m.model_name, pt.technique_name, " +
                "SUM(gti.calories) AS gt_total_kcal, SUM(gti.protein_g) AS gt_total_protein_g, " +
                "SUM(gti.total_fat_g) AS gt_total_fat_g, SUM(gti.total_carbohydrate_g) AS gt_total_carbohydrate_g, " +
                "nr.total_calories AS pred_total_kcal, nr.total_protein_g AS pred_total_protein_g, " +
                "nr.total_fat_g AS pred_total_fat_g, nr.total_carbohydrate_g AS pred_total_carbohydrate_g " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE e.status = 'completed' AND gti.annotation_layer = 'layer2' " +
                "GROUP BY e.experiment_id, t.video_id, m.model_name, pt.technique_name, " +
                "nr.total_calories, nr.total_protein_g, nr.total_fat_g, nr.total_carbohydrate_g " +
                "ORDER BY e.experiment_id");
        FILENAME_MAP.put("LAYER2C", "layer2c_total_nutrition_per_video.csv");

        // ---- LAYER3A ----
        QUERY_MAP.put("LAYER3A",
                "SELECT m.model_name, pt.technique_name, COUNT(*) AS total_runs, " +
                "SUM(CASE WHEN nr.json_valid = TRUE THEN 1 ELSE 0 END) AS valid_count, " +
                "SUM(CASE WHEN nr.json_valid = FALSE THEN 1 ELSE 0 END) AS invalid_count, " +
                "ROUND(SUM(CASE WHEN nr.json_valid = TRUE THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS validity_rate_pct " +
                "FROM experiment e " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "WHERE e.status = 'completed' " +
                "GROUP BY m.model_name, pt.technique_name " +
                "ORDER BY m.model_name, pt.technique_name");
        FILENAME_MAP.put("LAYER3A", "layer3a_json_validity_rate.csv");

        // ---- LAYER3B ----
        QUERY_MAP.put("LAYER3B",
                "SELECT e.experiment_id, t.video_id, m.model_name, pt.technique_name, " +
                "ir.name_original AS pred_name_original, ir.name_en AS pred_name_en, ir.is_hallucinated " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY e.experiment_id, ir.ingredient_id");
        FILENAME_MAP.put("LAYER3B", "layer3b_hallucination_per_ingredient.csv");

        // ---- LAYER3C ----
        QUERY_MAP.put("LAYER3C",
                "SELECT e.experiment_id, t.video_id, m.model_name, pt.technique_name, " +
                "COUNT(DISTINCT gti.gt_ingredient_id) AS gt_ingredient_count, " +
                "COUNT(DISTINCT ir.ingredient_id) AS pred_ingredient_count, " +
                "SUM(CASE WHEN ir.is_hallucinated = FALSE THEN 1 ELSE 0 END) AS true_positives, " +
                "SUM(CASE WHEN ir.is_hallucinated = TRUE THEN 1 ELSE 0 END) AS false_positives " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE e.status = 'completed' " +
                "GROUP BY e.experiment_id, t.video_id, m.model_name, pt.technique_name " +
                "ORDER BY e.experiment_id");
        FILENAME_MAP.put("LAYER3C", "layer3c_tp_fp_per_experiment.csv");

        // ---- LAYER4 ----
        QUERY_MAP.put("LAYER4",
                "SELECT 'Layer 4 Human Evaluation not yet available - Phase 2 only' AS note");
        FILENAME_MAP.put("LAYER4", "layer4_human_evaluation_placeholder.csv");

        // ---- LAYER5 ----
        QUERY_MAP.put("LAYER5",
                "SELECT t.video_id, m.model_name, pt.technique_name, " +
                "COUNT(DISTINCT ir.ingredient_id) AS pred_count, " +
                "SUM(CASE WHEN ir.is_hallucinated = FALSE THEN 1 ELSE 0 END) AS true_positives, " +
                "SUM(CASE WHEN ir.is_hallucinated = TRUE THEN 1 ELSE 0 END) AS false_positives, " +
                "COUNT(DISTINCT gti.gt_ingredient_id) AS gt_count, " +
                "nr.json_valid, nr.total_calories AS pred_total_kcal, " +
                "SUM(gti.calories) AS gt_total_kcal " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE e.status = 'completed' AND gti.annotation_layer = 'layer2' " +
                "GROUP BY t.video_id, m.model_name, pt.technique_name, nr.json_valid, nr.total_calories " +
                "ORDER BY t.video_id, m.model_name, pt.technique_name");
        FILENAME_MAP.put("LAYER5", "layer5_composite_summary.csv");
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Runs the SQL query associated with {@code queryLabel}, writes the results
     * to a CSV file inside {@code outputFolder}, and returns the absolute path
     * of the written file.
     *
     * <p>The output folder is created automatically if it does not exist.</p>
     *
     * @param queryLabel   One of: LAYER1A, LAYER1B, LAYER2A, LAYER2B, LAYER2C,
     *                     LAYER3A, LAYER3B, LAYER3C, LAYER4, LAYER5
     *                     (case-insensitive).
     * @param outputFolder Destination folder path for the CSV file.
     * @return Absolute path of the written CSV file.
     * @throws IllegalArgumentException if {@code queryLabel} is not recognised.
     * @throws Exception                on any SQL or I/O error.
     */
    public String exportCsv(String queryLabel, String outputFolder) throws Exception {

        // Normalise the label so callers are not case-sensitive.
        String label = (queryLabel == null) ? "" : queryLabel.trim().toUpperCase();

        String sql = QUERY_MAP.get(label);
        if (sql == null) {
            throw new IllegalArgumentException(
                    "Unknown query label: '" + queryLabel + "'. " +
                    "Valid labels are: LAYER1A, LAYER1B, LAYER2A, LAYER2B, LAYER2C, " +
                    "LAYER3A, LAYER3B, LAYER3C, LAYER4, LAYER5");
        }

        String filename = FILENAME_MAP.get(label);

        // Ensure output directory exists.
        File outDir = new File(outputFolder);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        File csvFile = new File(outDir, filename);

        // Execute the query and stream results into the CSV file.
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(
                             new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Write header row using ResultSetMetaData column labels.
            writer.write(buildHeaderRow(meta, columnCount));
            writer.newLine();

            // Write data rows.
            while (rs.next()) {
                writer.write(buildDataRow(rs, columnCount));
                writer.newLine();
            }
        }

        return csvFile.getAbsolutePath();
    }

    /**
     * Runs the SQL query associated with {@code queryLabel} and returns the data
     * for preview in the UI.
     *
     * @param queryLabel One of the LAYER labels.
     * @return A map containing "headers" (List of String) and "data" (List of List of String).
     */
    public Map<String, Object> previewData(String queryLabel) throws Exception {
        String label = (queryLabel == null) ? "" : queryLabel.trim().toUpperCase();
        String sql = QUERY_MAP.get(label);
        if (sql == null) {
            throw new IllegalArgumentException("Unknown query label: '" + queryLabel + "'");
        }

        java.util.List<String> headers = new java.util.ArrayList<>();
        java.util.List<java.util.List<String>> data = new java.util.ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                headers.add(meta.getColumnLabel(i));
            }

            while (rs.next()) {
                java.util.List<String> row = new java.util.ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    row.add(value == null ? "" : value);
                }
                data.add(row);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("headers", headers);
        result.put("data", data);
        return result;
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Builds a comma-separated header row from the {@link ResultSetMetaData}.
     *
     * @param meta        the result-set metadata.
     * @param columnCount number of columns.
     * @return a single CSV header line (no trailing newline).
     */
    private String buildHeaderRow(ResultSetMetaData meta, int columnCount)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                sb.append(',');
            }
            sb.append(escapeCsvValue(meta.getColumnLabel(i)));
        }
        return sb.toString();
    }

    /**
     * Builds a comma-separated data row from the current {@link ResultSet} position.
     *
     * @param rs          the result set, positioned at the current row.
     * @param columnCount number of columns.
     * @return a single CSV data line (no trailing newline).
     */
    private String buildDataRow(ResultSet rs, int columnCount) throws Exception {

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                sb.append(',');
            }
            String value = rs.getString(i);
            // Treat SQL NULL as an empty field.
            sb.append(escapeCsvValue(value == null ? "" : value));
        }
        return sb.toString();
    }

    /**
     * Wraps a field value in double-quotes if it contains a comma, a
     * double-quote, or a newline character. Any embedded double-quotes are
     * escaped by doubling them (RFC 4180 convention).
     *
     * @param value the raw cell value.
     * @return the CSV-safe representation of {@code value}.
     */
    private String escapeCsvValue(String value) {

        if (value == null) {
            return "";
        }

        // Quote the field if it contains a comma, a double-quote, or a newline.
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            // Escape any existing double-quotes by doubling them.
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }

        return value;
    }
}
