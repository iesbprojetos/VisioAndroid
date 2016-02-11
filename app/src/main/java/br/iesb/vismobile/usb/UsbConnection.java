package br.iesb.vismobile.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private static final String ACTION_USB_PERMISSION = "br.iesb.vismobile.USB_PERMISSION";

    private Context context;
    private Set<DeviceConnectionListener> listeners;
    private UsbManager usbManager;
    private PendingIntent permissionIntent;
    private UsbDevice selectedDevice;
    private String mockDevice;
    private List<UsbEndpoint> usbEndpoints;
    private UsbDeviceConnection usbConn;

    private final ExecutorService commExecutor = Executors.newSingleThreadExecutor();

    /**
     * Obter instância singleton da classe UsbConnection. Uma nova instância é criada, caso já não exista.
     * @param context Android Context (de preferência Application Context)
     * @param listener Objeto que irá receber notificações sobre a conexão com o Visio
     * @return Instância singleton da classe UsbConnection
     */
    public static synchronized UsbConnection getSingleton(Context context, DeviceConnectionListener listener) {
        if (SINGLETON == null) {
            SINGLETON = new UsbConnection(context, listener);
        }

        SINGLETON.addListener(listener);

        return SINGLETON;
    }

    /**
     * Construtor privado (Singleton Pattern)
     * @param context Android Context (de preferência Application Context)
     * @param listener Objeto que irá receber notificações sobre a conexão com o Visio
     */
    private UsbConnection(Context context, DeviceConnectionListener listener) {
        this.context = context;
        this.listeners = new HashSet<>();
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbPermissionReceiver, filter);
        usbEndpoints = new ArrayList<>();
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

    /**
     * Somente para testes
     * Simula a solicitação de permissão para acessar dispositivo USB
     * Em produção, usar requestPermission(UsbDevice)
     * @param mockDevice nome do dispositivo USB simulado
     */
    public void mockRequestPermission(String mockDevice) {
        Intent intent = new Intent(ACTION_USB_PERMISSION);
        intent.putExtra("MOCK_DEVICE", mockDevice);

        context.sendBroadcast(intent);
    }

    /**
     * Envia dados para o dispositivo USB conectado
     * @param data Dados a serem enviados
     */
    public void writeData(final String data) {
        commExecutor.submit(new Runnable() {
            @Override
            public void run() {
                // TODO: write
                UsbEndpoint outEndpoint = usbEndpoints.get(0);
                if (usbConn.bulkTransfer(outEndpoint, data.getBytes(), data.getBytes().length, 0) <= 0) {
                    if (listeners != null) {
                        // alert error on main thread
                        Handler mainHandler = new Handler(context.getMainLooper());
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                for (DeviceConnectionListener listener : listeners) {
                                    listener.onDeviceWriteOperationFailed();
                                }
                            }
                        };
                        mainHandler.post(r);
                    }
                }
            }
        });
    }

    public void readData() {
        commExecutor.submit(new Runnable() {
            @Override
            public void run() {
                // TODO: read
                byte[] buffer = new byte[1024];
                UsbEndpoint inEndpoint = usbEndpoints.get(1);
                
                if (usbConn.bulkTransfer(inEndpoint, buffer, 1024, 0) <= 0) {
                    if (listeners != null) {
                        // error
                        Handler mainHandler = new Handler(context.getMainLooper());
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                for (DeviceConnectionListener listener : listeners) {
                                    listener.onDeviceReadOperationFailed();
                                }
                            }
                        };
                        mainHandler.post(r);
                    }
                }
            }
        });
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
                    if (BuildConfig.MOCK_DEVICE) {
                        String intentMockDevice = intent.getStringExtra("MOCK_DEVICE");
                        if (intentMockDevice != null) {
                            mockDevice = intentMockDevice;
                            for (DeviceConnectionListener listener : listeners) {
                                listener.onDevicePermissionGranted(null);
                            }
                            // TODO: set up communication

                        } else {
                            // TODO: permission denied
                        }
                    } else {
                        // Dispositivo USB
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        // Acesso autorizado pelo usuário?
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            // Sim, obter interface de comunicação
                            if (device != null) {
                                selectedDevice = device;
                                for (DeviceConnectionListener listener : listeners) {
                                    listener.onDevicePermissionGranted(device);
                                }

                                // TODO: test
                                UsbInterface usbInterface = selectedDevice.getInterface(0);
                                if (BuildConfig.DEBUG) {
                                    for (DeviceConnectionListener listener : listeners) {
                                        listener.onDeviceShowInfo(selectedDevice);
                                    }
                                }

                                // TODO?: get two endpoints - in and out
                                UsbEndpoint endpoint = usbInterface.getEndpoint(0);
                                usbEndpoints.add(endpoint);
                                usbConn = usbManager.openDevice(selectedDevice);
                                usbConn.claimInterface(usbInterface, true);
                            }
                        } else {
                            // TODO: acesso negado pelo usuário
                        }
                    }
                }
            }
        }
    };
}
