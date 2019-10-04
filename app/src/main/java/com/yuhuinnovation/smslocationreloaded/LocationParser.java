package com.yuhuinnovation.smslocationreloaded;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class LocationParser extends AppCompatActivity {

    // Variables that store latitude and longitude
    double lat = 0.00;
    double lon = 0.00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_receiver);

        String intentMessage = "";
        boolean invalidLink = false;
        Intent intent = getIntent();
        String action = intent.getAction();

        Uri data = intent.getData();

        try {
            lat = Double.parseDouble(data.getQueryParameter("lat"));
            lon = Double.parseDouble(data.getQueryParameter("lon"));
        }
        catch (Exception e) {
            e.printStackTrace();
            invalidLink = true;
        }

        // TODO Register location listener

    }

    // TODO Add location listener

    private double getDistance(double lat1, double lon1, double lat2, double lon2) {

        // Haversine formula (ported from https://www.movable-type.co.uk/scripts/latlong.html)
        final double R = 6371e3;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2-lat1);
        double deltaLambda = Math.toRadians(lon2-lon1);
        double a = Math.sin(deltaPhi/2) * Math.sin(deltaPhi/2) + Math.cos(phi1) * Math.cos(phi2) +
                Math.sin(deltaLambda/2) * Math.sin(deltaLambda/2); // square of half the chord length between points
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); // Angular distance in radians
        double d = R * c; //where d is the final result (distance between two points)

        return d;
    }
}
