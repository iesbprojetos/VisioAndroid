package br.iesb.vismobile.ui;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.iesb.vismobile.BuildConfig;
import br.iesb.vismobile.R;
import br.iesb.vismobile.ChartData;
import br.iesb.vismobile.file.FileManager;
import br.iesb.vismobile.usb.DeviceConnectionListener;
import br.iesb.vismobile.usb.UsbConnection;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectionTabFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectionTabFragment extends Fragment implements DeviceConnectionListener {
    private View view;

    private RadioGroup radioGroupOperacao, radioGroupUnidade;
    private RadioButton radioContinuo, radioUnico, radioUndCounts, radioUndMV;
    private Button btnAdquirir;
    private Button btnParar;
    private Button btnConectar;
    private Spinner spinDispositivos;
    private SeekBar seekNumAmostras;
    private TextView txtNumAmostras;
    private SeekBar seekTempoIntegra;
    private TextView txtTempoIntegra;
    private EditText editFaixaEspectralDe, editFaixaEspectralA;
    private UsbConnection usbConn;
    private List<UsbDevice> devices;

    private FileManager fileManager;
    private int chartCount;

    private OnFragmentInteractionListener listener;

    public ConnectionTabFragment() {
        // Required empty public constructor
        fileManager = FileManager.getSingleton(getContext());
        chartCount = 0;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ConnectionTabFragment.
     */
    public static ConnectionTabFragment newInstance() {
        return new ConnectionTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbConn = UsbConnection.getSingleton(getContext(), this);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
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
        if (view == null) {
            createView(inflater, container, savedInstanceState);
        }

        return view;
    }

    private void createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_connection_tab, container, false);
        radioGroupOperacao = (RadioGroup) view.findViewById(R.id.rgroupModoOperacao);
        radioUnico = (RadioButton) view.findViewById(R.id.radioUnico);
        radioUnico.setChecked(true);
        radioContinuo = (RadioButton) view.findViewById(R.id.radioContinuo);

        radioGroupUnidade = (RadioGroup) view.findViewById(R.id.rgroupUnidade);
        radioUndCounts = (RadioButton) view.findViewById(R.id.radioUndCounts);
        radioUndCounts.setChecked(true);
        radioUndMV = (RadioButton) view.findViewById(R.id.radioUndMV);
        radioGroupUnidade.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioUndCounts:
                        fileManager.setMiliVolts(false);
                        break;
                    case R.id.radioUndMV:
                        fileManager.setMiliVolts(true);
                        break;
                }

                // redesenhar grafico
                listener.onUnitChanged();

            }
        });

        btnAdquirir = (Button) view.findViewById(R.id.btnAdquirir);
        btnAdquirir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkedId = radioGroupOperacao.getCheckedRadioButtonId();
                switch (checkedId) {
                    case R.id.radioUnico:
                        usbConn.readUnique();
                        break;
                    case R.id.radioContinuo:
                        usbConn.startReadingData();
                        btnAdquirir.setEnabled(false);
                        btnParar.setEnabled(true);
                        break;
                    default:
                        Snackbar.make(view, "Selecione um Modo de Operação", Snackbar.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        btnAdquirir.setEnabled(false);

        btnParar = (Button) view.findViewById(R.id.btnParar);
        btnParar.setEnabled(false);
        btnParar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usbConn.stopReadingData();
            }
        });

        spinDispositivos = (Spinner) view.findViewById(R.id.spinDispositivo);
        final List<String> deviceDescriptions = getDeviceDescriptions();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_dispositivo, deviceDescriptions);
        spinDispositivos.setAdapter(adapter);

        btnConectar = (Button) view.findViewById(R.id.btnConectar);
        btnConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbConn.isConnected()) {
                    usbConn.disconnect();
                } else {
                    int pos = spinDispositivos.getSelectedItemPosition();
                    if (BuildConfig.MOCK_DEVICE) {
                        if (pos == 0) {
                            // mock device
                            usbConn.setConnectedToMockDevice(true);
                            onDeviceConnected();
                            return;
                        }
                        pos--;
                    }

                    try {
                        UsbDevice device = devices.get(pos);
                        usbConn.requestPermission(device);
                        btnConectar.setText("Conectando");
                        btnConectar.setEnabled(false);
                    } catch (IndexOutOfBoundsException | NullPointerException e) {
                        Snackbar.make(view, "Nenhum dispositivo selecionado.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });
        if (usbConn.isConnected()) {
            onDeviceConnected();
        } else {
            onDeviceDisconnected();
        }

        txtNumAmostras = (TextView) view.findViewById(R.id.txtNumAmostras);
        txtNumAmostras.setText(String.valueOf(usbConn.getSampleSize()));

        seekNumAmostras = (SeekBar) view.findViewById(R.id.seekNumAmostras);
        seekNumAmostras.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    progress = 1;
                    seekBar.setProgress(progress);
                }
                txtNumAmostras.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                usbConn.setSampleSize(seekBar.getProgress());
            }
        });
        seekNumAmostras.setProgress(usbConn.getSampleSize());

