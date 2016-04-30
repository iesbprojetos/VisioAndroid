package br.iesb.vismobile;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.iesb.vismobile.file.ChartCollection;
import br.iesb.vismobile.file.ChartData;
import br.iesb.vismobile.file.FileManager;
import br.iesb.vismobile.usb.DeviceConnectionListener;
import br.iesb.vismobile.usb.UsbConnection;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChartTabFragment extends Fragment implements DeviceConnectionListener {
    private LineChart chart;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private TextView txtCurrent;
    private TextView txtTotal;

    private UsbConnection usbConn;
    private FileManager fileManager;

    private int currentGraph;
    private int totalGraphs;

    private final ExecutorService convertExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler;

    public ChartTabFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ChartTabFragment.
     */
    public static ChartTabFragment newInstance() {
        return new ChartTabFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbConn = UsbConnection.getSingleton(getContext(), this);
        fileManager = FileManager.getSingleton(getContext());
        mainHandler = new Handler(getContext().getMainLooper());
    }

    @Override
    public void onDestroy() {
        usbConn.removeListener(this);
//        usbConn.release();
        usbConn = null;
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_chart_tab, container, false);

        btnPrevious = (ImageButton) view.findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (currentGraph <= 1) {
                            return;
                        }

                        currentGraph--;
                        Map<Integer, Double> mapData = fileManager.getCollection().getCharData(currentGraph-1).getData();
                        final double[] data = new double[mapData.values().size()];

                        for (int i = 0; i < mapData.values().size(); i++) {
                            data[i] = mapData.get(i);
                        }

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                drawGraph(data);
                                txtCurrent.setText(String.valueOf(currentGraph));
                            }
                        });
                    }
                });
            }
        });

        btnNext = (ImageButton) view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (currentGraph >= totalGraphs) {
                            return;
                        }

                        currentGraph++;
                        Map<Integer, Double> mapData = fileManager.getCollection().getCharData(currentGraph-1).getData();
                        final double[] data = new double[mapData.values().size()];

                        for (int i = 0; i < mapData.values().size(); i++) {
                            data[i] = mapData.get(i);
                        }

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                drawGraph(data);

                                txtCurrent.setText(String.valueOf(currentGraph));
                            }
                        });

                    }
                });

            }
        });
        txtCurrent = (TextView) view.findViewById(R.id.txtCurrentGraph);
        txtTotal = (TextView) view.findViewById(R.id.txtTotalGraph);

        chart = (LineChart) view.findViewById(R.id.chart);
        chart.setNoDataText("Aguardando dados...");
        chart.setScaleYEnabled(false);
        chart.setPinchZoom(true);

//        YAxis leftAxis = chart.getAxisLeft();
//        leftAxis.setAxisMinValue(-50);
//        leftAxis.setAxisMaxValue(50);
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        loadData(true);

        return view;
    }

    private void loadData(boolean drawLast) {
        ChartCollection collection = fileManager.getCollection();
        int size = collection.size();
        if (size > 0) {
            List<Entry> entries = new ArrayList<>();
            List<String> xVals = new ArrayList<>();

            ChartData data = collection.getCharData(size-1);
            Map<Integer, Double> values = data.getData();
            for (int x : values.keySet()) {
                xVals.add(String.valueOf(x));

                double value = values.get(x);

                Number num = value;
                float val = num.floatValue();
                Entry entry = new Entry(val, x);
                entries.add(entry);
            }

            LineDataSet dataSet = new LineDataSet(entries, "Dados");
            dataSet.setDrawCircleHole(false);
            dataSet.setDrawCircles(false);
            LineData chartData = new LineData(xVals, dataSet);
            chart.setData(chartData);

            totalGraphs = size;
            if (drawLast) {
                currentGraph = totalGraphs;
            } else {
                if (currentGraph < 1 || currentGraph > size) {
                    currentGraph = totalGraphs;
                }
            }

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    chart.notifyDataSetChanged();
                    chart.invalidate();

                    txtTotal.setText(String.valueOf(totalGraphs));
                    txtCurrent.setText(String.valueOf(currentGraph));
                }
            });
        }
    }

    public void onRedrawDraph(final boolean loadData) {
        convertExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (loadData) {
                    loadData(false);
                }

                Map<Integer, Double> mapData = fileManager.getCollection().getCharData(currentGraph-1).getData();
                final double[] data = new double[mapData.values().size()];

                for (int i = 0; i < mapData.values().size(); i++) {
                    data[i] = mapData.get(i);
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        drawGraph(data);

                        txtCurrent.setText(String.valueOf(currentGraph));
                    }
                });
            }
        });
    }

    private void drawGraph(final double[] data) {
        convertExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<Entry> entries = new ArrayList<>();
                final List<String> xVals = new ArrayList<>();

                for (int i = 0; i < data.length; i++) {
                    // x label
                    xVals.add(String.valueOf(i));

                    // entry (y, x)
                    Number num = fileManager.isMiliVolts() ? data[i] * 0.8056640625 : data[i];
                    float val = num.floatValue();
                    Entry entry = new Entry(val, i);
                    entries.add(entry);
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LineDataSet dataSet = new LineDataSet(entries, "Dados");
                        dataSet.setDrawCircleHole(false);
                        dataSet.setDrawCircles(false);
                        LineData chartData = new LineData(xVals, dataSet);
                        chart.setData(chartData);

                        chart.notifyDataSetChanged();
                        chart.invalidate();
                    }
                });
            }
        });
    }

    @Override
    public void onDeviceConnected() {

    }

    @Override
    public void onDeviceDisconnected() {

    }

    @Override
    public void onDevicePermissionDenied() {

    }

    @Override
    public void onDeviceClaimFailed() {

    }

    @Override
    public void onDeviceWriteOperationFailed() {

    }

    @Override
    public void onDeviceRead(double[] data) {
        if (currentGraph == totalGraphs) {
            drawGraph(data);
            txtCurrent.setText(String.valueOf(++currentGraph));
        }
        txtTotal.setText(String.valueOf(++totalGraphs));
    }

    @Override
    public void onDeviceStopReading() {
        // TODO:
    }

    @Override
    public void onDeviceReadOperationFailed() {

    }
}
