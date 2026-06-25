package edu.utem.ftmk.server;

import edu.utem.ftmk.network.Request;
import edu.utem.ftmk.network.Response;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ExperimentEngine experimentEngine = new ExperimentEngine();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in  = new ObjectInputStream(clientSocket.getInputStream())) {

            Request request = (Request) in.readObject();
            System.out.println("Received action: " + request.getAction());
            Response response = processRequest(request);
            out.writeObject(response);
            out.flush();

        } catch (Exception e) {
            System.err.println("ClientHandler Error: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (Exception ignored) {}
        }
    }

    private Response processRequest(Request request) {
        String action = request.getAction();
        try {
            switch (action) {
                case "GET_REELS":                return handleGetReels();
                case "GET_MODELS":               return handleGetModels();
                case "GET_TECHNIQUES":           return handleGetTechniques();
                case "GET_EXPERIMENTS":          return handleGetExperiments();
                case "GET_TRANSCRIPTS":          return handleGetTranscripts();
                case "CREATE_EXPERIMENT":        return handleCreateExperiment(request);
                case "RUN_EXPERIMENT":           return handleRunExperiment(request);
                case "GET_RESULT":               return handleGetResult(request);
                case "GET_INGREDIENTS":          return handleGetIngredients(request);
                case "GET_GROUND_TRUTH":         return handleGetGroundTruth(request);
                case "GET_GROUND_TRUTH_BY_REEL": return handleGetGroundTruthByReel(request);
                case "GET_TRANSCRIPT":           return handleGetTranscript(request);
                case "GET_REEL_STATUS":          return handleGetReelStatus(request);
                case "EXPORT_CSV":               return handleExportCsv(request);
                case "PREVIEW_CSV_DATA":         return handlePreviewCsvData(request);
                case "EXPORT_FACT_SHEET_CSV":    return handleExportFactSheetCsv(request);
                case "DETECT_HALLUCINATIONS":    return handleDetectHallucinations(request);
                default: return new Response(false, "Unknown action: " + action);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Server Error: " + e.getMessage());
        }
    }

    // GET_REELS: returns all reels with transcript status
    private Response handleGetReels() throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT r.reel_id, r.reel_id_instagram, i.instagram_account, " +
                       "       CASE WHEN t.transcript_id IS NULL THEN 'No Transcript' ELSE 'Ready' END AS transcript_status " +
                       "FROM reel r " +
                       "JOIN influencer i ON r.influencer_id = i.influencer_id " +
                       "LEFT JOIN transcript t ON r.reel_id = t.reel_id";
            try (PreparedStatement s = conn.prepareStatement(q); ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("reel_id",           rs.getString("reel_id"));
                    m.put("reel_id_instagram", rs.getString("reel_id_instagram"));
                    m.put("influencer",         rs.getString("instagram_account"));
                    m.put("status",             rs.getString("transcript_status"));
                    list.add(m);
                }
            }
        }
        Response r = new Response(true, "OK");
        r.addData("reels", list);
        return r;
    }

    // GET_MODELS: returns all LLM models
    private Response handleGetModels() throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT model_id, model_name, model_tag FROM llm_model";
            try (PreparedStatement s = conn.prepareStatement(q); ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("model_id",   rs.getString("model_id"));
                    m.put("model_name", rs.getString("model_name"));
                    m.put("model_tag",  rs.getString("model_tag"));
                    list.add(m);
                }
            }
        }
        Response r = new Response(true, "OK");
        r.addData("models", list);
        return r;
    }

    // GET_TECHNIQUES: returns all prompt techniques
    private Response handleGetTechniques() throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT technique_id, technique_name FROM prompt_technique";
            try (PreparedStatement s = conn.prepareStatement(q); ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("technique_id",   rs.getString("technique_id"));
                    m.put("technique_name", rs.getString("technique_name"));
                    list.add(m);
                }
            }
        }
        Response r = new Response(true, "OK");
        r.addData("techniques", list);
        return r;
    }

    // GET_EXPERIMENTS: returns all experiments with status
    private Response handleGetExperiments() throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT e.experiment_id, r.reel_id_instagram, m.model_name, " +
                       "       p.technique_name, e.status, e.executed_at " +
                       "FROM experiment e " +
                       "JOIN transcript t       ON e.transcript_id  = t.transcript_id " +
                       "JOIN reel r             ON t.reel_id         = r.reel_id " +
                       "JOIN llm_model m        ON e.model_id        = m.model_id " +
                       "JOIN prompt_technique p ON e.technique_id   = p.technique_id " +
                       "ORDER BY e.experiment_id DESC";
            try (PreparedStatement s = conn.prepareStatement(q); ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("experiment_id", rs.getString("experiment_id"));
                    m.put("reel",          rs.getString("reel_id_instagram"));
                    m.put("model",         rs.getString("model_name"));
                    m.put("technique",     rs.getString("technique_name"));
                    m.put("status",        rs.getString("status"));
                    m.put("executed_at",   rs.getString("executed_at"));
                    list.add(m);
                }
            }
        }
        Response r = new Response(true, "OK");
        r.addData("experiments", list);
        return r;
    }

    // CREATE_EXPERIMENT: creates a new experiment row
    private Response handleCreateExperiment(Request request) throws Exception {
        int transcriptId = Integer.parseInt((String) request.getData("transcriptId"));
        int modelId      = Integer.parseInt((String) request.getData("modelId"));
        int techniqueId  = Integer.parseInt((String) request.getData("techniqueId"));
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "INSERT INTO experiment (transcript_id, model_id, technique_id, rag_enabled, status) VALUES (?,?,?,0,'pending')";
            try (PreparedStatement s = conn.prepareStatement(q, PreparedStatement.RETURN_GENERATED_KEYS)) {
                s.setInt(1, transcriptId); s.setInt(2, modelId); s.setInt(3, techniqueId);
                s.executeUpdate();
                ResultSet keys = s.getGeneratedKeys();
                if (keys.next()) {
                    Response r = new Response(true, "Experiment created.");
                    r.addData("experimentId", String.valueOf(keys.getInt(1)));
                    return r;
                }
            }
        }
        return new Response(false, "Failed to create experiment.");
    }

    // RUN_EXPERIMENT: runs the LLM pipeline
    private Response handleRunExperiment(Request request) throws Exception {
        int experimentId = Integer.parseInt((String) request.getData("experimentId"));
        String jsonResult = experimentEngine.runExperiment(experimentId);
        Response r = new Response(true, "Experiment completed.");
        r.addData("jsonResult", jsonResult);
        return r;
    }

    // GET_RESULT: fetches the nutrition_result for an experiment
    private Response handleGetResult(Request request) throws Exception {
        int experimentId = Integer.parseInt((String) request.getData("experimentId"));
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT * FROM nutrition_result WHERE experiment_id = ?";
            try (PreparedStatement s = conn.prepareStatement(q)) {
                s.setInt(1, experimentId);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        Map<String, String> m = new LinkedHashMap<>();
                        ResultSetMetaData meta = rs.getMetaData();
                        for (int i = 1; i <= meta.getColumnCount(); i++) m.put(meta.getColumnName(i), rs.getString(i));
                        Response r = new Response(true, "OK");
                        r.addData("result", m);
                        return r;
                    }
                }
            }
        }
        return new Response(false, "No result found for experiment " + experimentId);
    }

    // GET_INGREDIENTS: fetches ingredient_result rows for a result_id
    private Response handleGetIngredients(Request request) throws Exception {
        int resultId = Integer.parseInt((String) request.getData("resultId"));
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT * FROM ingredient_result WHERE result_id = ?";
            try (PreparedStatement s = conn.prepareStatement(q)) {
                s.setInt(1, resultId);
                try (ResultSet rs = s.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, String> m = new LinkedHashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) m.put(meta.getColumnName(i), rs.getString(i));
                        list.add(m);
                    }
                }
            }
        }
        Response r = new Response(true, "OK");
        r.addData("ingredients", list);
        return r;
    }

    // GET_GROUND_TRUTH: fetches ground truth ingredients for the reel linked to an experiment
    private Response handleGetGroundTruth(Request request) throws Exception {
        int experimentId = Integer.parseInt((String) request.getData("experimentId"));
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q =
                "SELECT gti.gt_ingredient_id, gti.name_original, gti.name_en, " +
                "       gti.quantity_value_culinary, gti.quantity_unit_culinary, gti.estimated_weight_g, " +
                "       gti.calories, gti.protein_g, gti.total_fat_g, gti.total_carbohydrate_g, " +
                "       gti.annotation_layer " +
                "FROM experiment e " +
                "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE e.experiment_id = ? " +
                "ORDER BY gti.gt_ingredient_id";
            try (PreparedStatement s = conn.prepareStatement(q)) {
                s.setInt(1, experimentId);
                try (ResultSet rs = s.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, String> m = new LinkedHashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) m.put(meta.getColumnName(i), rs.getString(i));
                        list.add(m);
                    }
                }
            }
        }
        Response r = new Response(true, "OK");
        r.addData("groundTruth", list);
        return r;
    }

    // GET_GROUND_TRUTH_BY_REEL: fetches ground truth ingredients by reel ID
    private Response handleGetGroundTruthByReel(Request request) throws Exception {
        int reelId = Integer.parseInt((String) request.getData("reelId"));
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q =
                "SELECT gti.name_original, gti.name_en " +
                "FROM transcript t " +
                "JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE t.reel_id = ?";
            try (PreparedStatement s = conn.prepareStatement(q)) {
                s.setInt(1, reelId);
                try (ResultSet rs = s.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("name_original", rs.getString("name_original"));
                        m.put("name_en", rs.getString("name_en"));
                        list.add(m);
                    }
                }
            }
        }
        Response r = new Response(true, "OK");
        r.addData("groundTruth", list);
        return r;
    }

    // -------------------------------------------------------
    // GET_TRANSCRIPT: reads the transcript text file for a reel
    // -------------------------------------------------------
    private Response handleGetTranscript(Request request) throws Exception {
        int reelId = Integer.parseInt((String) request.getData("reelId"));
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT t.file_path FROM transcript t WHERE t.reel_id = ? LIMIT 1";
            try (PreparedStatement s = conn.prepareStatement(q)) {
                s.setInt(1, reelId);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        String path = rs.getString("file_path");
                        java.nio.file.Path p = java.nio.file.Paths.get(path);
                        if (!java.nio.file.Files.exists(p)) {
                            p = java.nio.file.Paths.get("..", path);
                            if (!java.nio.file.Files.exists(p)) {
                                p = java.nio.file.Paths.get("..", "..", path);
                            }
                        }
                        String text = new String(java.nio.file.Files.readAllBytes(p));
                        Response resp = new Response(true, "OK");
                        resp.addData("transcriptText", text);
                        return resp;
                    }
                }
            }
        }
        return new Response(false, "No transcript found for reel " + reelId);
    }

    // -------------------------------------------------------
    // GET_REEL_STATUS: returns model x technique status for a reel
    // -------------------------------------------------------
    private Response handleGetReelStatus(Request request) throws Exception {
        int reelId = Integer.parseInt((String) request.getData("reelId"));
        List<Map<String, String>> statusList = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT m.model_name, pt.technique_name, e.status " +
                       "FROM experiment e " +
                       "JOIN transcript t ON e.transcript_id = t.transcript_id " +
                       "JOIN llm_model m ON e.model_id = m.model_id " +
                       "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                       "WHERE t.reel_id = ?";
            try (PreparedStatement s = conn.prepareStatement(q)) {
                s.setInt(1, reelId);
                try (ResultSet rs = s.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("model",     rs.getString("model_name"));
                        m.put("technique", rs.getString("technique_name"));
                        m.put("status",    rs.getString("status"));
                        statusList.add(m);
                    }
                }
            }
        }
        Response resp = new Response(true, "OK");
        resp.addData("statusList", statusList);
        return resp;
    }

    // -------------------------------------------------------
    // EXPORT_CSV: runs a named metric query and writes CSV
    // -------------------------------------------------------
    private Response handleExportCsv(Request request) throws Exception {
        String queryLabel  = (String) request.getData("queryLabel");
        String outputFolder = (String) request.getData("outputFolder");
        ExportEngine engine = new ExportEngine();
        String filePath = engine.exportCsv(queryLabel, outputFolder);
        Response resp = new Response(true, "CSV exported successfully.");
        resp.addData("filePath", filePath);
        return resp;
    }

    // -------------------------------------------------------
    // PREVIEW_CSV_DATA: runs a named metric query and returns data
    // -------------------------------------------------------
    private Response handlePreviewCsvData(Request request) throws Exception {
        String queryLabel  = (String) request.getData("queryLabel");
        ExportEngine engine = new ExportEngine();
        Map<String, Object> data = engine.previewData(queryLabel);
        Response resp = new Response(true, "OK");
        resp.addData("preview", data);
        return resp;
    }

    // -------------------------------------------------------
    // EXPORT_FACT_SHEET_CSV: exports ingredients for one experiment
    // -------------------------------------------------------
    private Response handleExportFactSheetCsv(Request request) throws Exception {
        int experimentId = Integer.parseInt((String) request.getData("experimentId"));
        String outputFolder = (String) request.getData("outputFolder");
        java.io.File dir = new java.io.File(outputFolder);
        dir.mkdirs();
        java.io.File file = new java.io.File(dir, "fact_sheet_experiment_" + experimentId + ".csv");

        try (Connection conn = DatabaseManager.getConnection();
             java.io.PrintWriter pw = new java.io.PrintWriter(
                 new java.io.OutputStreamWriter(
                     new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {

            pw.println("name_original,name_en,quantity_value,unit_original,unit_en," +
                       "estimated_weight_g,calories,total_fat_g,protein_g,total_carbohydrate_g,is_hallucinated");

            String q = "SELECT ir.* FROM ingredient_result ir " +
                       "JOIN nutrition_result nr ON ir.result_id = nr.result_id " +
                       "WHERE nr.experiment_id = ?";
            try (PreparedStatement s = conn.prepareStatement(q)) {
                s.setInt(1, experimentId);
                try (ResultSet rs = s.executeQuery()) {
                    while (rs.next()) {
                        pw.println(
                            csv(rs.getString("name_original")) + "," +
                            csv(rs.getString("name_en")) + "," +
                            csv(rs.getString("quantity_value")) + "," +
                            csv(rs.getString("unit_original")) + "," +
                            csv(rs.getString("unit_en")) + "," +
                            csv(rs.getString("estimated_weight_g")) + "," +
                            csv(rs.getString("calories")) + "," +
                            csv(rs.getString("total_fat_g")) + "," +
                            csv(rs.getString("protein_g")) + "," +
                            csv(rs.getString("total_carbohydrate_g")) + "," +
                            csv(rs.getString("is_hallucinated")));
                    }
                }
            }
        }
        Response resp = new Response(true, "Fact sheet CSV exported.");
        resp.addData("filePath", file.getAbsolutePath());
        return resp;
    }

    /** Escapes a value for CSV: wraps in quotes if it contains commas or quotes. */
    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"")) return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    // -------------------------------------------------------
    // GET_TRANSCRIPTS: returns all transcript rows from the DB
    // -------------------------------------------------------
    private Response handleGetTranscripts() throws Exception {
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            String q = "SELECT t.transcript_id, t.reel_id, t.video_id, t.file_path, " +
                       "       r.reel_id_instagram, i.instagram_account " +
                       "FROM transcript t " +
                       "JOIN reel r ON t.reel_id = r.reel_id " +
                       "JOIN influencer i ON r.influencer_id = i.influencer_id " +
                       "ORDER BY t.transcript_id";
            try (PreparedStatement s = conn.prepareStatement(q); ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("transcript_id",      rs.getString("transcript_id"));
                    m.put("reel_id",            rs.getString("reel_id"));
                    m.put("video_id",           rs.getString("video_id"));
                    m.put("reel_id_instagram",  rs.getString("reel_id_instagram"));
                    m.put("influencer",         rs.getString("instagram_account"));
                    list.add(m);
                }
            }
        }
        Response r = new Response(true, "OK");
        r.addData("transcripts", list);
        return r;
    }

    // -------------------------------------------------------
    // DETECT_HALLUCINATIONS: runs LLM judge on all completed
    // experiments that have un-judged ingredients (is_hallucinated IS NULL).
    // Each experiment is judged by the same model that ran it.
    // -------------------------------------------------------
    private Response handleDetectHallucinations(Request request) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            HallucinationJudge judge = new HallucinationJudge();
            int count = judge.judgeAll(conn);
            Response r = new Response(true, "Hallucination detection complete.");
            r.addData("ingredientsJudged", String.valueOf(count));
            return r;
        }
    }
}
