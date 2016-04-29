package com.example.ethanmorris.smartcarseatapp;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

    private final String TAG = ConnectionWizard.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private BluetoothService myService;
    private String mDeviceName, mDeviceAddress;
    private BluetoothGattCharacteristic mDataMDLP, mControlMLDP;
    private static final String MLDP_PRIVATE_SERVICE = "00035b03-58e6-07dd-021a-08123a000300";
    public static final String MLDP_DATA_PRIVATE_CHAR =    "00035b03-58e6-07dd-021a-08123a000301";
    private static final String MLDP_CONTROL_PRIVATE_CHAR = "00035b03-58e6-07dd-021a-08123a0003ff";
    public static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private String incomingMessage;

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

        //final Intent intent = getIntent();
        mHandler = new Handler();
        incomingMessage = new String();

        list = (ListView) findViewById(R.id.bluetoothList);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        bar = (ProgressBar) findViewById(R.id.progressBar);
        devices = new ArrayList<BluetoothDevice>();
        display = new ArrayList<String >();
        adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, display);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (devices.get(position) != null) {
                    bluetoothDevice = devices.get(position);
                    mDeviceName = bluetoothDevice.getName();
                    mDeviceAddress = bluetoothDevice.getAddress();
                }

                Intent gattServiceIntent = new Intent(ConnectionWizard.this, BluetoothService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                startService(gattServiceIntent);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        if(myService != null){
            final boolean result = myService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result = " + result);
        }

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

    @Override
    protected void onStop() {
        Log.i(TAG, "In onStop() " + mDeviceAddress);
        super.onStop();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service){
            myService = ((BluetoothService.LocalBinder) service).getService();
            if(!myService.init()){
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            //test if i can maintain connection
            //myService.setDeviceAddress(mDeviceAddress);
            myService.connect(mDeviceAddress);
        }

        //Maybe removing this will keep the connection after closing the app
        @Override
        public void onServiceDisconnected(ComponentName componentName){

        }
    };
}
