package com.example.shotleft;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BackendTesting extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backend_testing);

        TextView display = findViewById(R.id.textView);
        Button testing = findViewById(R.id.button);
        testing.setOnClickListener( v -> {

            OkHttpClient client = new OkHttpClient();

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

                        BackendTesting.this.runOnUiThread(new Runnable() {
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

        });

    }
    private void processJSON(String json) throws JSONException {
        JSONArray ja = new JSONArray(json);
        for(int i = 0;i < ja.length();i++){
            JSONObject jo = ja.getJSONObject(i);
            double lat = Double.parseDouble(jo.getString("rank_latitude"));
            double log = Double.parseDouble(jo.getString("rank_longitude"));
            double dist = Double.parseDouble(jo.getString("distance"));
            String rank = jo.getString("rank_name");

        }
    }

}
