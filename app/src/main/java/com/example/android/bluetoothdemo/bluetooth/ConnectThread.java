package com.example.android.bluetoothdemo.bluetooth;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class ConnectThread extends Thread {
    public static final String LOG_TAG=ConnectThread.class.getSimpleName();

    public interface OnConnectedThreadListener{
        void onConnected();
        void onReturnConnectedThread(ConnectedThread thread);
    }

    private static final String NAME = "38400000-8cf0-11bd-b23e-10b96e4ef00d";
    public static final UUID UUID= java.util.UUID.fromString(NAME);

    private OnConnectedThreadListener listener;

    private Activity activity;

    public void setContext(Activity activity) {
        this.activity = activity;
    }

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

    public OnConnectedThreadListener getListener() {
        return listener;
    }

    public void setListener(OnConnectedThreadListener listener) {
        this.listener = listener;
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
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "connected!", Toast.LENGTH_SHORT).show();
                }
            });

            ConnectedThread connectedThread = new ConnectedThread(socket);
            connectedThread.start();
            listener.onReturnConnectedThread(connectedThread);

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
