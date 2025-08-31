package com.example.shotleft;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap myMap;
    private String responseData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://umzxebhgucvpariulruy.supabase.co/rest/v1/rpc/nearest_rank")
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        LatLng braam = new LatLng(-26.1929, 28.0304);
        LatLng UJ = new LatLng(-26.1834, 27.9988);
        googleMap.addMarker(new MarkerOptions().position(braam).title("wss"));
        googleMap.addMarker(new MarkerOptions().position(UJ).title("uj"));

        float closerZoom = 15.0f; // Adjust this value for desired zoom
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(braam, closerZoom));
    }

    private void processJSON(String json) throws JSONException {
        JSONArray ja = new JSONArray(json);
        for(int i = 0;i < ja.length();i++){
            JSONObject jo = ja.getJSONObject(i);
            String name = jo.getString("rank_name");
            String lat = jo.getString("rank_latitude");
            String lon = jo.getString("rank_longitude");
            //double dist = Double.parseDouble(jo.getString("distance"));

            String[] rank = {name, lat, lon};
            displayRank(rank);

        }
    }

    private void displayRank(String[] rank) {

    }
}