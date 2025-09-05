package com.example.shotleft;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;

public class Search_Activity extends AppCompatActivity {

    private Button confirmButton;
    private double destinationLat, destinationLng;
    private double startLat, startLng;
    private String startName, destinationName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        confirmButton = findViewById(R.id.confirmButton);
        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(), "AIzaSyAbPm8G3zn7DSRRqgXPvgz4va4NIWPQd3U");

        }
        PlacesClient placesClient = Places.createClient(this);


        AutocompleteSupportFragment startLocationAutocomplete = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.start_location_autocomplete);

        AutocompleteSupportFragment destinationAutocomplete = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.destination_autocomplete);

        startLocationAutocomplete.setPlaceFields(Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        destinationAutocomplete.setPlaceFields(Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        String currentLocationName = getIntent().getStringExtra("Current_Location_Name");
        double currentLat = getIntent().getDoubleExtra("Current_Location_lat", 0.0);
        double currentLng = getIntent().getDoubleExtra("Current_Location_lng", 0.0);

        if (currentLocationName != null) {
            startLocationAutocomplete.setText(currentLocationName);
            startLat = currentLat;
            startLng = currentLng;
            startName = currentLocationName;
        }

        //Handle user selecting NEW start location
        startLocationAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                if(place.getLatLng() != null){
                    startLat = place.getLatLng().latitude;
                    startLng = place.getLatLng().longitude;
                    startName = place.getName();
                }
            }


            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(Search_Activity.this, "Error selecting start location: " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
                Log.d("Search activity: ", status.getStatusMessage());
            }
        });

        //Handling user selecting a destination
        destinationAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                if (place.getLatLng() != null) {
                    destinationLat = place.getLatLng().latitude;
                    destinationLng = place.getLatLng().longitude;
                    destinationName = place.getName();
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(Search_Activity.this, "Error selecting destination: " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }
        });

        //Confirm button sends results back to MainActivity
        confirmButton.setOnClickListener(v -> {
            if (destinationName == null || destinationLat == 0.0 || destinationLng == 0.0) {
                Toast.makeText(Search_Activity.this, "Please select a destination", Toast.LENGTH_LONG).show();
                return;
            }
            Intent resultIntent = new Intent();
            resultIntent.putExtra("START_NAME", startName);
            resultIntent.putExtra("START_LAT", startLat);
            resultIntent.putExtra("START_LNG", startLng);
            resultIntent.putExtra("DEST_NAME", destinationName);
            resultIntent.putExtra("DEST_LAT", destinationLat);
            resultIntent.putExtra("DEST_LNG", destinationLng);

            setResult(RESULT_OK, resultIntent);
            finish();
        });

    }


}