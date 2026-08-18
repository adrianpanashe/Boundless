package com.example.boundless;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";
    private static final String CHANNEL_ID = "GEOFENCE_ALERTS";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "GeofenceBroadcastReceiver onReceive called!");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "GeofencingEvent has error: " + geofencingEvent.getErrorCode());
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            for (Geofence geofence : triggeringGeofences) {
                String requestId = geofence.getRequestId();
                Log.i(TAG, "Entered geofence ID: " + requestId);

                try {
                    int tripId = Integer.parseInt(requestId);
                    fetchTripAndNotify(context, tripId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed to parse trip ID from Geofence requestId: " + requestId);
                }
            }
        } else {
            // Log the error.
            Log.e(TAG, "Invalid geofence transition type: " + geofenceTransition);
        }
    }

    private void fetchTripAndNotify(Context context, int tripId) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            Trip trip = db.tripDao().getTripById(tripId);

            if (trip != null) {
                sendNotification(context, trip);
            } else {
                Log.e(TAG, "Trip not found in DB for ID: " + tripId);
            }
        });
    }

    private void sendNotification(Context context, Trip trip) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Geofence Alerts";
            String description = "Alerts when you are near a saved trip location.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(context, TripDetailActivity.class);
        intent.putExtra("TRIP_ID", trip.getId());
        intent.putExtra("TRIP_NAME", trip.getName());
        intent.putExtra("TRIP_DESC", trip.getDescription());
        intent.putExtra("TRIP_IMAGE_PATH", trip.getImagePath());
        intent.putExtra("TRIP_IMAGE", trip.getImageResourceId());

        // Add FLAG_ACTIVITY_NEW_TASK since we're starting it from a receiver
        // And FLAG_ACTIVITY_CLEAR_TOP so we don't pile up detail activities
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, trip.getId(), intent, pendingIntentFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round) // Replace with your app icon
                .setContentTitle("You're near " + trip.getName() + "!")
                .setContentText(trip.getSubtitle() != null && !trip.getSubtitle().isEmpty() ? trip.getSubtitle() : "Tap to view details.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(trip.getId(), builder.build());
    }
}
