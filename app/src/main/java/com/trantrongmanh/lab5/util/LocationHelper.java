package com.trantrongmanh.lab5.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Quản lý quyền và lấy vị trí GPS bằng FusedLocationProviderClient.
 */
public class LocationHelper {

    public static final int REQUEST_LOCATION_PERMISSION = 1001;

    public interface LocationCallback2 {
        void onLocationReceived(double lat, double lon, String cityName);
        void onLocationError(String message);
    }

    private static final String TAG = "LocationHelper";

    private final Context context;
    private final FusedLocationProviderClient fusedClient;

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }

    // ── Kiểm tra quyền ────────────────────────────────────────────────────────

    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION_PERMISSION);
    }

    // ── Lấy vị trí hiện tại ───────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    public void getCurrentLocation(LocationCallback2 callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Chưa cấp quyền vị trí");
            return;
        }

        // Thử lấy vị trí cuối cùng trước (nhanh hơn)
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.d(TAG, "Last location: " + location.getLatitude() + ", " + location.getLongitude());
                String city = getCityName(location.getLatitude(), location.getLongitude());
                callback.onLocationReceived(location.getLatitude(), location.getLongitude(), city);
            } else {
                // Không có vị trí cuối, yêu cầu vị trí mới
                requestFreshLocation(callback);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "getLastLocation failed: " + e.getMessage());
            requestFreshLocation(callback);
        });
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation(LocationCallback2 callback) {
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(1)
                .build();

        fusedClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                fusedClient.removeLocationUpdates(this);
                if (result != null && result.getLastLocation() != null) {
                    double lat = result.getLastLocation().getLatitude();
                    double lon = result.getLastLocation().getLongitude();
                    Log.d(TAG, "Fresh location: " + lat + ", " + lon);
                    String city = getCityName(lat, lon);
                    callback.onLocationReceived(lat, lon, city);
                } else {
                    callback.onLocationError("Không lấy được vị trí");
                }
            }
        }, Looper.getMainLooper());
    }

    // ── Geocoding: tọa độ → tên thành phố ────────────────────────────────────

    public String getCityName(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(context, new Locale("vi", "VN"));
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Ưu tiên: locality (thành phố) → subAdminArea → adminArea
                String city = address.getLocality();
                if (city == null || city.isEmpty()) city = address.getSubAdminArea();
                if (city == null || city.isEmpty()) city = address.getAdminArea();
                if (city == null || city.isEmpty()) city = address.getCountryName();

                Log.d(TAG, "City resolved: " + city);
                return city != null ? city : "Unknown";
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
        }
        return "Unknown";
    }

    /**
     * Trả về tên thành phố dạng tiếng Anh (dùng cho API query).
     */
    public String getCityNameEnglish(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                if (city == null || city.isEmpty()) city = address.getSubAdminArea();
                if (city == null || city.isEmpty()) city = address.getAdminArea();
                return city != null ? city : "Ho Chi Minh City";
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder EN error: " + e.getMessage());
        }
        return "Ho Chi Minh City";
    }
}
