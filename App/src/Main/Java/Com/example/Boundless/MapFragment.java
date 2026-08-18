package com.example.boundless;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private LocationHelper locationHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Required OSMDroid setup
        Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);

        // Long press to add trip
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                Intent intent = new Intent(getActivity(), AddTripActivity.class);
                intent.putExtra("LATITUDE", p.getLatitude());
                intent.putExtra("LONGITUDE", p.getLongitude());
                startActivity(intent);
                return true;
            }
        };

        MapEventsOverlay MapEventsOverlay = new MapEventsOverlay(mReceive);
        mapView.getOverlays().add(MapEventsOverlay);

        checkAndRequestPermissions();

        return view;
    }

    private void initLocationOverlay() {
        if (getContext() == null) return;
        locationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(getContext()), mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        mapView.getOverlays().add(locationOverlay);

        locationHelper = new LocationHelper(getContext());

        locationHelper.getLastLocation((lat, lng) -> {
            GeoPoint point = new GeoPoint(lat, lng);
            mapView.getController().animateTo(point);
        });

        locationHelper.startTracking((lat, lng) -> {});

        loadSavedTripMarkers();
    }


    public Bitmap getRoundedBitmap(Bitmap bitmap, int radius) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);

        // Draw rounded rectangle
        canvas.drawRoundRect(rectF, radius, radius, paint);

        // Set blending mode
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // Draw bitmap inside rounded shape
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private void loadSavedTripMarkers() {
        if (getContext() == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            List<Trip> trips = db.tripDao().getAllTrips();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    for (Trip trip : trips) {
                        if (trip.getLatitude() != 0 && trip.getLongitude() != 0) {
                            GeoPoint point = new GeoPoint(trip.getLatitude(), trip.getLongitude());
                            Marker marker = new Marker(mapView);
                            marker.setPosition(point);
                            marker.setTitle(trip.getName());
                            marker.setSubDescription(trip.getSubtitle());
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                            if (trip.getImagePath() != null && !trip.getImagePath().isEmpty()) {
                                Bitmap bitmap = ImageUtils.loadImageFromPath(trip.getImagePath());
                                if (bitmap != null) {

                                    // Scale down for marker
                                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);

                                    // Create rounded bitmap
                                    Bitmap roundedBitmap = getRoundedBitmap(scaledBitmap, 20); // 20 = corner radius

                                    android.graphics.drawable.BitmapDrawable drawable =
                                            new android.graphics.drawable.BitmapDrawable(getResources(), roundedBitmap);

                                    marker.setIcon(drawable);
                                }
                            }

                            marker.setOnMarkerClickListener((m, mapV) -> {
                                NavigationHelper.navigateTo(requireContext(), trip.getLatitude(), trip.getLongitude(), trip.getName());
                                return true;
                            });

                            mapView.getOverlays().add(marker);
                        }
                    }
                    mapView.invalidate();
                });
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        } else {
            initLocationOverlay();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocationOverlay();
            } else {
                Toast.makeText(getContext(), "Location permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (locationOverlay != null) loadSavedTripMarkers();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (locationHelper != null) locationHelper.stopTracking();
    }
}
