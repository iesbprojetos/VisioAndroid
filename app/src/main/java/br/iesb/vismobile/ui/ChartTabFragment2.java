package br.iesb.vismobile.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.iesb.vismobile.ChartCollection;
import br.iesb.vismobile.ChartData;
import br.iesb.vismobile.R;
import br.iesb.vismobile.file.FileManager;
import br.iesb.vismobile.usb.DeviceConnectionListener;
import br.iesb.vismobile.usb.UsbConnection;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChartTabFragment2 extends Fragment implements DeviceConnectionListener {
    private static final String ARG_FROM_DEVICE = "FROM_DEVICE";

    private LineChartView chart;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private TextView txtCurrent;
    private TextView txtTotal;

    private UsbConnection usbConn;
    private FileManager fileManager;

    private int currentGraph;
    private int totalGraphs;

    private boolean fromDevice;

    private final ExecutorService convertExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler;

    public ChartTabFragment2() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ChartTabFragment.
     */
    public static ChartTabFragment2 newInstance(boolean fromDevice) {
        ChartTabFragment2 fragment = new ChartTabFragment2();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FROM_DEVICE, fromDevice);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileManager = FileManager.getSingleton(getContext());

        DeviceConnectionListener listener = null;
        Bundle args = getArguments();
        if (args != null) {
            fromDevice = args.getBoolean(ARG_FROM_DEVICE);
            if (fromDevice) {
                listener = this;
            }
        }
        usbConn = UsbConnection.getSingleton(getContext(), listener);

        mainHandler = new Handler(getContext().getMainLooper());
    }

    @Override
    public void onDestroy() {
        usbConn.removeListener(this);
        usbConn = null;
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_chart_tab2, container, false);

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
                        ChartCollection collection = fromDevice ? fileManager.getCollection() : fileManager.getPcaCollection();
                        final Map<Double, Double> mapData = collection.getCharData(currentGraph-1).getData();

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                drawGraph(mapData);
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
                        ChartCollection collection = fromDevice ? fileManager.getCollection() : fileManager.getPcaCollection();
                        final Map<Double, Double> mapData = collection.getCharData(currentGraph-1).getData();

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                drawGraph(mapData);

                                txtCurrent.setText(String.valueOf(currentGraph));
                            }
                        });

                    }
                });

            }
        });
        txtCurrent = (TextView) view.findViewById(R.id.txtCurrentGraph);
        txtTotal = (TextView) view.findViewById(R.id.txtTotalGraph);

        chart = (LineChartView) view.findViewById(R.id.chart);
        chart.setInteractive(true);
        chart.setZoomType(ZoomType.HORIZONTAL_AND_VERTICAL);

        // TODO: make it invisible then visible when there's data
        chart.setVisibility(View.INVISIBLE);

//        if (fromDevice) {
//            YAxis leftAxis = chart.getAxisLeft();
//            leftAxis.setAxisMinValue(0);
//            leftAxis.setAxisMaxValue(100);
//        }
//        YAxis rightAxis = chart.getAxisRight();
//        rightAxis.setEnabled(false);

//        XAxis xAxis = chart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        loadData(true);

        return view;
    }

    private void loadData(boolean drawLast) {
        ChartCollection collection = fromDevice ? fileManager.getCollection() : fileManager.getPcaCollection();
        if (collection != null) {
            int size = collection.size();
            if (size > 0) {
                ChartData data = collection.getCharData(size - 1);
                Map<Double, Double> mapData = data.getData();
                final LineChartData chartData = mapDataToChartData(mapData);;

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
                        chart.setLineChartData(chartData);

                        txtTotal.setText(String.valueOf(totalGraphs));
                        txtCurrent.setText(String.valueOf(currentGraph));

                        chart.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }

    public void onRedrawGraph(final boolean loadData) {
        convertExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (loadData) {
                    loadData(false);
                }

                ChartCollection collection = fromDevice ? fileManager.getCollection() : fileManager.getPcaCollection();
                Map<Double, Double> mapData = null;
                if (currentGraph-1 < collection.size()) {
                    mapData = collection.getCharData(currentGraph - 1).getData();
                } else {
                    mapData = new HashMap<>();
                }

                final Map<Double, Double> finalMapData = mapData;

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        drawGraph(finalMapData);

                        txtCurrent.setText(String.valueOf(currentGraph));
                    }
                });
            }
        });
    }

    private void drawGraph(final Map<Double, Double> data) {
        convertExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final LineChartData chartData = mapDataToChartData(data);

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        chart.setLineChartData(chartData);
                        chart.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private LineChartData mapDataToChartData(Map<Double, Double> mapData) {
        List<PointValue> chartValues = new ArrayList<>();
//        List<AxisValue> xAxisValues = new ArrayList<>();

        List<Double> keyList = new ArrayList<>();
        keyList.addAll(mapData.keySet());
        Collections.sort(keyList);

        for (Double doubleX : keyList) {
            float x = doubleX.floatValue();
//            AxisValue xValue = new AxisValue(x);
//            xAxisValues.add(xValue);

            Number num = mapData.get(doubleX);
            float y = num.floatValue();
            PointValue v = new PointValue(x, y);
            chartValues.add(v);
        }

        Line lineData = new Line(chartValues).setColor(Color.RED).setStrokeWidth(1).setPointRadius(2);
        lineData.setHasPoints(!fromDevice);
        lineData.setHasLines(fromDevice);
        List<Line> lineDataSet = new ArrayList<>();
        lineDataSet.add(lineData);

//        Axis xAxis = new Axis(xAxisValues);
        Axis xAxis = new Axis();
        Axis yAxis = new Axis();
        LineChartData chartData = new LineChartData(lineDataSet);
        chartData.setAxisXBottom(xAxis);
        chartData.setAxisYLeft(yAxis);

        return chartData;
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
            Map<Double, Double> mapData = new TreeMap<>();
            for (int i = 0; i < data.length; i++) {
                mapData.put((double)i, data[i]);
            }
            drawGraph(mapData);
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
