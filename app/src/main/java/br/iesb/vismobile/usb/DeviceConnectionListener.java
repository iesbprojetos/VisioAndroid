package br.iesb.vismobile.usb;

/**
 * Interface DeviceConnectionListener
 * Deve ser implementada por classes que precisam receber notificações sobre a comunicação com o
 * espectofotometro
 * Created by dfcarvalho on 11/12/15.
 */
public interface DeviceConnectionListener {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onDevicePermissionDenied();
    void onDeviceClaimFailed();
    void onDeviceWriteOperationFailed();
    void onDeviceRead(double[] data);
    void onDeviceStopReading();
    void onDeviceReadOperationFailed();
}
