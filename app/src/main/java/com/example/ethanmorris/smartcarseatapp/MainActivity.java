package com.example.ethanmorris.smartcarseatapp;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter adapter;
    private final String DeviceName = "00:1E:C0:32:52:67";
    private boolean found = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "Bluetooth LE not supported!!!!", Toast.LENGTH_LONG).show();
            finish();
        }

        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();

        //access the button on MainActivity
        Button button = (Button) findViewById(R.id.connectBtn);
        TextView textView = (TextView) findViewById(R.id.textView);

        //verify bluetooth is available
        checkBluetooth();

        for(BluetoothDevice d : manager.getConnectedDevices(BluetoothProfile.GATT)){
            Toast.makeText(this, d.toString(), Toast.LENGTH_LONG).show();
            if(d.toString() == DeviceName){
                found = true;
            }
        }

        if(found){
            button.setVisibility(View.INVISIBLE);
            textView.setText("You are already connected!");
        }

    }

    public void checkBluetooth(){
        if(adapter == null || !adapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }


    public void connectDevice(View view){
        Intent intent = new Intent(MainActivity.this, ConnectionWizard.class);
        startActivity(intent);
    }
}