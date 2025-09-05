package com.example.shotleft;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
public class GeocodingUtils {

    public static String getAddressFromLatLng(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        String addressText = "Address not found";

        try {
            // Get a list of addresses for the given latitude and longitude.
            // The number '1' means you only want the best match.
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            // Check if any addresses were found.
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Build a string from the address lines.
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        sb.append(", ");
                    }
                }
                addressText = sb.toString();
            }
        } catch (IOException e) {
            // Handle network or other errors here.
            e.printStackTrace();
            addressText = "Error retrieving address";
        }

        return addressText;
    }
}
