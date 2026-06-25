package edu.utem.ftmk.server;

public class TestResultSaver {
    public static void main(String[] args) {
        String rawJson = "Here is the complete nutrition facts sheet in JSON format:\n\n```\n{\n  \"recipe_name\": \"Biskut Coklat Urai\" \n}\n```";
        String json = rawJson.trim();
        int firstBrace = json.indexOf('{');
        int lastBrace = json.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace >= 0 && lastBrace > firstBrace) {
            json = json.substring(firstBrace, lastBrace + 1);
        }

        boolean jsonValid = json.startsWith("{") && json.endsWith("}");
        System.out.println("Extracted json: " + json);
        System.out.println("jsonValid: " + jsonValid);
    }
}
