package com.belt.hapticnav;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothSPPManager {
    private static final String TAG = "BluetoothSPP";
    private static final String DEVICE_NAME = "ESP32_Belt";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private BluetoothCallback callback;

    public interface BluetoothCallback {
        void onConnectionStateChanged(boolean connected, String message);
    }

    public BluetoothSPPManager(BluetoothCallback callback) {
        this.callback = callback;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @SuppressLint("MissingPermission")
    public void connect() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            callback.onConnectionStateChanged(false, "Bluetooth is OFF");
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice esp32Device = null;

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (DEVICE_NAME.equals(device.getName())) {
                    esp32Device = device;
                    break;
                }
            }
        }

        if (esp32Device == null) {
            callback.onConnectionStateChanged(false, "Please pair 'ESP32_Belt' first");
            return;
        }

        bluetoothAdapter.cancelDiscovery();

        new Thread(() -> {
            try {
                bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(SPP_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                callback.onConnectionStateChanged(true, "Connected to ESP32_Belt");
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                close();
                callback.onConnectionStateChanged(false, "Connection Failed");
            }
        }).start();
    }

    public void sendCommand(String cmd) {
        if (outputStream != null) {
            try {
                outputStream.write(cmd.getBytes());
                Log.d(TAG, "Sent: " + cmd);
            } catch (IOException e) {
                Log.e(TAG, "Send failed", e);
                callback.onConnectionStateChanged(false, "Connection Lost");
                close();
            }
        }
    }

    public void close() {
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException ignored) {}
    }
}
