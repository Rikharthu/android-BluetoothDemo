package com.example.android.bluetoothdemo.bluetooth;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class ConnectThread extends Thread {
    private static final String NAME = "Bluetooth Demo";
    public static final UUID UUID= java.util.UUID.fromString(NAME);

    private final BluetoothSocket socket;
    private final BluetoothDevice device;
    private BluetoothAdapter bluetoothAdapter;

    public ConnectThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter=bluetoothAdapter;
        // Use a temporary object that is later assigned to socket,
        // because socket is final
        BluetoothSocket tmp = null;
        this.device = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(UUID);
        } catch (IOException e) { }
        socket = tmp;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        bluetoothAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            socket.connect();
            // client's and server's UUID matches and connection is accepted
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                socket.close();
            } catch (IOException closeException) { }
            return;
        }

        // Do work to manage the connection (in a separate thread)
        manageConnectedSocket(socket);
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) { }
    }
}
