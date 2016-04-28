package com.example.ethanmorris.smartcarseatapp;

import android.app.PendingIntent;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.location.LocationServices;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements ConnectionCallbacks,
        OnConnectionFailedListener, ResultCallback<Status> {

    protected static final String TAG = "MainActivity";

    private BluetoothAdapter adapter;
    private final String DeviceName = "00:1E:C0:32:52:67";
    private boolean found = false;

    protected Location mLastLocation;
    protected GoogleApiClient mGoogleApiClient;
    protected ArrayList<Geofence> mGeofenceList;
    private SharedPreferences mSharedPreferences;
    private PendingIntent mGeofencePendingIntent;

    private boolean mGeofencesAdded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
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

        for (BluetoothDevice d : manager.getConnectedDevices(BluetoothProfile.GATT)) {
            Toast.makeText(this, d.toString(), Toast.LENGTH_LONG).show();
            if (d.toString() == DeviceName) {
                found = true;
            }
        }

        if (found) {
            button.setVisibility(View.INVISIBLE);
            textView.setText("You are already connected!");
        }

        // Geofence code below
        Button geofenceButton = (Button) findViewById(R.id.geofenceButton);

        mGeofencePendingIntent = null;
        mGeofenceList = new ArrayList<Geofence>();
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);

        // create geofences using current location on button click

        buildGoogleApiClient();


    }

    public void checkBluetooth() {
        if (adapter == null || !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
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


    public void connectDevice(View view) {
        Intent intent = new Intent(MainActivity.this, ConnectionWizard.class);
        startActivity(intent);
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

    public void createGeofence(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Log.i(TAG, "GoogleApiClient is not connected");
            return;
        }

        // find current location

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null){
            Double lat = mLastLocation.getLatitude();
            Double lng = mLastLocation.getLongitude();
            Toast.makeText(this, "Current location found:\n" + lat + "\n" + lng, Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
        }

        addToGeofenceList(mLastLocation);

        try {
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, getGeofencingRequest(),
                    getGeofencePendingIntent()).setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG, "Security exception");
        }
    }

    public void addToGeofenceList(Location location) {

        mGeofenceList.add(new Geofence.Builder()
                .setRequestId("Car seat")
                .setCircularRegion(
                        location.getLatitude(),
                        location.getLongitude(),
                        Constants.GEOFENCE_RADIUS
                )
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
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
                    this, "Geofence added", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Error adding geofence");
        }
    }
}