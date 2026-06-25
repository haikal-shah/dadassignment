package edu.utem.ftmk.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReparseDB {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection()) {
            String sel = "SELECT result_id, experiment_id, raw_json_output FROM nutrition_result";
            PreparedStatement ps = conn.prepareStatement(sel);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int rid = rs.getInt("result_id");
                int eid = rs.getInt("experiment_id");
                String raw = rs.getString("raw_json_output");
                
                System.out.println("Deleting old result for experiment " + eid);
                conn.createStatement().executeUpdate("DELETE FROM ingredient_result WHERE result_id = " + rid);
                conn.createStatement().executeUpdate("DELETE FROM nutrition_result WHERE result_id = " + rid);
                
                System.out.println("Reparsing raw JSON...");
                ResultSaver saver = new ResultSaver();
                int newRid = saver.saveResult(conn, eid, raw);
                System.out.println("Saved new result ID: " + newRid);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
