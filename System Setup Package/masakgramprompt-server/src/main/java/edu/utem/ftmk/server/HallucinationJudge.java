package edu.utem.ftmk.server;

import edu.utem.ftmk.llm.LLMService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * HallucinationJudge is a post-processing step that uses a chosen LLM model
 * to determine whether each predicted ingredient is hallucinated (i.e., not
 * present in the ground truth list for that reel).
 *
 * It processes all completed experiments where is_hallucinated is still NULL,
 * and updates each ingredient_result row with TRUE (hallucinated) or FALSE (valid).
 *
 * The judge model is chosen by the user at runtime — any of the 5 available
 * Ollama models can be selected, allowing flexibility in the evaluation setup.
 */
public class HallucinationJudge {

    private final LLMService llmService = new LLMService();

    /**
     * Judges all un-judged ingredient_result rows across all completed experiments.
     * Each experiment's ingredients are judged by the same LLM model that ran
     * the experiment — keeping evaluation self-contained per condition.
     *
     * @param conn Active database connection
     * @return Total number of ingredients judged
     */
    public int judgeAll(Connection conn) throws Exception {
        int totalJudged = 0;

        // Find all completed experiments that still have un-judged ingredients,
        // along with the model_tag that was used for that experiment
        String expQuery =
            "SELECT DISTINCT e.experiment_id, e.transcript_id, m.model_tag, m.model_name " +
            "FROM experiment e " +
            "JOIN llm_model m ON e.model_id = m.model_id " +
            "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
            "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
            "WHERE e.status = 'completed' AND ir.is_hallucinated IS NULL";

        List<Object[]> experiments = new ArrayList<>();
        try (PreparedStatement s = conn.prepareStatement(expQuery);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                experiments.add(new Object[]{
                    rs.getInt("experiment_id"),
                    rs.getInt("transcript_id"),
                    rs.getString("model_tag"),
                    rs.getString("model_name")
                });
            }
        }

        System.out.println("[HallucinationJudge] Found " + experiments.size() + " experiment(s) to judge.");

        for (Object[] exp : experiments) {
            int    experimentId = (int)    exp[0];
            int    transcriptId = (int)    exp[1];
            String modelTag     = (String) exp[2];
            String modelName    = (String) exp[3];

            System.out.println("[HallucinationJudge] Exp #" + experimentId + " → judging with: " + modelName);

            // Load ground truth ingredient names (English) for this transcript's reel
            List<String> groundTruthNames = loadGroundTruth(conn, transcriptId);
            if (groundTruthNames.isEmpty()) {
                System.out.println("[HallucinationJudge] No ground truth for experiment " + experimentId + " — skipping.");
                continue;
            }

            String gtList = String.join(", ", groundTruthNames);

            // Load result_id for this experiment
            int resultId = -1;
            try (PreparedStatement s = conn.prepareStatement(
                    "SELECT result_id FROM nutrition_result WHERE experiment_id = ? LIMIT 1")) {
                s.setInt(1, experimentId);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) resultId = rs.getInt("result_id");
                }
            }
            if (resultId < 0) continue;

            // Load un-judged predicted ingredients for this result
            String ingQuery =
                "SELECT ingredient_id, name_en, name_original " +
                "FROM ingredient_result " +
                "WHERE result_id = ? AND is_hallucinated IS NULL";

            try (PreparedStatement s = conn.prepareStatement(ingQuery)) {
                s.setInt(1, resultId);
                try (ResultSet rs = s.executeQuery()) {
                    while (rs.next()) {
                        int ingredientId   = rs.getInt("ingredient_id");
                        String predNameEn  = rs.getString("name_en");
                        String predNameOrig = rs.getString("name_original");

                        // Use whichever name is available
                        String predName = (predNameEn != null && !predNameEn.isEmpty())
                                ? predNameEn : predNameOrig;
                        if (predName == null || predName.isEmpty()) {
                            predName = "unknown";
                        }

                        boolean isHallucinated = judgeIngredient(modelTag, predName, gtList);

                        // Update the DB
                        try (PreparedStatement upd = conn.prepareStatement(
                                "UPDATE ingredient_result SET is_hallucinated = ? WHERE ingredient_id = ?")) {
                            upd.setBoolean(1, isHallucinated);
                            upd.setInt(2, ingredientId);
                            upd.executeUpdate();
                        }

                        System.out.println("[HallucinationJudge] Exp #" + experimentId +
                            " | '" + predName + "' → " + (isHallucinated ? "HALLUCINATED" : "valid"));
                        totalJudged++;
                    }
                }
            }
        }

        System.out.println("[HallucinationJudge] Done. Total ingredients judged: " + totalJudged);
        return totalJudged;
    }

    /**
     * Asks the judge LLM whether a predicted ingredient matches any item
     * in the ground truth list. Returns true if hallucinated (not found).
     */
    private boolean judgeIngredient(String modelTag, String predName, String gtList) {
        String prompt =
            "You are a food ingredient matching assistant.\n" +
            "Ground truth ingredients: " + gtList + "\n" +
            "Predicted ingredient: '" + predName + "'\n" +
            "Is the predicted ingredient the same food item as any ingredient in the ground truth list? " +
            "(Consider different spellings, languages, or phrasings of the same food.)\n" +
            "Reply with only YES or NO.";
        try {
            String response = llmService.prompt(modelTag, prompt).trim().toUpperCase();
            // Treat any response containing NO as hallucinated
            return response.startsWith("NO") || response.contains(" NO");
        } catch (Exception e) {
            System.err.println("[HallucinationJudge] LLM call failed for '" + predName + "': " + e.getMessage());
            // If the LLM fails, default to not hallucinated (conservative)
            return false;
        }
    }

    /**
     * Loads all ground truth ingredient name_en values for the reel
     * associated with the given transcript_id.
     */
    private List<String> loadGroundTruth(Connection conn, int transcriptId) throws Exception {
        List<String> names = new ArrayList<>();
        String q =
            "SELECT gti.name_en " +
            "FROM ground_truth_reel gtr " +
            "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
            "WHERE gtr.transcript_id = ?";
        try (PreparedStatement s = conn.prepareStatement(q)) {
            s.setInt(1, transcriptId);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name_en");
                    if (name != null && !name.isEmpty()) names.add(name);
                }
            }
        }
        return names;
    }
}
