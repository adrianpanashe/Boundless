package com.example.boundless;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private TripDao tripDao;
    private ExecutorService executorService;
    private GeofenceHelper geofenceHelper;
    private int tripId = -1;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_trip_detail);

        mapView = findViewById(R.id.map_detail);
        if (mapView != null) {
            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setMultiTouchControls(true);
            mapView.getController().setZoom(15.0);
        }

        // Database
        db = AppDatabase.getInstance(this);
        tripDao = db.tripDao();
        executorService = Executors.newSingleThreadExecutor();
        geofenceHelper = new GeofenceHelper(this);

        // Find views
        Toolbar toolbar = findViewById(R.id.toolbar);
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        ImageView headerImage = findViewById(R.id.img_detail_header);
        TextView descriptionText = findViewById(R.id.tv_detail_description);
        TextView subtitleText = findViewById(R.id.tv_detail_subtitle);
        TextView locationText = findViewById(R.id.tv_detail_location);
        FloatingActionButton fabDelete = findViewById(R.id.fab_delete_trip);
        FloatingActionButton fabEdit = findViewById(R.id.fab_edit_trip);

        // Set up the toolbar so the back button works natively
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get the data passed from the Adapter
        tripId = getIntent().getIntExtra("TRIP_ID", -1);
        String tripName = getIntent().getStringExtra("TRIP_NAME");
        String tripDesc = getIntent().getStringExtra("TRIP_DESC");
        String tripImagePath = getIntent().getStringExtra("TRIP_IMAGE_PATH");

        // Set basic initial data (sync)
        collapsingToolbar.setTitle(tripName);
        descriptionText.setText(tripDesc);

        if (tripImagePath != null && !tripImagePath.isEmpty()) {
            Bitmap bitmap = ImageUtils.loadImageFromPath(tripImagePath);
            headerImage.setImageBitmap(bitmap);
        } else {
            int tripImageId = getIntent().getIntExtra("TRIP_IMAGE", R.drawable.ic_launcher_background);
            headerImage.setImageResource(tripImageId);
        }

        // Fetch remaining detailed data from database using the ID
        if (tripId != -1) {
            loadTripData(collapsingToolbar, headerImage, descriptionText, subtitleText, locationText);
        }

        fabDelete.setOnClickListener(v -> deleteTrip());
        fabEdit.setOnClickListener(v -> editTrip());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tripId != -1) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
            ImageView headerImage = findViewById(R.id.img_detail_header);
            TextView descriptionText = findViewById(R.id.tv_detail_description);
            TextView subtitleText = findViewById(R.id.tv_detail_subtitle);
            TextView locationText = findViewById(R.id.tv_detail_location);

            loadTripData(collapsingToolbar, headerImage, descriptionText, subtitleText, locationText);
        }
    }

    private void loadTripData(CollapsingToolbarLayout collapsingToolbar, ImageView headerImage, TextView descriptionText, TextView subtitleText, TextView locationText) {
        executorService.execute(() -> {
            Trip currentTrip = tripDao.getTripById(tripId);
            if (currentTrip != null) {
                runOnUiThread(() -> {
                    collapsingToolbar.setTitle(currentTrip.getName());
                    descriptionText.setText(currentTrip.getDescription());
                    subtitleText.setText(currentTrip.getSubtitle());

                    if (currentTrip.getLatitude() != 0.0 || currentTrip.getLongitude() != 0.0) {
                        locationText.setText(String.format("Location: %.4f, %.4f", currentTrip.getLatitude(), currentTrip.getLongitude()));
                        updateMiniMap(currentTrip.getLatitude(), currentTrip.getLongitude(), currentTrip.getName());
                    } else {
                        locationText.setText("Location: N/A");
                    }

                    if (currentTrip.getImagePath() != null && !currentTrip.getImagePath().isEmpty()) {
                        Bitmap bitmap = ImageUtils.loadImageFromPath(currentTrip.getImagePath());
                        headerImage.setImageBitmap(bitmap);
                    } else {
                        headerImage.setImageResource(currentTrip.getImageResourceId() != 0 ? currentTrip.getImageResourceId() : R.drawable.ic_launcher_background);
                    }
                });
            }
        });
    }

    private void editTrip() {
        if (tripId != -1) {
            Intent intent = new Intent(this, AddTripActivity.class);
            intent.putExtra("TRIP_ID", tripId);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Cannot edit dummy trip directly.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTrip() {
        if (tripId != -1) {
            executorService.execute(() -> {
                Trip tripToDelete = tripDao.getTripById(tripId);
                if (tripToDelete != null) {
                    tripDao.delete(tripToDelete);
                    geofenceHelper.removeGeofence(String.valueOf(tripId));
                    runOnUiThread(() -> {
                        Toast.makeText(TripDetailActivity.this, "Trip deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
        } else {
            Toast.makeText(this, "Cannot delete dummy trip directly.", Toast.LENGTH_SHORT).show();
        }
    }

    // This makes the back arrow in the toolbar work
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void updateMiniMap(double lat, double lng, String name) {
        if (mapView == null) return;
        GeoPoint point = new GeoPoint(lat, lng);
        mapView.getController().setCenter(point);

        mapView.getOverlays().clear();
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(name);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}