package com.example.boundless;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;
import java.util.concurrent.Executors;

public class MapActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Required OSMDroid setup - must be before setContentView
        Configuration.getInstance().load(this,
                PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        // Hook up Add Trip button → launches AddTripActivity
        MaterialButton btnAddTrip = findViewById(R.id.btn_add_new_trip_map);
        btnAddTrip.setOnClickListener(v ->
                startActivity(new Intent(MapActivity.this, AddTripActivity.class)));

        // Set up the OSMDroid map
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);

        checkAndRequestPermissions();
    }

    // Sets up the live blue-dot location overlay on the map
    private void initLocationOverlay() {
        locationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(this), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation(); // Camera follows user
        mapView.getOverlays().add(locationOverlay);

        // Start FusedLocation tracking for accuracy + battery efficiency
        locationHelper = new LocationHelper(this);

        // Move camera to last known position immediately
        locationHelper.getLastLocation((lat, lng) -> {
            GeoPoint point = new GeoPoint(lat, lng);
            mapView.getController().animateTo(point);
        });

        // Keep updating as user moves
        locationHelper.startTracking((lat, lng) -> {
            // Location updates handled by overlay automatically
            // This callback is available if you need current coords elsewhere
        });

        // Load saved trip markers from the database
        loadSavedTripMarkers();
    }

    // Reads all saved trips from Room DB and drops markers on the map
    private void loadSavedTripMarkers() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Trip> trips = db.tripDao().getAllTrips();

            runOnUiThread(() -> {
                for (Trip trip : trips) {
                    // Only add marker if trip has valid coordinates
                    if (trip.getLatitude() != 0 && trip.getLongitude() != 0) {
                        GeoPoint point = new GeoPoint(
                                trip.getLatitude(), trip.getLongitude());

                        Marker marker = new Marker(mapView);
                        marker.setPosition(point);
                        marker.setTitle(trip.getName());
                        marker.setSubDescription(trip.getSubtitle());
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        // Tap marker → navigate to that trip
                        marker.setOnMarkerClickListener((m, mapV) -> {
                            NavigationHelper.navigateTo(
                                    MapActivity.this,
                                    trip.getLatitude(),
                                    trip.getLongitude(),
                                    trip.getName());
                            return true;
                        });

                        mapView.getOverlays().add(marker);
                    }
                }
                mapView.invalidate(); // Refresh map to show markers
            });
        });
    }

    // Check location permissions and request if not granted
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, start tracking immediately
            initLocationOverlay();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocationOverlay();
            } else {
                Toast.makeText(this,
                        "Location permission is required to show your position.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // Refresh markers when returning from AddTripActivity
        if (locationOverlay != null) loadSavedTripMarkers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        // Stop GPS updates to save battery when app is in background
        if (locationHelper != null) locationHelper.stopTracking();
    }
}


