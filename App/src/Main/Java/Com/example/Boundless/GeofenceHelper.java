package com.example.boundless;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class GeofenceHelper {

    private static final String TAG = "GeofenceHelper";
    private static final float GEOFENCE_RADIUS_IN_METERS = 100;
    // 1 hour
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE;

    private GeofencingClient geofencingClient;
    private Context context;
    private PendingIntent geofencePendingIntent;

    public GeofenceHelper(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    public void addGeofence(String geofenceId, double latitude, double longitude) {
        if (latitude == 0.0 && longitude == 0.0) {
            Log.d(TAG, "Latitude and Longitude are 0. Skipping geofence creation.");
            return;
        }

        Geofence geofence = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(geofenceId)

                // Set the circular region of this geofence.
                .setCircularRegion(
                        latitude,
                        longitude,
                        GEOFENCE_RADIUS_IN_METERS
                )

                // Set the expiration duration of the geofence.
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)

                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
                // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
                // is already inside that geofence.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                // Add the geofence to be monitored by geofencing service.
                .addGeofence(geofence)
                .build();

        try {
            geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence Added: " + geofenceId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add geofence", e));
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing", e);
        }
    }

    public void removeGeofence(String geofenceId) {
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(geofenceId);

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence Removed: " + geofenceId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to remove geofence", e));
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        return geofencePendingIntent;
    }
}
