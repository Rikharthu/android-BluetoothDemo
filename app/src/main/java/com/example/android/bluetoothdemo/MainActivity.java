package com.example.android.bluetoothdemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_ENABLE_BLUETOOTH = 7103;

    BluetoothAdapter bluetoothAdapter;
    private SimpleAdapter arrayAdapter;
    private List<HashMap<String,String>> devices;

    private RelativeLayout rootLayout;
    private Button turnBluetoothOnBtn;
    private Button discoverDevicesBtn;
    private ListView deviceList;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = (RelativeLayout) findViewById(R.id.root_layout);
        deviceList= (ListView) findViewById(R.id.device_list);
        progressBar= (ProgressBar) findViewById(R.id.progressBar);
        turnBluetoothOnBtn = (Button) findViewById(R.id.turn_bluetooth_on_button);
        discoverDevicesBtn= (Button) findViewById(R.id.discover_devices_button);

        turnBluetoothOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turnOnBluetooth();
            }
        });

        discoverDevicesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverDevicesBtn.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                scanForDevices();
            }
        });

        devices=new ArrayList<>();
        String[] from={"SOURCE","DEVICE_NAME","DEVICE_MAC_ADDRESS"};//string array
        int[] to={R.id.list_item_source,R.id.list_item_device_name,R.id.list_item_device_mac_address};//int array of views id's
        arrayAdapter=new SimpleAdapter(this,devices,R.layout.list_item,from,to);
        deviceList.setAdapter(arrayAdapter);


        // the entry-point for all Bluetooth interaction
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                // bluetooth is disabled. show connect button
                turnBluetoothOnBtn.setVisibility(View.VISIBLE);
            }else{
                turnBluetoothOnBtn.setVisibility(View.GONE);

                // check coarse location permission
                int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                // enable discoverability
                Intent discoverableIntent = new
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);

            }
        }
    }

    private void scanForDevices(){
        devices.clear();
        arrayAdapter.notifyDataSetChanged();

        // first query paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                HashMap<String,String> deviceInfo = new HashMap<>();
                deviceInfo.put("SOURCE","paired");
                deviceInfo.put("DEVICE_NAME",device.getName()==null?"<null>":device.getName());
                deviceInfo.put("DEVICE_MAC_ADDRESS",device.getAddress());
                devices.add(deviceInfo);
                arrayAdapter.notifyDataSetChanged();
                Log.d(LOG_TAG,"paired device: "+device.getName() + "\n" + device.getAddress());
            }
        }

        // then start a device discovery asynchronously (requires coarse location permissions)
        boolean isDiscovering = bluetoothAdapter.startDiscovery();
        if(isDiscovering){
            Toast.makeText(this, "Discovering devices", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Could not start bluetooth discovery", Toast.LENGTH_SHORT).show();
        }
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // check if we are still scanning for devices
                if(bluetoothAdapter.isDiscovering()){
                    handler.postDelayed(this, 100);
                }else{
                    discoverDevicesBtn.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, "Discovering finished!", Toast.LENGTH_SHORT).show();
                }
            }
        };
        // postpone by 10 sec, since discovery takes ~12 secs
        handler.postDelayed(runnable, 10000);
    }

    private void turnOnBluetooth() {
        // request to turn on the bluetooth
        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
        /* prompt dialog will be shown to enable bluetooth (activity looses focus)
        If the user responds "Yes," the system will begin to enable Bluetooth
        and focus will return to your application once the process completes (or fails) */
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                // bluetooth turned on
                turnBluetoothOnBtn.setVisibility(View.GONE);
            } else if (resultCode == RESULT_CANCELED) {
                // user declined to turn on bluetooth or there was an error
                Snackbar snackbar = Snackbar.make(rootLayout, "Bluetooth is required!", Snackbar.LENGTH_LONG)
                        .setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                turnOnBluetooth();
                            }
                        });
                snackbar.show();

            }
        }

    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // bluetooth state changed
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    Toast.makeText(context, "Bluetooth is off!", Toast.LENGTH_SHORT).show();
                    turnBluetoothOnBtn.setVisibility(View.VISIBLE);
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    Toast.makeText(context, "Bluetooth is on!", Toast.LENGTH_SHORT).show();
                    turnBluetoothOnBtn.setVisibility(View.GONE);
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // When discovery finds a bluetooth device
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.d(LOG_TAG,"discovered device: "+device.getName() + "\n" + device.getAddress());

                HashMap<String,String> deviceInfo = new HashMap<>();
                deviceInfo.put("SOURCE","discovered");
                deviceInfo.put("DEVICE_NAME",device.getName()==null?"<null>":device.getName());
                deviceInfo.put("DEVICE_MAC_ADDRESS",device.getAddress());
                devices.add(deviceInfo);
                arrayAdapter.notifyDataSetChanged();
            }
        }
    };

}
