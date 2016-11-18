package br.iesb.vismobile;

import java.util.Map;

/**
 * Created by dfcarvalho on 3/31/16.
 */
public class ChartData {
    private String name;
    private String description;
    private Long timestamp;
    private Map<Double, Double> data;

    public ChartData(String name, String description, Long timestamp, Map<Double, Double> data) {
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

    public Map<Double, Double> getData() {
        return data;
    }
}
