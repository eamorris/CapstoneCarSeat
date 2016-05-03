package com.example.ethanmorris.smartcarseatapp;

import android.app.Activity;
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
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//sources
/*
developer.android.com

http://www.microchip.com/DevelopmentTools/ProductDetails.aspx?PartNO=rn-4020-pictail

This was the source for connection code
http://www.truiton.com/2015/04/android-bluetooth-low-energy-ble-example/

BroadcastReceiver
http://www.intertech.com/Blog/using-localbroadcastmanager-in-service-to-activity-communications/
 */


public class BluetoothService extends Service implements ConnectionCallbacks,
        OnConnectionFailedListener, ResultCallback<Status> {

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
    private PendingIntent alarmPendingIntent;

    public final static UUID UUID_MLDP_DATA_PRIVATE_CHARACTERISTIC = UUID.fromString(ConnectionWizard.MLDP_DATA_PRIVATE_CHAR);
    public final static UUID UUID_CHARACTERISTIC_NOTIFICATION_CONFIG = UUID.fromString(ConnectionWizard.CHARACTERISTIC_NOTIFICATION_CONFIG);

    private final IBinder mBinder = new LocalBinder();

    protected Location mLastLocation;
    protected GoogleApiClient mGoogleApiClient;
    protected ArrayList<Geofence> mGeofenceList;
    private SharedPreferences mSharedPreferences;
    private PendingIntent mGeofencePendingIntent;

    private boolean mGeofencesAdded;
    private boolean carHasBeenParked;
    private boolean notificationTriggered;

    @Override
    public void onCreate() {
        super.onCreate();
        incomingMessage = new String();
        registerReceiver(broadcastReceiver, gattIntentFilter());

        mGeofencePendingIntent = null;
        mGeofenceList = new ArrayList<Geofence>();
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);

        carHasBeenParked = false;
        buildGoogleApiClient();
        mGoogleApiClient.connect();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //handles unbinding activities from the service
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

        /*
        automatically detects when a user has connected or disconnected from the the gatt
        server and logs the data
         */
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

        /*
        automatically called when bluetooth gatt services are discovered and send the results to
        the broadcast receiver which determines what to do with them
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }else{
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /*
        automatically called when a bluetooth characteristic is read and sends the characteristic
        to the broadcast receiver which gets the information from the characteristic
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        /*
        automatically called when a bluetooth characteristic is changed and sends the characteristic
        to the broadcast receiver which gets the information from the characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    //sends appropriate broadcasts to the recevier
    private void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    //sends appropriate broadcasts to the recevier
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic){
        final Intent intent = new Intent(action);
        if(action.equals(ACTION_DATA_AVAILABLE)){
            String dataValue = characteristic.getStringValue(0);
            if(!dataValue.equals("S")) {
                if (counter != 0) {
                    //permanent += dataValue;
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

    //initializes the bluetooth adapter for the BluetoothService
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

    /*
    checks to see if the bluetooth adapter is initialized or in use
    if not, the device address is used to connect to selected device from the connection wizard
    with the bluetooth gatt server
     */
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

    //returns all services associated with the bluetooth connection
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

    //reads bluetooth characteristics that are of the read type
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
                    Log.i(TAG, dataValue);
                    processIncomingPacket(dataValue);
                }
            }
        }
    };

    //filters for the broadcast receiver to determine what actions to take when a broadcast is sent
    private static IntentFilter gattIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ACTION_GATT_CONNECTED");
        intentFilter.addAction("ACTION_GATT_DISCONNECTED");
        intentFilter.addAction("ACTION_GATT_SERVICES_DISCOVERED");
        intentFilter.addAction("ACTION_DATA_AVAILABLE");
        intentFilter.addAction("TRIGGER_ALARM");
        return intentFilter;
    }

    /*
    takes the incoming binary string and determines what if any action is necessary
    to prevent harm to the child
     */
    private void processIncomingPacket(String data) {
        /*  Incoming Message is the string coming in from the car seat
        String is composed of four bits, each corresponding to an individual sensor
            Data Key:
                First two bits are temperature flags
                '00' Good: signifies a comfortable temperature
                '01' Warning: higher than 75 deg F but lower than 90 deg F
                '10' Hazardous: higher than 90 deg F

                Third bit is the car power
                '1' signifies that the car is on
                '0' signifies that the car is off (car seat runs only on battery in this case)

                Final bit is the weight sensor
                '1' signifies that a child is in the seat (weight is present)
                '0' signifies that the child is not in the seat (weight is not present)
    */
        incomingMessage = data;
        String warning = "";

        if(incomingMessage.charAt(0) == '0' && incomingMessage.charAt(1) == '0'){
            notificationTriggered = false;
        }
        
        if((incomingMessage.charAt(0) == '1' || incomingMessage.charAt(1) == '1') &&
                incomingMessage.charAt(3) == '1' && !notificationTriggered){

            notificationTriggered = true;

            if(incomingMessage.charAt(0) == '1'){
                warning = "TEMPERATURES AT DANGEROUS LEVELS";
            }

            if(incomingMessage.charAt(1) == '1'){
                warning = "Temperatures approaching dangerous levels";
            }

            //trigger a notfication
            Intent intent = new Intent(getApplicationContext(), ConnectionWizard.class);
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
            taskStackBuilder.addParentStack(MainActivity.class);
            taskStackBuilder.addNextIntent(intent);
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_launcher))
                    .setColor(Color.RED)
                    .setContentTitle("Child in Danger")
                    .setContentText(warning)
                    .setContentIntent(pendingIntent)
                    .setSound(sound);
            builder.setAutoCancel(true);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, builder.build());

        }

        // Car has been parked, child is in the seat: Create a geofence
        if(incomingMessage.charAt(2) == '0' && incomingMessage.charAt(3) == '1' && carHasBeenParked == false){
            //check geofence and trigger alarm if need be
            Log.i(TAG, incomingMessage + ": Car seat switched to batteyr power, weight active");
            carHasBeenParked = true;
            createGeofence();
        }

        // Child has been removed from the seat: Kill any existing geofences
        if(incomingMessage.charAt(3) == '0' && carHasBeenParked == true){
            // Kill the geofence because the child has been removed from the car seat
            Log.i(TAG, incomingMessage + ": Child removed from car seat, remove geofence");
            carHasBeenParked = false;
            removeGeofence();
        }
    }

    //takes the arraylist of all available bluetooth services and determines what type of
    //characteristics are contained and takes appropriate actions
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

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);

        return builder.build();
    }

    // This method finds the current location and adds it to the list of geofences
    public void createGeofence() {
        if (!mGoogleApiClient.isConnected()) {
            Log.i(TAG, "GoogleApiClient is not connected");
            return;
        }

        // Testing Alarm Service
        /*
        Intent intent = new Intent(this, AlarmReceiverActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 99, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() - 1, pendingIntent);
        */

        // Find current location
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Double lat = mLastLocation.getLatitude();
            Double lng = mLastLocation.getLongitude();
            Toast.makeText(this, "Current location found:\n" + lat + "\n" + lng, Toast.LENGTH_SHORT).show();
            addToGeofenceList(mLastLocation);

        } else {
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
        }

        // addToGeofenceList(mLastLocation);

        try {
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, getGeofencingRequest(),
                    getGeofencePendingIntent()).setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG, "Security exception");
        }
    }

    // This function simply removes all geofences and clears the list of geofences
    public void removeGeofence() {

        if (!mGoogleApiClient.isConnected()) {
            Log.i(TAG, "GoogleApiClient is not connected");
            return;
        }
        try {
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, getGeofencePendingIntent()).setResultCallback(this);
            mGeofenceList.clear();
            Toast.makeText(this, "Child secured: Geofence removed", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Geofences removed");
        } catch (SecurityException securityException) {
            Log.e(TAG, "Security exception");
        }
      //  AlarmReceiverActivity.mMediaPlayer.stop();
        // This clears the notification message that reminds the user to secure their child
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }

    // This method adds a geofence to the list based on the location passed in as a parameter
    public void addToGeofenceList(Location location) {

        mGeofenceList.add(new Geofence.Builder()
                .setRequestId("")
                .setCircularRegion(
                        location.getLatitude(),
                        location.getLongitude(),
                        Constants.GEOFENCE_RADIUS
                )
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)

                .build());
        Toast.makeText(this, "Geofence added", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Geofence added");
    }

    private PendingIntent getGeofencePendingIntent() {

        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void onResult(Status status) {

        if (status.isSuccess()) {
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.apply();
            Toast.makeText(
                    this, "Geofence add/delete event", Toast.LENGTH_SHORT).show();
            String numGeofences = String.valueOf(mGeofenceList.size());
            Log.i(TAG, "Number of active geofences: " + numGeofences);
        } else {
            Log.e(TAG, "Error adding/deleting geofences");
        }
    }
}
