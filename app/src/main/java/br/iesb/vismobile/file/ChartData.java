package br.iesb.vismobile.file;

import java.util.Map;

/**
 * Created by dfcarvalho on 3/31/16.
 */
public class ChartData {
    private String name;
    private String description;
    private Long timestamp;
    private Map<Integer, Double> data;

    public ChartData(String name, String description, Long timestamp, Map<Integer, Double> data) {
        this.name = name;
        this.description = description;
        this.timestamp = timestamp;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Map<Integer, Double> getData() {
        return data;
    }
}
