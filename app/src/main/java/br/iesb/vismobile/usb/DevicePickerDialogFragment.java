package br.iesb.vismobile.usb;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.iesb.vismobile.BuildConfig;
import br.iesb.vismobile.R;

/**
 * Classe DevicePickerDialogFragment (UI)
 * Uma caixa de diálogo que lista os dispositivos USB disponíveis para que o usuário selecione
 * o espectofometro
 * Created by dfcarvalho on 11/12/15.
 */
public class DevicePickerDialogFragment extends DialogFragment {

    private List<UsbDevice> devices;
    private UsbConnection usbConnection;

    /**
     * Construtor
     * Usado somente pelo S.O.
     */
    public DevicePickerDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Obter uma nova instância da classe DevicePickerDialogFragment para ser apresentada ao usuário
     * @return Instância de DevicePickerDialogFragment
     */
    public static DevicePickerDialogFragment newInstance() {
        DevicePickerDialogFragment fragment = new DevicePickerDialogFragment();
        // TODO: args?
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        // TODO: o que fazer com o listener?
        usbConnection = UsbConnection.getSingleton(activity.getApplicationContext(), null);

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.fragment_device_picker_dialog, null);
        ListView listViewDevices = (ListView) dialogView.findViewById(R.id.listViewDevices);
        devices = usbConnection.getDeviceList();
        final List<String> deviceDescriptions = getDeviceDescriptions();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceDescriptions);

        listViewDevices.setAdapter(adapter);
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                UsbDevice device = devices.get(position);
                usbConnection.requestPermission(device);
                dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView)
                // TODO: create string resource
                .setTitle("Escolha o dispositivo:")
                // TODO: create string resource
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });

        return builder.create();
    }

    @Override
    public void onDestroyView() {
//        usbConnection.release();
        usbConnection = null;

        super.onDestroyView();
    }

    public List<String> getDeviceDescriptions() {
        List<String> descriptions = new ArrayList<>();

        for (UsbDevice device : devices) {
            String description;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                description = String.format("%s - %s\n%s", device.getManufacturerName(), device.getProductName(), device.getDeviceName());
            } else {
                description = device.getDeviceName();
            }

            descriptions.add(description);
        }

        return descriptions;
    }
}
