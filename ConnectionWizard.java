package com.carseat.capstone.csce.uark.smartcarseatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;


public class ConnectionWizard extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayAdapter<String> mArrayAdapter;
    ProgressBar bar;
    ListView list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_wizard);

        bar = (ProgressBar) findViewById(R.id.progressBar);
        mBluetoothAdapter.startDiscovery();
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        list = (ListView) findViewById(R.id.bluetoothList);
        list.setAdapter(mArrayAdapter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //I'M NOT SURE WHY BUT IF THE device.getAddress() IS NOT INCLUDED HERE
                //THEN THE list.setAdapter(mArrayAdapter) CALL UP TOP CRASHES THE APP
                if(device.getName() != null) {
                    mArrayAdapter.add(device.getName() /*+ " " + device.getAddress()*/);
                }
            }else{
                if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    mBluetoothAdapter.cancelDiscovery();
                    bar.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    public void connection(){
        //connection code goes here
    }
}
