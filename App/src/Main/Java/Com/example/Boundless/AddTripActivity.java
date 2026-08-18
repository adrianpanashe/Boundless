package com.example.boundless;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.content.ActivityNotFoundException;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

public class AddTripActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private GeofenceHelper geofenceHelper;

    private TextInputEditText etName, etSubtitle, etDescription;
    private TextView tvLocation;
    private ImageView imgPreview;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String currentImagePath = null;

    private int tripIdToEdit = -1;

    private ExecutorService executorService;
    private AppDatabase db;
    private TripDao tripDao;

    // Handle Camera Intent
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            imgPreview.setImageBitmap(imageBitmap);
                            currentImagePath = ImageUtils.saveImageToInternalStorage(this, imageBitmap);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        imgPreview.setImageBitmap(bitmap);
                        currentImagePath = ImageUtils.saveImageToInternalStorage(this, uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        etName = findViewById(R.id.et_name);
        etSubtitle = findViewById(R.id.et_subtitle);
        etDescription = findViewById(R.id.et_description);
        tvLocation = findViewById(R.id.tv_location);
        imgPreview = findViewById(R.id.img_preview);

        Button btnGetLocation = findViewById(R.id.btn_get_location);
        Button btnCaptureImage = findViewById(R.id.btn_capture_image);
        Button btnSelectGallery = findViewById(R.id.btn_select_gallery); // Assuming I'll add this to XML
        Button btnSave = findViewById(R.id.btn_save);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = AppDatabase.getInstance(this);
        tripDao = db.tripDao();
        executorService = Executors.newSingleThreadExecutor();
        geofenceHelper = new GeofenceHelper(this);

        tripIdToEdit = getIntent().getIntExtra("TRIP_ID", -1);
        if (tripIdToEdit != -1) {
            setTitle("Edit Trip");
            btnSave.setText("Update Trip");
            loadExistingTripData();
        } else {
            setTitle("Add New Trip");
            // Check if coordinates were passed from MapFragment
            double passedLat = getIntent().getDoubleExtra("LATITUDE", 0.0);
            double passedLng = getIntent().getDoubleExtra("LONGITUDE", 0.0);
            if (passedLat != 0.0 && passedLng != 0.0) {
                currentLatitude = passedLat;
                currentLongitude = passedLng;
                tvLocation.setText(String.format("Lat: %.4f, Lon: %.4f", currentLatitude, currentLongitude));
            }
        }

        btnGetLocation.setOnClickListener(v -> checkLocationPermissionAndGetLocation());

        btnCaptureImage.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                cameraLauncher.launch(takePictureIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        });

        if (imgPreview != null) {
            imgPreview.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        }

        btnSave.setOnClickListener(v -> saveTrip());
    }

    private void loadExistingTripData() {
        executorService.execute(() -> {
            Trip existingTrip = tripDao.getTripById(tripIdToEdit);
            if (existingTrip != null) {
                runOnUiThread(() -> {
                    etName.setText(existingTrip.getName());
                    etSubtitle.setText(existingTrip.getSubtitle());
                    etDescription.setText(existingTrip.getDescription());

                    currentLatitude = existingTrip.getLatitude();
                    currentLongitude = existingTrip.getLongitude();
                    if (currentLatitude != 0.0 || currentLongitude != 0.0) {
                        tvLocation.setText(String.format("Lat: %.4f, Lon: %.4f", currentLatitude, currentLongitude));
                    }

                    currentImagePath = existingTrip.getImagePath();
                    if (currentImagePath != null && !currentImagePath.isEmpty()) {
                        Bitmap bitmap = ImageUtils.loadImageFromPath(currentImagePath);
                        imgPreview.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                currentLatitude = location.getLatitude();
                                currentLongitude = location.getLongitude();
                                tvLocation.setText(String.format("Lat: %.4f, Lon: %.4f", currentLatitude, currentLongitude));
                            } else {
                                Toast.makeText(AddTripActivity.this, "Location not found. Make sure GPS is on.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String generateGradientPlaceholder() {
        int width = 800;
        int height = 600;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Dark, playful gradient (Deep Teal to Dark Navy/Purple)
        int colorStart = Color.parseColor("#00251a"); // Dark Teal
        int colorEnd = Color.parseColor("#311b92"); // Deep Purple

        LinearGradient gradient = new LinearGradient(
                0, 0, width, height,
                colorStart, colorEnd,
                Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setDither(true);
        paint.setShader(gradient);

        canvas.drawRect(0, 0, width, height, paint);

        return ImageUtils.saveImageToInternalStorage(this, bitmap);
    }

    private void saveTrip() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String subtitle = etSubtitle.getText() != null ? etSubtitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a trip name", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            Trip tripToSave;
            if (tripIdToEdit != -1) {
                tripToSave = tripDao.getTripById(tripIdToEdit);
                if (tripToSave == null) return;
            } else {
                tripToSave = new Trip();
            }

            tripToSave.setName(name);
            tripToSave.setSubtitle(subtitle);
            tripToSave.setDescription(description);
            tripToSave.setLatitude(currentLatitude);
            tripToSave.setLongitude(currentLongitude);

            // Apply the custom gradient placeholder if no image was taken
            if (currentImagePath == null || currentImagePath.isEmpty()) {
                currentImagePath = generateGradientPlaceholder();
            }
            tripToSave.setImagePath(currentImagePath);

            if (tripIdToEdit != -1) {
                tripDao.update(tripToSave);
                geofenceHelper.addGeofence(String.valueOf(tripIdToEdit), tripToSave.getLatitude(), tripToSave.getLongitude());
            } else {
                long newId = tripDao.insert(tripToSave);
                geofenceHelper.addGeofence(String.valueOf(newId), tripToSave.getLatitude(), tripToSave.getLongitude());
            }

            runOnUiThread(() -> {
                Toast.makeText(AddTripActivity.this, tripIdToEdit != -1 ? "Trip updated!" : "Trip saved!", Toast.LENGTH_SHORT).show();
                finish(); // Close activity and return
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
