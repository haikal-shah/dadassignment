package edu.utem.ftmk.server;

import java.sql.Connection;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RedoSystem {
    public static void main(String[] args) {
        try {
            System.out.println("Connecting to DB...");
            Connection conn = DatabaseManager.getConnection();
            System.out.println("Connected!");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
