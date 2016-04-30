package br.iesb.vismobile.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.iesb.vismobile.BuildConfig;

/**
 * Classe UsbConnection (Singleton)
 * Created by dfcarvalho on 11/12/15.
 * Controla a comunicação entre o dispositivo e o Visio (espectofotometro)
 * Obter instância com o método: getSingleton()
 */
public class UsbConnection {
    private static UsbConnection SINGLETON = null;
//    private static int counter = 0;

    private static final String ACTION_USB_PERMISSION = "br.iesb.vismobile.USB_PERMISSION";

    public static final String PREFS = "CONN_PREFS";
    public static final String PREF_BAUD_RATE = "BAUD_RATE";
    public static final String PREF_DATA_BITS = "DATA_BITS";
    public static final String PREF_STOP_BITS = "STOP_BITS";
    public static final String PREF_PARIDADE = "PARIDADE";

    public static final int STD_BAUD_RATE = 250000;
    public static final int STD_DATA_BITS = UsbSerialInterface.DATA_BITS_8;
    public static final int STD_STOP_BITS = 0;
    public static final int STD_PARIDADE = UsbSerialInterface.PARITY_NONE;
    public static final int STD_SAMPLE_SIZE = 2048;

    private Context context;
    private SharedPreferences prefs;
    private Set<DeviceConnectionListener> listeners;
    private UsbManager usbManager;
    private PendingIntent permissionIntent;
    private UsbDevice selectedDevice;
    private UsbDeviceConnection usbConn;
    private boolean connectedToMockDevice;
    private UsbSerialDevice serialDevice;
    private boolean reading;
    private int sampleSize;
    private byte[] buffer;
    private int currentPos;
    private int baudRate;
    private int dataBits;
    private int stopBits;
    private int paridade;

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

//        counter++;
        SINGLETON.addListener(listener);

        return SINGLETON;
    }

//    public void release() {
//        counter--;
//
//        if (counter == 0) {
//            unregisterReceiver();
//            this.context.recei
//            SINGLETON = null;
//        }
//    }

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
        sampleSize = STD_SAMPLE_SIZE;
        buffer = new byte[sampleSize];
        currentPos = 0;

        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        baudRate = prefs.getInt(PREF_BAUD_RATE, STD_BAUD_RATE);
        dataBits = prefs.getInt(PREF_DATA_BITS, STD_DATA_BITS);
        stopBits = prefs.getInt(PREF_STOP_BITS, STD_STOP_BITS);
        paridade = prefs.getInt(PREF_PARIDADE, STD_PARIDADE);

        connectedToMockDevice = false;
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

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_BAUD_RATE, baudRate);
        editor.apply();
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_DATA_BITS, dataBits);
        editor.apply();
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_STOP_BITS, stopBits);
        editor.apply();
    }

    public int getParidade() {
        return paridade;
    }

    public void setParidade(int paridade) {
        this.paridade = paridade;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_PARIDADE, paridade);
        editor.apply();
    }

    /**
     * Solicita do sistema permissão para acessar um dispositivo USB
     * @param device Dispositivo USB que deseja acessar
     */
    public void requestPermission(UsbDevice device) {
        usbManager.requestPermission(device, permissionIntent);
    }

    public boolean isConnected() {
        if (BuildConfig.MOCK_DEVICE) {
            if (connectedToMockDevice) {
                return true;
            }
        }

        return usbConn != null;
    }

    public void disconnect() {
        stopReadingData();

        if (BuildConfig.MOCK_DEVICE) {
            if (connectedToMockDevice) {
                connectedToMockDevice = false;

                for (DeviceConnectionListener listener : listeners) {
                    listener.onDeviceDisconnected();
                }

                return;
            }
        }

        serialDevice.close();
        serialDevice = null;
        usbConn.close();
        usbConn = null;
        selectedDevice = null;

        for (DeviceConnectionListener listener : listeners) {
            listener.onDeviceDisconnected();
        }
    }

    public void setConnectedToMockDevice(boolean connectedToMockDevice) {
        this.connectedToMockDevice = connectedToMockDevice;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        stopReadingData();
        this.sampleSize = sampleSize;
    }

    /**
     * Inicia leitura de dados do dispositivo conectado.
     * Dados lidos são repassados para os listeners através do método onDeviceRead()
     */
    public void startReadingData() {
        if (!reading) {
            reading = true;

            if (BuildConfig.MOCK_DEVICE) {
                if (isConnected()) {
                    commExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                synchronized (readLock) {
                                    if (!reading) {
                                        break;
                                    }
                                }

                                byte[] data = new byte[sampleSize];
                                Random rand = new Random();

                                for (int i = 0; i < sampleSize; i++) {
                                    synchronized (readLock) {
                                        if (!reading) {
                                            break;
                                        }
                                    }
                                    Number value = -50 + (100) * rand.nextDouble();
                                    data[i] = value.byteValue();
                                }

                                mReadCallback.onReceivedData(data);

                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    // TODO:
                                    e.printStackTrace();
                                }
                            }

                        }
                    });
                    return;
                }
            }

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
        if (BuildConfig.MOCK_DEVICE) {
            if (isConnected()) {
                byte[] data = new byte[sampleSize];

                Random rand = new Random();
                for (int i = 0; i < sampleSize; i++) {
                    Number value;
                    if (i == 0) {
                        value = -50 + (100) * rand.nextDouble();
                    } else {
                        double min = data[i-1] - 10 >= -50 ? data[i-1] - 10 : data[i-1];
                        double max = data[i-1] + 10 <= 50 ? data[i-1] + 10 : data[i-1];
                        value = min + (max - min) * rand.nextDouble();
                    }

                    data[i] = value.byteValue();
                }

                mReadOnceCallback.onReceivedData(data);
                return;
            }
        }

        commExecutor.submit(new Runnable() {
            @Override
            public void run() {
                serialDevice.write("+".getBytes());
                serialDevice.read(mReadOnceCallback);
            }
        });
    }

    public void stopReadingData() {
        synchronized (readLock) {
            reading = false;
        }

        for (DeviceConnectionListener listener : listeners) {
            listener.onDeviceStopReading();
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

    public BroadcastReceiver getUsbPermissionReceiver() {
        return usbPermissionReceiver;
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
                                serialDevice.setBaudRate(baudRate);
                                serialDevice.setDataBits(dataBits);
                                serialDevice.setParity(paridade);
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
                synchronized (readLock) {
                    if (!reading) {
                        return;
                    }
                }

                buffer[currentPos++] = b;

                if (currentPos >= sampleSize) {
                    final double[] valores = new double[sampleSize];
                    for (int i = 0; i < sampleSize; i++) {
                        Number num = buffer[i];
                        valores[i] = num.doubleValue();
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
                }
            }
        }
    };

    private UsbSerialInterface.UsbReadCallback mReadOnceCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(final byte[] bytes) {
            Log.d("Visio", bytes.toString());

            for (byte b : bytes) {
                buffer[currentPos++] = b;

                if (currentPos >= sampleSize) {
                    final double[] valores = new double[sampleSize];
                    for (int i = 0; i < sampleSize; i++) {
                        Number num = buffer[i];
                        valores[i] = num.doubleValue();
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
                }
            }
        }
    };
}
