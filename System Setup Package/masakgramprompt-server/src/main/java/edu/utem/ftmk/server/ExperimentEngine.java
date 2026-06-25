package edu.utem.ftmk.server;

import edu.utem.ftmk.llm.LLMService;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ExperimentEngine {

    private LLMService llmService = new LLMService();

    /**
     * Executes an experiment by reading the transcript, reading the prompts,
     * replacing {{TRANSCRIPT}} with the actual text, and calling the LLM.
     */
    public String runExperiment(int experimentId) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Fetch experiment details: transcript path, model tag, prompt paths
            String query = "SELECT t.file_path, m.model_tag, p.technique_name, p.system_prompt_file, p.user_prompt_file " +
                           "FROM experiment e " +
                           "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                           "JOIN llm_model m ON e.model_id = m.model_id " +
                           "JOIN prompt_technique p ON e.technique_id = p.technique_id " +
                           "WHERE e.experiment_id = ?";
            
            String transcriptPath = null;
            String modelTag = null;
            String techniqueName = null;
            String systemPromptFile = null;
            String userPromptFile = null;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, experimentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        transcriptPath   = rs.getString("file_path");
                        modelTag         = rs.getString("model_tag");
                        techniqueName    = rs.getString("technique_name");
                        systemPromptFile = rs.getString("system_prompt_file");
                        userPromptFile   = rs.getString("user_prompt_file");
                    } else {
                        throw new Exception("Experiment ID not found: " + experimentId);
                    }
                }
            }

            // Read files
            String transcript = "";
            System.out.println("Reading transcript from: " + resolvePath(transcriptPath).toAbsolutePath());
            try {
                transcript = new String(Files.readAllBytes(resolvePath(transcriptPath)));
            } catch (Exception e) {
                // If the file doesn't exist locally, fallback to a dummy text for testing
                transcript = "This is a dummy transcript since the file was not found: " + transcriptPath;
                System.out.println("Warning: Transcript file not found. Using dummy text.");
            }

            System.out.println("Reading system prompt : " + resolvePath(systemPromptFile).toAbsolutePath());
            System.out.println("Reading user prompt   : " + resolvePath(userPromptFile).toAbsolutePath());
            String systemPrompt       = new String(Files.readAllBytes(resolvePath(systemPromptFile)));
            String userPromptTemplate = new String(Files.readAllBytes(resolvePath(userPromptFile)));

            // Replace placeholder
            String userPrompt = userPromptTemplate.replace("{{TRANSCRIPT}}", transcript);

            // In LangChain4j, we only exposed a single string prompt in our basic LLMService.
            // A quick way is to combine system and user prompt.
            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;

            System.out.println("Running experiment " + experimentId + " on model " + modelTag);

            // Write assembled prompt to file for inspection before calling LLM
            try {
                java.nio.file.Path previewDir = resolvePath("prompt_previews");
                Files.createDirectories(previewDir);
                String safeLabel = (techniqueName != null) ? techniqueName.replaceAll("[^a-zA-Z0-9_-]", "_") : "unknown";
                String previewFileName = "prompt_previews/experiment_" + experimentId + "_" + safeLabel + ".txt";
                Files.write(resolvePath(previewFileName), combinedPrompt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("Prompt preview written to: " + resolvePath(previewFileName).toAbsolutePath());
            } catch (Exception pe) {
                System.out.println("Warning: Could not write prompt preview: " + pe.getMessage());
            }

            // Call LLM
            String jsonResult = llmService.prompt(modelTag, combinedPrompt);
            
            // Update experiment status to completed
            String updateQuery = "UPDATE experiment SET status = 'completed', executed_at = CURRENT_TIMESTAMP WHERE experiment_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setInt(1, experimentId);
                updateStmt.executeUpdate();
            }

            // Parse the JSON result and save it to nutrition_result + ingredient_result
            ResultSaver saver = new ResultSaver();
            int resultId = saver.saveResult(conn, experimentId, jsonResult);
            System.out.println("Saved nutrition_result with result_id = " + resultId);

            return jsonResult;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed: " + e.getMessage();
        }
    }

    private java.nio.file.Path resolvePath(String pathStr) {
        java.nio.file.Path p = Paths.get(pathStr);
        if (!Files.exists(p)) {
            // Try parent directory if running inside Eclipse submodule folder
            p = Paths.get("..", pathStr);
            if (!Files.exists(p)) {
                // Try one more level up
                p = Paths.get("..", "..", pathStr);
            }
        }
        return p;
    }
}
