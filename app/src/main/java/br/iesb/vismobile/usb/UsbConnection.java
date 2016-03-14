package br.iesb.vismobile.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Classe UsbConnection (Singleton)
 * Created by dfcarvalho on 11/12/15.
 * Controla a comunicação entre o dispositivo e o Visio (espectofotometro)
 * Obter instância com o método: getSingleton()
 */
public class UsbConnection {
    private static UsbConnection SINGLETON = null;
    private static final String ACTION_USB_PERMISSION = "br.iesb.vismobile.USB_PERMISSION";

    private Context context;
    private Set<DeviceConnectionListener> listeners;
    private UsbManager usbManager;
    private PendingIntent permissionIntent;
    private UsbDevice selectedDevice;
    private UsbDeviceConnection usbConn;
    private UsbSerialDevice serialDevice;
    private boolean reading;
    private int sampleSize;
    private byte[] buffer;
    private int currentPos;

    private final Object readLock = new Object();
    private final ExecutorService commExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler;

    /**
     * Obter instância singleton da classe UsbConnection. Uma nova instância é criada, caso já não exista.
     * @param context Android Context (de preferência Application Context)
     * @param listener Objeto que irá receber notificações sobre a conexão com o Visio
     * @return Instância singleton da classe UsbConnection
     */
    public static synchronized UsbConnection getSingleton(Context context, DeviceConnectionListener listener) {
        if (SINGLETON == null) {
            SINGLETON = new UsbConnection(context);
        }

        SINGLETON.addListener(listener);

        return SINGLETON;
    }

    /**
     * Construtor privado (Singleton Pattern)
     * @param context Android Context (de preferência Application Context)
     */
    private UsbConnection(Context context) {
        this.context = context;
        this.listeners = new HashSet<>();
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        context.registerReceiver(usbPermissionReceiver, filter);
        mainHandler = new Handler(context.getMainLooper());
        sampleSize = 2048;
        buffer = new byte[2048];
        currentPos = 0;
    }

    /**
     * Obter lista de dispositivos USB disponíveis
     * @return ArrayList com os UsbDevices disponíveis (pode estar vazia)
     */
    public List<UsbDevice> getDeviceList() {
        ArrayList<UsbDevice> deviceList = new ArrayList<>();

        HashMap<String, UsbDevice> deviceHashMap = usbManager.getDeviceList();
        for (UsbDevice usbDevice : deviceHashMap.values()) {
            deviceList.add(usbDevice);
        }

        return deviceList;
    }

    /**
     * Solicita do sistema permissão para acessar um dispositivo USB
     * @param device Dispositivo USB que deseja acessar
     */
    public void requestPermission(UsbDevice device) {
        usbManager.requestPermission(device, permissionIntent);
    }

    public boolean isConnected() {
        return usbConn != null;
    }

    public void disconnect() {
        stopReadingData();

        serialDevice.close();
        serialDevice = null;
        usbConn.close();
        usbConn = null;
        selectedDevice = null;

        for (DeviceConnectionListener listener : listeners) {
            listener.onDeviceDisconnected();
        }
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    /**
     * Inicia leitura de dados do dispositivo conectado.
     * Dados lidos são repassados para os listeners através do método onDeviceRead()
     */
    public void startReadingData() {
        if (!reading) {
            reading = true;

            commExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        synchronized (readLock) {
                            if (!reading) {
                                break;
                            }
                        }

                        serialDevice.write("+".getBytes());
                        serialDevice.read(mReadCallback);

                        try {
                            // TODO: alterar tempo de espera de acordo com o slider
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            // TODO:
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    public void readOnce() {
        commExecutor.submit(new Runnable() {
            @Override
            public void run() {
                serialDevice.write("+".getBytes());
                serialDevice.read(mReadCallback);
            }
        });
    }

    public void stopReadingData() {
        synchronized (readLock) {
            reading = false;
        }
    }

    public Set<DeviceConnectionListener> getListeners() {
        return listeners;
    }

    private void addListener(DeviceConnectionListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(DeviceConnectionListener listener) {
        if (listener != null) {
            this.listeners.remove(listener);
        }
    }

    /**
     * Classe interna usada para receber Intents com permissão para conectar a um dispositivo USB
     * após a função requestPermission(UsbDevice) ser chamada
     */
    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    // DEBUG
                    Toast.makeText(context, "BROADCAST RECEIVED", Toast.LENGTH_SHORT).show();
                    // Dispositivo USB
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    // Acesso autorizado pelo usuário?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        // Sim, obter interface de comunicação
                        if (device != null) {
                            selectedDevice = device;
                            usbConn = usbManager.openDevice(selectedDevice);
                            serialDevice = UsbSerialDevice.createUsbSerialDevice(device, usbConn);
                            if (serialDevice.open()) {
                                serialDevice.setBaudRate(250000);
                                serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
                                serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                                for (DeviceConnectionListener listener : listeners) {
                                    listener.onDeviceConnected();
                                }
                            }
                        }
                    } else {
                        // TODO: acesso negado pelo usuário
                        for (DeviceConnectionListener listener : listeners) {
                            listener.onDevicePermissionDenied();
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                Toast.makeText(context, "New device attached", Toast.LENGTH_SHORT).show();
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                stopReadingData();

                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (usbConn != null &&
                        selectedDevice != null && selectedDevice.equals(device) &&
                        selectedDevice.getInterfaceCount() > 1) {
                    usbConn.releaseInterface(selectedDevice.getInterface(1));
                }

                Toast.makeText(context, "Device detached", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private UsbSerialInterface.UsbReadCallback mReadCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(final byte[] bytes) {
            Log.d("Visio", bytes.toString());
            for (byte b : bytes) {
                buffer[currentPos++] = b;

                if (currentPos >= buffer.length) {
                    final double[] valores = new double[sampleSize/8];

                    try {
                        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(buffer));
                        for (int i = 0; i < valores.length; i++) {
                            double valor = stream.readDouble();
                            valores[i] = valor;
                        }
                    } catch (EOFException e) {
                        Log.e("Visio", "End of stream", e);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (DeviceConnectionListener listener : listeners) {
                                listener.onDeviceRead(valores);
                            }
                        }
                    });

                    buffer = new byte[sampleSize];
                    currentPos = 0;


//                    if (currentPos + bytes.length >= sampleSize) {
//                        for (byte b : bytes) {
//                            buffer[currentPos++] = b;
//                        }
//
//                        for (DeviceConnectionListener listener : listeners) {
//                            listener.onDeviceRead(buffer);
//                        }
//
//                        buffer = new byte[sampleSize];
//                        currentPos = 0;
//                    } else {
//                        for (byte b : bytes) {
//                            buffer[currentPos++] = b;
//                        }
//                    }
                }
            }
        }
    };
}
