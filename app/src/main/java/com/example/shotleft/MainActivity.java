package com.example.shotleft;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap myMap;
    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView rankName, rankLatitude, rankLongitude, rankDistance;
    private LinearLayout routesContainer;
    private Map<Marker, JSONObject> markerDataMap = new HashMap<>();
    private static final String TAG = "MainActivity";

    // Location-related variables
    private final int FINE_PERMISSION_CODE = 1;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private ArrayList<LatLng> rankLocations = new ArrayList<>();

    // Store routes information - now using List for multiple routes
    private Map<String, List<JSONObject>> rankRoutesMap = new HashMap<>();

    private final List<com.google.android.gms.maps.model.Polyline> activePolylines = new ArrayList<>();

    OkHttpClient client = new OkHttpClient();
    private String responseData;
    private String userLocationName;
    private ArrayList<String> rankArray;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: Starting initialization");
        CardView searchBarButton= findViewById(R.id.searchBarButton);

        searchBarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Convert user location to address
                        String userLocationName = GeocodingUtils.getAddressFromLatLng(
                                MainActivity.this,
                                currentLocation.getLatitude(),
                                currentLocation.getLongitude()
                        );

                        // Switch back to UI thread for navigation
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, Search_Activity.class);
                                intent.putExtra("Current_Location_lat", currentLocation.getLatitude());
                                intent.putExtra("Current_Location_lng", currentLocation.getLongitude());
                                intent.putExtra("Current_Location_Name", userLocationName);
                                intent.putExtra("Ranks", rankArray);
                                startActivity(intent);
                            }
                        });
                    }
                }).start(); // <-- THIS WAS MISSING
            }
        });
        // Initialize location provider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            // Initialize bottom sheet
            bottomSheet = findViewById(R.id.bottom_sheet);
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            rankName = bottomSheet.findViewById(R.id.rank_name);
            rankDistance = bottomSheet.findViewById(R.id.rank_distance);
            routesContainer = bottomSheet.findViewById(R.id.routes_container);

            // Set up bottom sheet behavior
            bottomSheetBehavior.setPeekHeight(200);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior.setHideable(false);

            // Set up close button
            View closeButton = bottomSheet.findViewById(R.id.close_button);
            closeButton.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
                Log.d(TAG, "Map fragment found and getting map async");
            } else {
                Log.e(TAG, "Map fragment is null");
                Toast.makeText(this, "Map fragment not found", Toast.LENGTH_LONG).show();
            }

            // First fetch routes information, then get location and rank data
            fetchRoutesInformation();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    Log.d(TAG, "Current location: " + location.getLatitude() + ", " + location.getLongitude());
                }
                // Fetch rank data regardless of whether we have location
                fetchRankData();
            }
        });
    }

    private void fetchRoutesInformation() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://umzxebhgucvpariulruy.supabase.co/rest/v1/routes_from_rank")
                .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Routes API call failed: " + e.getMessage());
                // Continue with location and rank data even if routes fail
                getLastLocation();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Routes API response not successful: " + response.code());
                    // Continue with location and rank data even if routes fail
                    getLastLocation();
                    return;
                }

                final String responseData = response.body().string();
                Log.d(TAG, "Routes API response received: " + responseData);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            processRoutesJSON(responseData);
                        } catch (JSONException e) {
                            Log.e(TAG, "Routes JSON parsing error: " + e.getMessage());
                            // Continue with location and rank data even if routes parsing fails
                            getLastLocation();
                        }
                    }
                });
            }
        });
    }

    private void processRoutesJSON(String json) throws JSONException {
        try {
            JSONArray ja = new JSONArray(json);
            Log.d(TAG, "Processing " + ja.length() + " route items");

            for (int i = 0; i < ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);

                // Extract route information
                String rankId = jo.optString("rank_id", "");

                if (!rankId.isEmpty()) {
                    // Initialize the list if it doesn't exist
                    if (!rankRoutesMap.containsKey(rankId)) {
                        rankRoutesMap.put(rankId, new ArrayList<>());
                    }

                    // Add the route to the list for this rank
                    rankRoutesMap.get(rankId).add(jo);
                }
            }

            Log.d(TAG, "Loaded routes for " + rankRoutesMap.size() + " ranks");
        } catch (Exception e) {
            Log.e(TAG, "Error processing routes JSON: " + e.getMessage());
        } finally {
            // Always proceed to get location and rank data
            getLastLocation();
        }
    }

    private void fetchRankData() {
        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("https://umzxebhgucvpariulruy.supabase.co/rest/v1/ranks")
                    .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4")
                    .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "API call failed: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "API response not successful: " + response.code());
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show());
                        return;
                    }

                    final String responseData = response.body().string();
                    Log.d(TAG, "API response received: " + responseData);

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d("Supabase", responseData);
                                //just to see the query results in the logcat
                                processJSON(responseData);
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                Toast.makeText(MainActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchRankData: " + e.getMessage());
            Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        myMap = googleMap;
        myMap.setOnMarkerClickListener(this);

        // Enable current location if permission granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);  // ✅ enables the blue dot
        }

        LatLng cur = new LatLng(-26, 28);
        if(currentLocation != null){
            cur = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        myMap.addMarker(new MarkerOptions().position(cur).title("My Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        float closerZoom = 12.0f; // Adjust this value for desired zoom
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cur, closerZoom));
        //googleMap.move... or  myMap.move...
                Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        //addRanks(); //need to check where else it could've been called

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
        if(requestCode == FINE_PERMISSION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastLocation();
            }else{
                Toast.makeText(this, "Location permission is denied, please allow the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processJSON(String json) throws JSONException {
        try {
            JSONArray ja = new JSONArray(json);
            Log.d(TAG, "Processing " + ja.length() + " items");

            // Clear previous data
            rankLocations.clear();
            markerDataMap.clear();
            if (myMap != null) {
                myMap.clear();
            }

            // Add current location marker if available
            if (currentLocation != null && myMap != null) {
                LatLng cur = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                myMap.addMarker(new MarkerOptions()
                        .position(cur)
                        .title("My Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }

            for (int i = 0; i < ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                String name = jo.getString("rank_name");
                double lat = jo.getDouble("rank_latitude");
                double lon = jo.getDouble("rank_longitude");
                String rankId = jo.optString("rank_id", "");
                jo.put("rank_id_ui", rankId);

                // Calculate distance if we have current location
                double distance = 0.0;
                if (currentLocation != null) {
                    Location rankLocation = new Location("");
                    rankLocation.setLatitude(lat);
                    rankLocation.setLongitude(lon);
                    distance = currentLocation.distanceTo(rankLocation) / 1000.0; // Convert to km
                    jo.put("distance", distance);
                }

                // Add routes information if available
                if (!rankId.isEmpty() && rankRoutesMap.containsKey(rankId)) {
                    // Store the list of routes as a JSONArray in the rank data
                    JSONArray routesArray = new JSONArray(rankRoutesMap.get(rankId));
                    jo.put("routes", routesArray);
                }

                // Add marker to map
                LatLng location = new LatLng(lat, lon);
                rankLocations.add(location);

                if (myMap != null) {
                    Marker marker = myMap.addMarker(new MarkerOptions()
                            .position(location)
                            .title(name)
                            .snippet("Distance: " + String.format("%.2f km", distance)));

                    // Store the data with the marker
                    markerDataMap.put(marker, jo);
                }
            }

            // Adjust camera to show nearest ranks if we have location
            if (currentLocation != null && !rankLocations.isEmpty() && myMap != null) {
                ArrayList<LatLng> nearestRanks = getNearestRanks(currentLocation, rankLocations);
                adjustCameraToNearestRanks(currentLocation, nearestRanks);
            }

            if (ja.length() == 0) {
                Toast.makeText(this, "No ranks found", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, ja.length() + " ranks loaded", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing JSON: " + e.getMessage());
            Toast.makeText(this, "Error processing data", Toast.LENGTH_SHORT).show();
        }
    }

    private void adjustCameraToNearestRanks(Location currentLocation, ArrayList<LatLng> nearestRanks) {
        if (myMap == null || nearestRanks.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        for(LatLng rank : nearestRanks){
            builder.include(rank);
        }

        int padding = 150; //pixels
        myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
    }
    private ArrayList<LatLng> getNearestRanks(Location currentLocation, ArrayList<LatLng> allRanks){
        if (currentLocation == null) {
            Log.e("getNearestRanks", "User location is null, skipping nearest rank calculation");
            return new ArrayList<>();
        }
        allRanks.sort((a, b) ->
                Double.compare(distanceBetween(currentLocation, a), distanceBetween(currentLocation, b)));

        ArrayList<LatLng> nearest = new ArrayList<>();
        for (int i = 0; i < Math.min(3, allRanks.size()); i++) {
            nearest.add(allRanks.get(i));
        }
        return nearest;
    }

    private double distanceBetween(Location currentLocation, LatLng rank){
        Location loc = new Location("");
        loc.setLatitude(rank.latitude);
        loc.setLongitude(rank.longitude);
        return currentLocation.distanceTo(loc);
    }

    private void fetchAndShowRoutesForRank(String rankName) {
        OkHttpClient client = new OkHttpClient();

        String jsonBody = "{\"p_rank_name\":\"" + rankName.replace("\"","\\\"") + "\"}";
        okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url("https://umzxebhgucvpariulruy.supabase.co/rest/v1/rpc/routes_from_rank")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "routes_from_rank failed: " + e.getMessage());
                runOnUiThread(() -> {
                    routesContainer.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Could not load routes", Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "routes_from_rank HTTP " +response.code());
                    runOnUiThread(() -> {
                        routesContainer.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Server error loading routes", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                final String json = response.body().string();
                runOnUiThread(() -> {
                    try {
                        showRoutesInBottomSheet(json);
                    } catch (JSONException ex) {
                        Log.e(TAG, "routes_from_rank JSON error: " + ex.getMessage());
                        routesContainer.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void showRoutesInBottomSheet(String json) throws JSONException {
        // Clear previous polylines
        for (com.google.android.gms.maps.model.Polyline line : activePolylines) {
            line.remove();
        }
        activePolylines.clear();
        routesContainer.removeAllViews();

        JSONArray arr = new JSONArray(json);
        if (arr.length() == 0) {
            routesContainer.setVisibility(View.GONE);
            return;
        }

        TextView title = new TextView(this);
        title.setText("Available Routes:");
        title.setTextSize(16);
        title.setPadding(0, 16, 0, 8);
        routesContainer.addView(title);

        // Clear previous route polylines if you keep references (optional)

        for (int i = 0; i < arr.length(); i++) {
            JSONObject route = arr.getJSONObject(i);

            String start = route.optString("start_rank_name", "Unknown start");
            String end   = route.optString("end_rank_name", "Unknown end");
            double fare  = route.optDouble("route_fare", Double.NaN);

            TextView tv = new TextView(this);
            String line = start + " \u2192 " + end; // arrow
            if (!Double.isNaN(fare)) line += "  (Fare: " + String.format("R%.2f", fare) + ")";
            tv.setText(line);
            tv.setTextSize(14);
            tv.setPadding(16, 8, 16, 8);
            routesContainer.addView(tv);

            // Draw the route if geometry exists
            if (route.has("route_geom")) {
                try {
                    JSONObject geom = route.getJSONObject("route_geom");
                    if ("LineString".equalsIgnoreCase(geom.optString("type"))) {
                        JSONArray coords = geom.getJSONArray("coordinates");
                        drawLineStringOnMap(coords);
                    }
                } catch (Exception ignored) { }
            }
        }

        routesContainer.setVisibility(View.VISIBLE);
    }

    private void drawLineStringOnMap(JSONArray coordinates) throws JSONException {
        if (myMap == null || coordinates.length() == 0) return;

        com.google.android.gms.maps.model.PolylineOptions opts = new com.google.android.gms.maps.model.PolylineOptions()
                .width(6f)
                .geodesic(true); // makes it bend naturally

        for (int i = 0; i < coordinates.length(); i++) {
            JSONArray pair = coordinates.getJSONArray(i);
            double lon = pair.getDouble(0); // GeoJSON order is [lon, lat]
            double lat = pair.getDouble(1);
            opts.add(new LatLng(lat, lon));
        }

        com.google.android.gms.maps.model.Polyline polyline = myMap.addPolyline(opts);
        activePolylines.add(polyline);
    }


    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        try {
            JSONObject rankData = markerDataMap.get(marker);
            if (rankData != null) {
                rankName.setText(rankData.getString("rank_name"));

                if (rankData.has("distance")) {
                    rankDistance.setText("Distance: " + String.format("%.2f km", rankData.getDouble("distance")));
                } else {
                    rankDistance.setText("Distance: Not available");
                }

                // NEW: fetch routes for this rank name
                fetchAndShowRoutesForRank(rankData.getString("rank_name"));

                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error displaying marker data: " + e.getMessage());
            Toast.makeText(this, "Error displaying rank data", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission is denied, please allow the permission", Toast.LENGTH_SHORT).show();
                // Still fetch rank data even without location
                fetchRankData();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String startName = data.getStringExtra("START_NAME");
            double startLat = data.getDoubleExtra("START_LAT", 0.0);
            double startLng = data.getDoubleExtra("START_LNG", 0.0);

            String destName = data.getStringExtra("DEST_NAME");
            double destLat = data.getDoubleExtra("DEST_LAT", 0.0);
            double destLng = data.getDoubleExtra("DEST_LNG", 0.0);
        }
    }
}