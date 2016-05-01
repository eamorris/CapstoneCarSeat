package com.example.ethanmorris.smartcarseatapp;

public final class Constants {

    private Constants() {}

    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 2;                  // 2 hours
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;

    // The radius below is in meters.
    public static final float GEOFENCE_RADIUS = 20;

    public static final String SHARED_PREFERENCES_NAME = "com.google.android.gms.location.Geofence.SHARED_PREFERENCES_NAME";
    public static final String GEOFENCES_ADDED_KEY = "com.google.android.gms.location.Geofence.GEOFENCES_ADDED_KEY";

}
