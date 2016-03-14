package br.iesb.vismobile;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

import br.iesb.vismobile.usb.DeviceConnectionListener;
import br.iesb.vismobile.usb.UsbConnection;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChartTabFragment extends Fragment implements DeviceConnectionListener {
    private LineChart chart;

    private UsbConnection usbConn;

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
        final View view = inflater.inflate(R.layout.fragment_chart_tab, container, false);

        chart = (LineChart) view.findViewById(R.id.chart);
        chart.setNoDataText("Aguardando dados...");

//        LineDataSet dataSet = new LineDataSet(new ArrayList<Entry>(), "Dados");
//        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
//        List<String> xVals = new ArrayList<>(256);
//        for (int i = 0; i < 256; i++) {
//            xVals.add(String.valueOf(i));
//        }
//        LineData chartData = new LineData(xVals);
//
//        chartData.addDataSet(dataSet);
//        chart.setData(chartData);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinValue(-50);
        leftAxis.setAxisMaxValue(50);

        return view;
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
        List<Entry> entries = new ArrayList<>();
        List<String> xVals = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            // x label
            xVals.add(String.valueOf(i));

            // entry (y, x)
            Number num = data[i];
            float val = num.floatValue();
            Entry entry = new Entry(val, i);
            entries.add(entry);
        }


        LineDataSet dataSet = new LineDataSet(entries, "Dados");
        LineData chartData = new LineData(xVals, dataSet);
        chart.setData(chartData);

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    @Override
    public void onDeviceReadOperationFailed() {

    }
}
