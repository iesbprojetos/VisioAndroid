package br.iesb.vismobile;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import br.iesb.vismobile.usb.DeviceConnectionListener;
import br.iesb.vismobile.usb.UsbConnection;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectionTabFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectionTabFragment extends Fragment implements DeviceConnectionListener {
    private RadioGroup radioGroupOperacao;
    private RadioButton radioContinuo, radioUnico, radiomV, radioCounts;
    private Button btnAdquirir;
    private Button btnParar;
    private Button btnConectar;
    private Spinner spinDispositivos;
    private UsbConnection usbConn;
    private List<UsbDevice> devices;

    public ConnectionTabFragment() {
        // Required empty public constructor
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
        final View view = inflater.inflate(R.layout.fragment_connection_tab, container, false);
        radioGroupOperacao = (RadioGroup) view.findViewById(R.id.rgroupModoOperacao);
        radioUnico = (RadioButton) view.findViewById(R.id.radioUnico);
        radioContinuo = (RadioButton) view.findViewById(R.id.radioContinuo);

        btnAdquirir = (Button) view.findViewById(R.id.btnAdquirir);
        btnAdquirir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = radioGroupOperacao.getCheckedRadioButtonId();
                switch (selectedId) {
                    case R.id.radioUnico:
                        usbConn.readOnce();
                        break;
                    case R.id.radioContinuo:
                        usbConn.startReadingData();
                        break;
                    default:
                        Snackbar.make(view, "Selecione um Modo de Operação", Snackbar.LENGTH_SHORT).show();
                        break;
                }
            }
        });

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
                    if (pos >= 0 && devices != null && devices.size() > pos) {
                        UsbDevice device = devices.get(pos);
                        usbConn.requestPermission(device);
                        btnConectar.setText("Conectando");
                        btnConectar.setEnabled(false);
                    } else {
                        Snackbar.make(view, "Nenhum dispositivo selecionado.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onDeviceConnected() {
        btnConectar.setText("Desconectar");
        btnConectar.setEnabled(true);
    }

    @Override
    public void onDeviceDisconnected() {
        btnConectar.setText("Conectar");
        btnConectar.setEnabled(true);
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
    }

    @Override
    public void onDeviceReadOperationFailed() {
        // TODO:
    }

    public List<String> getDeviceDescriptions() {
        devices = usbConn.getDeviceList();

        List<String> descriptions = new ArrayList<>();

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
}
