package com.example.boundless;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
public class LocationHelper {
    private final FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private final Context context;

    public interface LocationUpdateListener{
        void onLocationUpdated(double lat, double lng);
    }
    public LocationHelper(Context context){
        this.context = context;
        fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void startTracking(LocationUpdateListener listener){
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult result){
                if(result!=null && result.getLastLocation() != null){
                    double lat = result.getLastLocation().getLatitude();
                    double lng = result.getLastLocation().getLongitude();
                    listener.onLocationUpdated(lat, lng);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        }
    }
    public void stopTracking(){
        if (locationCallback !=null){
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }
    public void getLastLocation(LocationUpdateListener listener){
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                listener.onLocationUpdated(
                        location.getLatitude(),
                        location.getLongitude());
            }
        });

    }
}