//        txtTempoIntegra = (TextView) view.findViewById(R.id.txtTempoIntegra);
//        // TODO:
//        txtTempoIntegra.setText(String.valueOf(2048));
//
//        seekTempoIntegra = (SeekBar) view.findViewById(R.id.seekTempoIntegra);
//        seekTempoIntegra.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (progress == 0) {
//                    progress = 1;
//                    seekBar.setProgress(progress);
//                }
//                txtTempoIntegra.setText(String.valueOf(progress));
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                // TODO:
////                usbConn.setSampleSize(seekBar.getProgress());
//            }
//        });
//
//        // TODO:
//        seekTempoIntegra.setProgress(2048);
//
//        // TODO:
//        editFaixaEspectralDe = (EditText) view.findViewById(R.id.editFaixaEspectralDe);
//        editFaixaEspectralA = (EditText) view.findViewById(R.id.editFaixaEspectralA);
    }

    @Override
    public void onDeviceConnected() {
        btnConectar.setText("Desconectar");
        btnConectar.setEnabled(true);
        btnAdquirir.setEnabled(true);
    }

    @Override
    public void onDeviceDisconnected() {
        btnConectar.setText("Conectar");
        btnConectar.setEnabled(true);
        btnAdquirir.setEnabled(false);
    }

    @Override
    public void onDevicePermissionDenied() {
        btnConectar.setText("Conectar");
        btnConectar.setEnabled(true);
    }

    @Override
    public void onDeviceClaimFailed() {
        // TODO:
    }

    @Override
    public void onDeviceWriteOperationFailed() {
        // TODO:
    }

    @Override
    public void onDeviceRead(double[] data) {
        Log.d("Visio", "Bytes read: " + data);

        Map<Double, Double> chartDataMap = new TreeMap<>();
        for (int i = 0; i < data.length; i++) {
            chartDataMap.put((double)i, data[i]);
        }

        ChartData chartData = new ChartData(
                String.format("Chart_%d", chartCount++),
                "",
                new Date().getTime(),
                chartDataMap
        );

        fileManager.getCollection().addChartData(chartData);
    }

    @Override
    public void onDeviceStopReading() {
        btnAdquirir.setEnabled(true);
        btnParar.setEnabled(false);
    }

    @Override
    public void onDeviceReadOperationFailed() {
        onDeviceStopReading();
        // TODO:
    }

    public List<String> getDeviceDescriptions() {
        devices = usbConn.getDeviceList();

        List<String> descriptions = new ArrayList<>();

        if (BuildConfig.MOCK_DEVICE) {
            descriptions.add("MOCK_DEVICE");
        }

        for (UsbDevice device : devices) {
            String description;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                description = String.format("%s: %s - %s", device.getManufacturerName(), device.getProductName(), device.getDeviceName());
            } else {
                description = device.getDeviceName();
            }

            descriptions.add(description);
        }

        return descriptions;
    }

    public interface OnFragmentInteractionListener {
        void onUnitChanged();
    }
}
