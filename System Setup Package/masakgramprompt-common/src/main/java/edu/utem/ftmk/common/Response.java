package edu.utem.ftmk.common;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Map<String, Object> data = new HashMap<>();

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public void addData(String key, Object value) { data.put(key, value); }
    public Object getData(String key) { return data.get(key); }
}
