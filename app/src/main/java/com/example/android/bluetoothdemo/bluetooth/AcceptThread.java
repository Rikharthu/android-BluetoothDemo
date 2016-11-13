package com.example.android.bluetoothdemo.bluetooth;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class AcceptThread extends Thread {
    private static final String LOG_TAG =AcceptThread.class.getSimpleName() ;

    public interface OnAcceptedThreadListener {
        void onConnected();
        void onReturnAcceptedThread(ConnectedThread thread);
    }

    private OnAcceptedThreadListener listener;

    private static final String NAME = "38400000-8cf0-11bd-b23e-10b96e4ef00d";
    private static final UUID UUID = java.util.UUID.fromString(NAME);

    private final BluetoothServerSocket serverSocket;

    private Activity activity;

    public void setContext(Activity activity) {
        this.activity = activity;
    }

    public AcceptThread(BluetoothAdapter adapter) {
        Log.d(LOG_TAG,UUID.toString());

        // Use a temporary object that is later assigned to serverSocket,
        // because serverSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = adapter.listenUsingRfcommWithServiceRecord(NAME, UUID);
        } catch (IOException e) { }
        serverSocket = tmp;
    }

    public OnAcceptedThreadListener getListener() {
        return listener;
    }

    public void setListener(OnAcceptedThreadListener listener) {
        this.listener = listener;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = serverSocket.accept();
                // socket is already connected here
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket(socket);
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "accepted!", Toast.LENGTH_SHORT).show();
                }
            });

            ConnectedThread acceptThread = new ConnectedThread(socket);
            acceptThread.start();

            listener.onReturnAcceptedThread(acceptThread);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            serverSocket.close();
        } catch (IOException e) { }
    }
}
