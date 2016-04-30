package com.example.ethanmorris.smartcarseatapp;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

/**
 * Created by ethanmorris on 3/8/16.
 */


public class BluetoothService extends Service {

    private final String TAG = BluetoothService.class.getSimpleName();
    private BluetoothAdapter adapter;
    private BluetoothManager bluetoothManager;
    private BluetoothGatt gatt;
    private int connectionState = STATE_DISCONNECTED;
    private String deviceAddress;
    private String temp = "";
    int counter = 0;
    private boolean mConnected = false;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;
    private int startMode;

    public static final String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "EXTRA_DATA";
    public static final String TRIGGER_ALARM = "TRIGGER_ALARM";
    private String incomingMessage;

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    public final static UUID UUID_MLDP_DATA_PRIVATE_CHARACTERISTIC = UUID.fromString(ConnectionWizard.MLDP_DATA_PRIVATE_CHAR);
    public final static UUID UUID_CHARACTERISTIC_NOTIFICATION_CONFIG = UUID.fromString(ConnectionWizard.CHARACTERISTIC_NOTIFICATION_CONFIG);

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        incomingMessage = new String();
        registerReceiver(broadcastReceiver, gattIntentFilter());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        if(gatt != null){
            gatt.close();
            gatt = null;
        }

        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.i(TAG, "Connected to GATT server, starting service discovery");
                gatt.discoverServices();
            }else{
                if(newState == BluetoothProfile.STATE_DISCONNECTED){
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    Log.i(TAG, "Disconnected from GATT server.");
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }else{
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic){
        final Intent intent = new Intent(action);

        if(action.equals(ACTION_DATA_AVAILABLE)){
            String dataValue = characteristic.getStringValue(0);

            if(!dataValue.equals("S")) {
                if (counter != 0) {
                    temp += dataValue;
                }

                counter++;

                if (temp.length() == 4) {
                    intent.putExtra(EXTRA_DATA, temp);
                    sendBroadcast(intent);
                    temp = "";
                }
            }else{
                temp = "";
            }

        }else{
            Log.d(TAG, "Action: " + action);
        }
    }

    public boolean init(){
        if(bluetoothManager == null){
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if(bluetoothManager == null){
                Log.e(TAG, "Unable to initialize BluetoothManager");
                return false;
            }
        }

        adapter = bluetoothManager.getAdapter();

        if(adapter == null){
            Log.e(TAG, "Unable to obtain BluetoothAdapter");
            return false;
        }

        return true;
    }

    public boolean connect(final String address){
        if(adapter == null || address == null){
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if(deviceAddress != null && address.equals(deviceAddress) && gatt != null){
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");

            if(gatt.connect()){
                return true;
            }else{
                return false;
            }
        }

        final BluetoothDevice bluetoothDevice = adapter.getRemoteDevice(address);

        if(bluetoothDevice == null){
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }

        //true is to auto connect and remember device
        gatt = bluetoothDevice.connectGatt(this, true, gattCallback);
        Log.d(TAG, "Trying to create connection");
        deviceAddress = address;
        return true;
    }

    public List <BluetoothGattService> getSupportedGattServices() {
        if(gatt == null){
            return null;
        }

        return gatt.getServices();
    }

    public void disconnect(){
        if(adapter == null || gatt == null){
            Log.w(TAG, "Adapter not initialized.");
            return;
        }

        gatt.disconnect();
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic){
        if(adapter == null || gatt == null){
            Log.w(TAG, "Adapter not initialized.");
            return;
        }

        gatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled){
        if(adapter == null || gatt == null){
            Log.w(TAG, "Adapter not initialized.");
            return;
        }

        gatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CHARACTERISTIC_NOTIFICATION_CONFIG);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled){
        if(adapter == null || gatt == null){
            Log.w(TAG, "Adapter not initialized.");
            return;
        }

        gatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CHARACTERISTIC_NOTIFICATION_CONFIG);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(BluetoothService.ACTION_GATT_CONNECTED.equals(action)){
                mConnected = true;
            }else if(BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)){
                mConnected = false;
            }else if(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                findMldpGattService(getSupportedGattServices());
                Log.i(TAG, "SERVICES FOUND");
            }else{
                if(BluetoothService.ACTION_DATA_AVAILABLE.equals(action)){
                    Log.i(TAG, BluetoothService.ACTION_DATA_AVAILABLE);
                    String dataValue = intent.getStringExtra(EXTRA_DATA);
                    Log.i(TAG, "DATAVALUE: " + dataValue);
                    processIncomingPacket(dataValue);
                }
            }
        }
    };

    private static IntentFilter gattIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ACTION_GATT_CONNECTED");
        intentFilter.addAction("ACTION_GATT_DISCONNECTED");
        intentFilter.addAction("ACTION_GATT_SERVICES_DISCOVERED");
        intentFilter.addAction("ACTION_DATA_AVAILABLE");
        intentFilter.addAction("TRIGGER_ALARM");
        return intentFilter;
    }

    private void processIncomingPacket(String data) {

        incomingMessage = data;

        if(incomingMessage.charAt(0) == '1' && incomingMessage.charAt(3) == '1'){
            //trigger alarm
        }

        if(incomingMessage.charAt(1) == '1' && incomingMessage.charAt(3) == '1'){
            //trigger a notfication
            Intent intent = new Intent(getApplicationContext(), ConnectionWizard.class);

            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
            taskStackBuilder.addParentStack(MainActivity.class);
            taskStackBuilder.addNextIntent(intent);

            PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

            builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_launcher))
                    .setColor(Color.RED)
                    .setContentTitle("Child in Danger")
                    .setContentText("Temperatures approaching dangerous levels")
                    .setContentIntent(pendingIntent);

            builder.setAutoCancel(true);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, builder.build());
        }

        if(incomingMessage.charAt(2) == '1' && incomingMessage.charAt(3) == '1'){
            //check geofence and trigger alarm if need be
        }
    }

    private void findMldpGattService(List <BluetoothGattService> gattServices){
        if(gattServices == null){
            Log.d(TAG, "findMldpGattService found no Services");
            return;
        }

        for(BluetoothGattService gattService : gattServices){
            List <BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
                final int characteristicProperties = gattCharacteristic.getProperties();

                if((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0){
                    setCharacteristicNotification(gattCharacteristic, true);
                }

                if((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_INDICATE)) > 0){
                    setCharacteristicIndication(gattCharacteristic, true);
                }

                if((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE)) > 0){
                    gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                }

                if((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0){
                    gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                }

                if((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_READ)) > 0){
                    readCharacteristic(gattCharacteristic);
                }
            }
        }
    }

    public void setDeviceAddress(String address){
        this.deviceAddress = address;
        connect(deviceAddress);
    }
}
