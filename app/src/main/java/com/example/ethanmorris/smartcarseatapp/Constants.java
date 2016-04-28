package com.example.ethanmorris.smartcarseatapp;

public final class Constants {

    private Constants() {

    }

    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 2;                  // 2 hours
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    public static final float GEOFENCE_RADIUS = 30;                     // approximately 32 feet

    public static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";
    public static final String SHARED_PREFERENCES_NAME = "com.google.android.gms.location.Geofence.SHARED_PREFERENCES_NAME";
    public static final String GEOFENCES_ADDED_KEY = "com.google.android.gms.location.Geofence.GEOFENCES_ADDED_KEY";

}
