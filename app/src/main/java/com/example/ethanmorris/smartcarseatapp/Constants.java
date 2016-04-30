package com.example.ethanmorris.smartcarseatapp;

public final class Constants {

    private Constants() {}

    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 2;                  // 2 hours
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;

    // The radius below is in meters. The Nexus 4 device used in testing seems to be accurate only to
    // about 100 feet. Newer devices seem to be accurate to 30-40 feet, but this is set to approx 100 feet
    // for now to accommodate for the hardware that we have on loan from the CSCE department
    public static final float GEOFENCE_RADIUS = 10;

    public static final String SHARED_PREFERENCES_NAME = "com.google.android.gms.location.Geofence.SHARED_PREFERENCES_NAME";
    public static final String GEOFENCES_ADDED_KEY = "com.google.android.gms.location.Geofence.GEOFENCES_ADDED_KEY";

}
