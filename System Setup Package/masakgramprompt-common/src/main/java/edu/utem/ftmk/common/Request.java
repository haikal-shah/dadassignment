package edu.utem.ftmk.common;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private String action;
    private Map<String, Object> data = new HashMap<>();
    
    public Request(String action) { this.action = action; }
    public String getAction() { return action; }
    public void addData(String key, Object value) { data.put(key, value); }
    public Object getData(String key) { return data.get(key); }
}
