package com.example.ethanmorris.smartcarseatapp;


import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by ethanmorris on 3/8/16.
 */


public class BluetoothService extends IntentService {

    private final String TAG = BluetoothService.class.getSimpleName();
    BluetoothDevice bluetoothDevice;
    private BluetoothGatt gatt;
    private int connectionState = STATE_DISCONNECTED;
    private static BluetoothService instance = null;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;

    public static final String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "EXTRA_DATA";
    private final String SEND = "ACTIVE";


    public BluetoothService(){
        super("BluetoothService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String data = intent.getDataString();
    }

    BluetoothService(BluetoothDevice device){
        super("BluetoothService");
        gatt = device.connectGatt(this, true, gattCallback);
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;

            if(newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to gatt server.");
                Log.i(TAG, "Starting services discovery");
                gatt.discoverServices();
            }else{
                if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    intentAction = ACTION_GATT_DISCONNECTED;
                    connectionState = STATE_DISCONNECTED;
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(intentAction);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }else{
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                /*
                These lines were added because they are my attempt
                to send data to the bluetooth module on receiving
                data from the module
                 */

                /*byte[] data = SEND.getBytes();
                characteristic.setValue(data);
                gatt.writeCharacteristic(characteristic);*/
            }
        }
    };


    public void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic){
        Context context = getApplicationContext();
        Toast.makeText(context, characteristic.toString(), Toast.LENGTH_LONG).show();
        Log.i("UPDATE: ", characteristic.toString() + " ");
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();

        if(data != null && data.length > 0){
            final StringBuilder stringBuilder = new StringBuilder(data.length);

            for(byte byteChar : data){
                stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
