package com.example.ethanmorris.smartcarseatapp;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.Context;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

// This geofencing service was based on the resources/guide found on developer.android.com

public class GeofenceIntentService extends IntentService {

    protected static final String TAG = "GeofenceIntentService";

    public GeofenceIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    // This method handles the Geofence enter/exit transitions with a notification and exit events
    // with an alarm
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofenceEvent = GeofencingEvent.fromIntent(intent);
        if (geofenceEvent.hasError()) {
            Log.e(TAG, "Geofencing error");
            return;
        }

        int geofenceTransition = geofenceEvent.getGeofenceTransition();
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            List<Geofence> triggeringGeofences = geofenceEvent.getTriggeringGeofences();

            String transitionInfo = getTransitionInfo(this, geofenceTransition, triggeringGeofences);

            notifyUser(transitionInfo);
            Log.i(TAG, transitionInfo);

            // Trigger an alarm on a geofence exit

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
                Intent alarmIntent = new Intent(this, AlarmReceiverActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 99, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager alarmManager = (AlarmManager) getSystemService(Activity.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() - 1, pendingIntent);
            }
        }
    }

    // This method creates the user notification for geofence events
    private void notifyUser(String notificationInfo) {

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(intent);

        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationInfo)
                .setContentText("Return to app")
                .setContentIntent(pendingIntent);

        builder.setAutoCancel(true);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, builder.build());
    }

    // This method sets a geofence trigger message with the corresponding geofence ID that was triggered
    private String getTransitionInfo(Context context, int geofenceTransition, List<Geofence> triggeringGeofences) {

        String geofenceTransitionInfo;
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            geofenceTransitionInfo = "Vehicle parked. Please secure your child.";
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            geofenceTransitionInfo = "WARNING! Child has been left behind in your car!";
        } else {
            geofenceTransitionInfo = "Transition not specified";
        }

        // Retrieve ID of geofence with active transition events
        ArrayList triggeringGeofenceIDList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofenceIDList.add(geofence.getRequestId());
        }
        String geofenceID = TextUtils.join(", ", triggeringGeofenceIDList);
        String result = geofenceID + ": " + geofenceTransitionInfo;
        return result;
    }
}

