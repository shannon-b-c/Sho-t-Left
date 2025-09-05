package com.example.shotleft;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;

public class OptimalRoutesActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "OptimalRoutesActivity";
    private GoogleMap myMap;
    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private LinearLayout routesContainer;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location currentLocation;
    private final int FINE_PERMISSION_CODE = 1;
    private final ArrayList<Polyline> activePolylines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimal_routes);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Bottom sheet setup
        bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        routesContainer = bottomSheet.findViewById(R.id.routes_container);
        bottomSheetBehavior.setPeekHeight(100);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setHideable(false);

        bottomSheet.findViewById(R.id.close_button).setOnClickListener(v ->
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        // Map setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
        else Toast.makeText(this, "Map not found", Toast.LENGTH_LONG).show();

        getLastLocation();
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                Log.d(TAG, "Current location: " + location.getLatitude() + "," + location.getLongitude());
            }
            fetchOptimalRoutes();
        });
    }

    private void fetchOptimalRoutes() {
        if (currentLocation == null) {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("start_lon", 27.9311);
            jsonBody.put("start_lat", -26.2572);
            // Example destination: Replace with user input or selection
            jsonBody.put("dest_lon", 28.0472);
            jsonBody.put("dest_lat", -26.2052);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error: " + e.getMessage());
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        String anon_key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4";

        Request request = new Request.Builder()
                .url("https://umzxebhgucvpariulruy.supabase.co/rest/v1/rpc/optimal_routes")
                .post(body)
                .addHeader("apikey", anon_key)
                .addHeader("Authorization", "Bearer "+ anon_key)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Optimal routes request failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(OptimalRoutesActivity.this, "Failed to fetch routes", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server error: " + response.code());
                    runOnUiThread(() -> Toast.makeText(OptimalRoutesActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                final String responseData = response.body().string();
                runOnUiThread(() -> {
                    try {
                        displayRoutes(responseData);
                        Log.d("Supabase", responseData);
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(OptimalRoutesActivity.this, "Error parsing routes", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void displayRoutes(String json) throws JSONException {
        // Clear previous
        for (Polyline line : activePolylines) line.remove();
        activePolylines.clear();
        routesContainer.removeAllViews();

        JSONArray arr = new JSONArray(json);
        if (arr.length() == 0) {
            routesContainer.setVisibility(View.GONE);
            return;
        }
        routesContainer.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < arr.length(); i++) {
            JSONObject route = arr.getJSONObject(i);

            String startName = route.optString("start_rank_name", "Unknown start");
            String destName  = route.optString("dest_rank_name", "Unknown end");
            double fare      = route.optDouble("total_fare", Double.NaN);
            double startDist = route.optDouble("start_distance_m", Double.NaN) / 1000.0;
            double destDist  = route.optDouble("dest_distance_m", Double.NaN) / 1000.0;

            View routeView = inflater.inflate(R.layout.route_item_layout, routesContainer, false);
            TextView title = routeView.findViewById(R.id.item_route_title);
            TextView fareTv = routeView.findViewById(R.id.item_route_fare);
            TextView distTv = routeView.findViewById(R.id.item_route_distance);

            title.setText(startName + " → " + destName);
            if (!Double.isNaN(fare)) fareTv.setText(String.format("Fare: R%.2f", fare));
            if (!Double.isNaN(startDist) && !Double.isNaN(destDist))
                distTv.setText(String.format("Start dist: %.2f km | Dest dist: %.2f km", startDist, destDist));

            routesContainer.addView(routeView);

            if (route.has("route_geom")) {
                try {
                    JSONObject geom = route.getJSONObject("route_geom");
                    if ("LineString".equalsIgnoreCase(geom.optString("type"))) {
                        JSONArray coords = geom.getJSONArray("coordinates");
                        drawLineStringOnMap(coords);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void drawLineStringOnMap(JSONArray coordinates) throws JSONException {
        if (myMap == null || coordinates.length() == 0) return;

        PolylineOptions opts = new PolylineOptions().width(6f).geodesic(true);
        for (int i = 0; i < coordinates.length(); i++) {
            JSONArray pair = coordinates.getJSONArray(i);
            double lon = pair.getDouble(0);
            double lat = pair.getDouble(1);
            opts.add(new LatLng(lat, lon));
        }

        Polyline polyline = myMap.addPolyline(opts);
        activePolylines.add(polyline);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
        }

        if (currentLocation != null) {
            LatLng cur = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cur, 12f));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) getLastLocation();
            else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
