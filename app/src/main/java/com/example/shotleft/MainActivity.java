package com.example.shotleft;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap myMap;
    private final int FINE_PERMISSION_CODE = 1;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    OkHttpClient client = new OkHttpClient();
    private String responseData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Current Location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        //adding Ranks


    }
    public void addRanks(){
        Request request = new Request.Builder()
                .url("https://umzxebhgucvpariulruy.supabase.co/rest/v1/ranks")
                .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVtenhlYmhndWN2cGFyaXVscnV5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1ODI4MjYsImV4cCI6MjA3MjE1ODgyNn0.o3ABCnBftZln1CmXg3Q0ewm1kdInEdTJ1PLA9oTTUR4")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    final String responseData = response.body().string();

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                processJSON(responseData);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }
    private void getLastLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    currentLocation = location;

                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MainActivity.this);
                }else{
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MainActivity.this);
                }
            }
        });
        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);

        //OkHttpClient client = new OkHttpClient();


    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);  // ✅ enables the blue dot
        }

        LatLng cur = new LatLng(-26, 28);
        if(currentLocation != null){
            cur = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        googleMap.addMarker(new MarkerOptions().position(cur).title("My Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        //LatLng Bree = new LatLng(-26.20081, 28.03653);
        //googleMap.addMarker(new MarkerOptions().position(Bree).title("Bree Taxi Rank"));

        float closerZoom = 15.0f; // Adjust this value for desired zoom
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cur, closerZoom));

        addRanks();

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
        ArrayList<String> rankArray = new ArrayList<>();

        JSONArray ja = new JSONArray(json);
        for(int i = 0;i < ja.length();i++){
            JSONObject jo = ja.getJSONObject(i);
            String name = jo.getString("rank_name");
            String lat = jo.getString("rank_latitude");
            String lon = jo.getString("rank_longitude");
            //double dist = Double.parseDouble(jo.getString("distance"));

            rankArray.add(name+";"+lat+";"+lon);

            //String[] rank = {name, lat, lon};
            //displayRanks(rank);
        }
        displayRanks(rankArray);
    }
    private void displayRanks(ArrayList<String> rankArray){
        String[] rank;
        ArrayList<LatLng> locations = new ArrayList<>();
        for(int i =0; i < rankArray.size(); i++) {
            rank = rankArray.get(i).split(";");
            double lat = Double.parseDouble(rank[1]);
            double lng = Double.parseDouble(rank[2]);
            LatLng rankLocation = new LatLng(lat, lng);
            locations.add(rankLocation);
            myMap.addMarker(new MarkerOptions().position(rankLocation).title(rank[0]));
        }

        ArrayList<LatLng> nearestRanks = getNearestRanks(currentLocation, locations);
        adjustCameraToNearestRanks(currentLocation, nearestRanks);
    }

    private void adjustCameraToNearestRanks(Location currentLocation, ArrayList<LatLng> nearestRanks){
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
            return new ArrayList<>(); // or handle gracefully
        }
        allRanks.sort((a, b) ->
                Double.compare(distanceBetween(currentLocation, a), distanceBetween(currentLocation, b)));

        ArrayList<LatLng> nearest = new ArrayList<>();
        for (int i = 0; i < Math.min(2, allRanks.size()); i++) {
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
}