package com.example.ethanmorris.smartcarseatapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


//This was the source for connection code
//http://www.truiton.com/2015/04/android-bluetooth-low-energy-ble-example/
//BroadcastReceiver
//http://www.intertech.com/Blog/using-localbroadcastmanager-in-service-to-activity-communications/


public class ConnectionWizard extends AppCompatActivity{

    public ListView list;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    public ArrayList<BluetoothDevice> devices;
    public ArrayList<String> display;
    public ArrayAdapter<String> adapter1;
    public boolean mScanning;
    public ProgressBar bar;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private List<ScanFilter> filters;
    private ScanSettings settings;
    BluetoothDevice bluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_wizard);

        list = (ListView) findViewById(R.id.bluetoothList);
        mHandler = new Handler();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        bar = (ProgressBar) findViewById(R.id.progressBar);
        devices = new ArrayList<BluetoothDevice>();
        display = new ArrayList<String >();
        adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, display);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ACTION_GATT_CONNECTED");
        intentFilter.addAction("ACTION_GATT_DISCONNECTED");
        intentFilter.addAction("ACTION_GATT_SERVICES_DISCOVERED");
        intentFilter.addAction("ACTION_DATA_AVAILABLE");

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,intentFilter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (devices.get(position) != null) {
                    //Connection(devices.get(position));
                    bluetoothDevice = devices.get(position);
                    Connection(bluetoothDevice);
                }
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        filters = new ArrayList<ScanFilter>();
        scanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanLeDevice(true);
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    scanner.stopScan(scanCallback);
                    bar.setVisibility(View.INVISIBLE);
                    invalidateOptionsMenu();
                }
            }, 10000);
            mScanning = true;
            scanner.startScan(filters, settings, scanCallback);
        } else {
            mScanning = false;
            scanner.stopScan(scanCallback);
        }
        invalidateOptionsMenu();
    }


    public ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

            if(!devices.contains(device) && device.getName() != null) {
                devices.add(device);
                display.add(device.getName());
                adapter1.notifyDataSetChanged();
                list.setAdapter(adapter1);
            }
        }
    };


    public void onDestroy(){
        if(gatt == null){
            return;
        }

        adapter1.clear();
        gatt.close();
        gatt = null;
        super.onDestroy();
    }

    public void Connection(BluetoothDevice device){
        Toast.makeText(this, device.toString(), Toast.LENGTH_LONG).show();
        BluetoothService myService = new BluetoothService(device);
        Intent intent = new Intent(this, BluetoothService.class);
        /*startService(intent);*/

        if(startService(intent) != null){
            Toast.makeText(this, "SERVICE IS RUNNING", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this, "SERVICE SUCKS", Toast.LENGTH_LONG).show();
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            /*Bundle extras = getIntent().getExtras();
            String temp = extras.getString(BluetoothService.EXTRA_DATA);*/

            if(BluetoothService.ACTION_GATT_CONNECTED.equals(action)){
                Toast.makeText(context, "Connected", Toast.LENGTH_LONG).show();
            }else if(BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)){
                Toast.makeText(context, "Not Connected", Toast.LENGTH_LONG).show();
            }else if(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                Toast.makeText(context, "Services", Toast.LENGTH_LONG).show();
            }else{
                if(BluetoothService.ACTION_DATA_AVAILABLE.equals(action)){
                    Toast.makeText(context, "Data Available", Toast.LENGTH_LONG).show();
                    //Toast.makeText(context, temp, Toast.LENGTH_LONG).show();
                }
            }
        }
    };
}
