package br.iesb.vismobile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by dfcarvalho on 4/3/16.
 */
public class ChartCollection implements Iterable<ChartData> {
    private String name;
    private List<ChartData> charts;

    public ChartCollection() {
        this.charts = new ArrayList<>();
    }

    public ChartCollection(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addChartData(ChartData chartData) {
        charts.add(chartData);
    }

    public ChartData getCharData(int i) {
        return charts.get(i);
    }

    public int size() {
        return charts.size();
    }

    @Override
    public Iterator<ChartData> iterator() {
        return charts.iterator();
    }
}
