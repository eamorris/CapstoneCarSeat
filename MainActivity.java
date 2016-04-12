package com.carseat.capstone.csce.uark.smartcarseatapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    String deviceName;
    ArrayList<String> foundDevices = new ArrayList<String>();
    //ArrayAdapter<String> mArrayAdapter;
    Boolean found = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //access the button on MainActivity
        Button button = (Button) findViewById(R.id.connectBtn);

        //verify bluetooth is available
        checkBluetooth();

        //find paired devices
        find();

        if(found) {
            //call connection function
            button.setVisibility(View.INVISIBLE);
        }

    }


    public void checkBluetooth(){
        if(mBluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "Bluetooth is not supported!", Toast.LENGTH_LONG).show();
        }else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    /*
    this function is to check for all paired devices before searching for discoverable devices
     */
    public void find(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices) {
                //This add all bluetooth devices into an array, from here we can check for carseat BT module
                foundDevices.add(device.getName());
            }
        }

        //this loop looks for our device name
        for(String s : foundDevices){
            if(s == deviceName){
                //device = s;
                found = true;
                break;
            }
        }
    }

    public void connectDevice(View view){
        Intent intent = new Intent(MainActivity.this, ConnectionWizard.class);
        startActivity(intent);
    }
}
