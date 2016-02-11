package br.iesb.vismobile.usb;

import android.hardware.usb.UsbDevice;

/**
 * Interface DeviceConnectionListener
 * Deve ser implementada por classes que precisam receber notificações sobre a comunicação com o
 * espectofotometro
 * Created by dfcarvalho on 11/12/15.
 */
public interface DeviceConnectionListener {
    void onDevicePermissionGranted(UsbDevice device);
    void onDevicePermissionDenied();
    void onDeviceConnected(UsbDevice device);
    void onDeviceWriteOperationFailed();
    void onDeviceReadOperationFailed();
    void onDeviceShowInfo(UsbDevice device);
}
