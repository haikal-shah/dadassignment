package edu.utem.ftmk.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * ResultSaver parses the raw JSON string returned by the LLM and
 * saves the structured nutritional data into the nutrition_result
 * and ingredient_result tables.
 *
 * We use a hand-rolled JSON parser to avoid adding external dependencies
 * like Jackson or Gson to this project.
 */
public class ResultSaver {

    /**
     * Extracts a string value from a raw JSON string by key.
     * e.g. extractString("{\"recipe_name\":\"Telur Goreng\"}", "recipe_name") => "Telur Goreng"
     */
    private String extractString(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int idx = json.indexOf(search);
            if (idx < 0) return null;
            int start = json.indexOf("\"", idx + search.length()) + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) { return null; }
    }

    /**
     * Extracts a numeric (double) value from a raw JSON string by key.
     */
    private double extractDouble(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int idx = json.indexOf(search);
            if (idx < 0) return 0.0;
            int start = idx + search.length();
            while (start < json.length() && (Character.isWhitespace(json.charAt(start)) || json.charAt(start) == '"')) start++;
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end))
                || json.charAt(end) == '.' || json.charAt(end) == '-' || json.charAt(end) == '/')) end++;
            
            String val = json.substring(start, end);
            if (val.contains("/")) {
                String[] parts = val.split("/");
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            }
            return Double.parseDouble(val);
        } catch (Exception e) { return 0.0; }
    }

    private int extractInt(String json, String key) {
        return (int) extractDouble(json, key);
    }

    /**
     * Extracts a named JSON block (object) from a larger JSON string.
     */
    private String extractBlock(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int idx = json.indexOf(search);
            if (idx < 0) return null;
            int start = json.indexOf("{", idx + search.length());
            if (start < 0) return null;
            int depth = 0, pos = start;
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) return json.substring(start, pos + 1); }
                pos++;
            }
            return null;
        } catch (Exception e) { return null; }
    }

    /**
     * Saves a parsed LLM JSON response to nutrition_result and ingredient_result tables.
     */
    public int saveResult(Connection conn, int experimentId, String rawJson) throws Exception {
        // Extract JSON from potentially noisy LLM output (e.g., markdown blocks, intro text)
        String json = rawJson.trim();
        int firstBrace = json.indexOf('{');
        int lastBrace = json.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace >= 0 && lastBrace > firstBrace) {
            json = json.substring(firstBrace, lastBrace + 1);
        }

        boolean jsonValid = json.startsWith("{") && json.endsWith("}");

        String recipeName  = extractString(json, "recipe_name");
        int servings       = extractInt(json, "servings_estimated");
        if (servings <= 0) servings = 1;

        String servingBlock = extractBlock(json, "amount_per_serving");
        String totalBlock   = extractBlock(json, "nutrition_total");

        String insertResult =
            "INSERT INTO nutrition_result " +
            "(experiment_id, recipe_name, servings_estimated, " +
            " serving_calories, serving_total_fat_g, serving_saturated_fat_g, serving_cholesterol_mg, " +
            " serving_sodium_mg, serving_carbohydrate_g, serving_fiber_g, serving_sugars_g, serving_protein_g, " +
            " serving_vitamin_d_mcg, serving_calcium_mg, serving_iron_mg, serving_potassium_mg, " +
            " total_calories, total_fat_g, total_saturated_fat_g, total_cholesterol_mg, " +
            " total_sodium_mg, total_carbohydrate_g, total_fiber_g, total_sugars_g, total_protein_g, " +
            " total_vitamin_d_mcg, total_calcium_mg, total_iron_mg, total_potassium_mg, " +
            " raw_json_output, json_valid) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        int resultId = -1;
        try (PreparedStatement stmt = conn.prepareStatement(insertResult, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt   (1,  experimentId);
            stmt.setString(2,  recipeName);
            stmt.setInt   (3,  servings);
            stmt.setDouble(4,  servingBlock != null ? extractDouble(servingBlock, "calories") : 0);
            stmt.setDouble(5,  servingBlock != null ? extractDouble(servingBlock, "total_fat_g") : 0);
            stmt.setDouble(6,  servingBlock != null ? extractDouble(servingBlock, "saturated_fat_g") : 0);
            stmt.setDouble(7,  servingBlock != null ? extractDouble(servingBlock, "cholesterol_mg") : 0);
            stmt.setDouble(8,  servingBlock != null ? extractDouble(servingBlock, "sodium_mg") : 0);
            stmt.setDouble(9,  servingBlock != null ? extractDouble(servingBlock, "total_carbohydrate_g") : 0);
            stmt.setDouble(10, servingBlock != null ? extractDouble(servingBlock, "dietary_fiber_g") : 0);
            stmt.setDouble(11, servingBlock != null ? extractDouble(servingBlock, "total_sugars_g") : 0);
            stmt.setDouble(12, servingBlock != null ? extractDouble(servingBlock, "protein_g") : 0);
            stmt.setDouble(13, servingBlock != null ? extractDouble(servingBlock, "vitamin_d_mcg") : 0);
            stmt.setDouble(14, servingBlock != null ? extractDouble(servingBlock, "calcium_mg") : 0);
            stmt.setDouble(15, servingBlock != null ? extractDouble(servingBlock, "iron_mg") : 0);
            stmt.setDouble(16, servingBlock != null ? extractDouble(servingBlock, "potassium_mg") : 0);
            stmt.setDouble(17, totalBlock != null ? extractDouble(totalBlock, "calories") : 0);
            stmt.setDouble(18, totalBlock != null ? extractDouble(totalBlock, "total_fat_g") : 0);
            stmt.setDouble(19, totalBlock != null ? extractDouble(totalBlock, "saturated_fat_g") : 0);
            stmt.setDouble(20, totalBlock != null ? extractDouble(totalBlock, "cholesterol_mg") : 0);
            stmt.setDouble(21, totalBlock != null ? extractDouble(totalBlock, "sodium_mg") : 0);
            stmt.setDouble(22, totalBlock != null ? extractDouble(totalBlock, "total_carbohydrate_g") : 0);
            stmt.setDouble(23, totalBlock != null ? extractDouble(totalBlock, "dietary_fiber_g") : 0);
            stmt.setDouble(24, totalBlock != null ? extractDouble(totalBlock, "total_sugars_g") : 0);
            stmt.setDouble(25, totalBlock != null ? extractDouble(totalBlock, "protein_g") : 0);
            stmt.setDouble(26, totalBlock != null ? extractDouble(totalBlock, "vitamin_d_mcg") : 0);
            stmt.setDouble(27, totalBlock != null ? extractDouble(totalBlock, "calcium_mg") : 0);
            stmt.setDouble(28, totalBlock != null ? extractDouble(totalBlock, "iron_mg") : 0);
            stmt.setDouble(29, totalBlock != null ? extractDouble(totalBlock, "potassium_mg") : 0);
            stmt.setString(30, rawJson);
            stmt.setBoolean(31, jsonValid);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) resultId = keys.getInt(1);
        }

        if (jsonValid && resultId > 0) saveIngredients(conn, resultId, json);
        return resultId;
    }

    /**
     * Parses and saves each ingredient entry from the ingredients array.
     */
    private void saveIngredients(Connection conn, int resultId, String json) throws Exception {
        int arrStart = json.indexOf("\"ingredients\":");
        if (arrStart < 0) return;
        int openBracket  = json.indexOf("[", arrStart);
        int closeBracket = json.lastIndexOf("]");
        if (openBracket < 0 || closeBracket < 0) return;

        String ingredientsJson = json.substring(openBracket + 1, closeBracket);
        int pos = 0;
        while (pos < ingredientsJson.length()) {
            int start = ingredientsJson.indexOf("{", pos);
            if (start < 0) break;
            int depth = 0, end = start;
            while (end < ingredientsJson.length()) {
                char c = ingredientsJson.charAt(end);
                if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) break; }
                end++;
            }
            String obj = ingredientsJson.substring(start, end + 1);

            String sql =
                "INSERT INTO ingredient_result " +
                "(result_id, name_original, name_en, quantity_value, unit_original, unit_en, " +
                " estimated_weight_g, calories, total_fat_g, saturated_fat_g, cholesterol_mg, " +
                " sodium_mg, total_carbohydrate_g, dietary_fiber_g, total_sugars_g, protein_g, " +
                " vitamin_d_mcg, calcium_mg, iron_mg, potassium_mg, is_hallucinated) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NULL)";

            try (PreparedStatement s = conn.prepareStatement(sql)) {
                s.setInt   (1,  resultId);
                s.setString(2,  extractString(obj, "ingredient_name_original"));
                s.setString(3,  extractString(obj, "ingredient_name_en"));
                s.setDouble(4,  extractDouble(obj, "quantity_value"));
                s.setString(5,  extractString(obj, "quantity_unit_original"));
                s.setString(6,  extractString(obj, "quantity_unit_en"));
                s.setDouble(7,  extractDouble(obj, "estimated_weight_g"));
                s.setDouble(8,  extractDouble(obj, "calories"));
                s.setDouble(9,  extractDouble(obj, "total_fat_g"));
                s.setDouble(10, extractDouble(obj, "saturated_fat_g"));
                s.setDouble(11, extractDouble(obj, "cholesterol_mg"));
                s.setDouble(12, extractDouble(obj, "sodium_mg"));
                s.setDouble(13, extractDouble(obj, "total_carbohydrate_g"));
                s.setDouble(14, extractDouble(obj, "dietary_fiber_g"));
                s.setDouble(15, extractDouble(obj, "total_sugars_g"));
                s.setDouble(16, extractDouble(obj, "protein_g"));
                s.setDouble(17, extractDouble(obj, "vitamin_d_mcg"));
                s.setDouble(18, extractDouble(obj, "calcium_mg"));
                s.setDouble(19, extractDouble(obj, "iron_mg"));
                s.setDouble(20, extractDouble(obj, "potassium_mg"));
                s.executeUpdate();
            }
            pos = end + 1;
        }
    }
}
