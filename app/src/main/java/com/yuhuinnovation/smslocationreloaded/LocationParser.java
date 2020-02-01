package com.yuhuinnovation.smslocationreloaded;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Locale;

public class LocationParser extends AppCompatActivity {

    // Variables that store latitude and longitude passed in with the request
    double lat = 0.00;
    double lon = 0.00;

    // Variables that store the current latitude and longitude
    double currentlat = 0.00;
    double currentlon = 0.00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_parser);

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

        Spinner spinner = findViewById(R.id.mapServiceSpinner); //get spinner from xml
        String[] spinnerItems = getResources().getStringArray(R.array.mapServiceArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, spinnerItems);
        spinner.setAdapter(adapter); //set adapter to the one created above

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get locale
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String language = SP.getString("locale","default");

        // Set locale
        if (language.equals("none") || language.equals("default")) {
            Locale locale = new Locale(Locale.getDefault().getLanguage(), Locale.getDefault().getCountry());
            Locale.setDefault(locale);
            Configuration config = getBaseContext().getResources().getConfiguration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
        else if (language.contains("_")) {
            // Special consideration for languages with region codes
            String[] languageRegion = language.split("_");
            Locale locale = new Locale(languageRegion[0], languageRegion[1]);
            Locale.setDefault(locale);
            Configuration config = getBaseContext().getResources().getConfiguration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
        else {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration config = getBaseContext().getResources().getConfiguration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister location listener
        try {
            LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            mLocationManager.removeUpdates(mLocationListener);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            currentlat = location.getLatitude();
            currentlon = location.getLongitude();

            if (currentlat != 0 && currentlon != 0) {
                double distance = getDistance(currentlat, currentlon, lat, lon);
                Log.d("currentcoords", String.valueOf(currentlat) + ", " + String.valueOf(currentlon + ", ") +
                        String.valueOf(lat) + ", " + String.valueOf(lon));

                double distancekm = Math.round((distance / 1000) * 1000d) / 1000d;
                TextView textView = (TextView) findViewById(R.id.distanceValue);
                textView.setText(String.valueOf(distancekm) + " km");
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {
            Toast.makeText(getApplicationContext(), R.string.toast_text_gpson, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String s) {
            Toast.makeText(getApplicationContext(), R.string.toast_text_gpsoff, Toast.LENGTH_SHORT).show();
        }
    };

    private double getDistance(double lat1, double lon1, double lat2, double lon2) {

        // Haversine formula (ported from https://www.movable-type.co.uk/scripts/latlong.html)
        final double R = 6371e3;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2-lat1);
        double deltaLambda = Math.toRadians(lon2-lon1);
        /* // Haversine formula
        double a = Math.sin(deltaPhi/2) * Math.sin(deltaPhi/2) + Math.cos(phi1) * Math.cos(phi2) +
                Math.sin(deltaLambda/2) * Math.sin(deltaLambda/2); // square of half the chord length between points
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); // Angular distance in radians
        double d = R * c; //where d is the final result (distance between two points)
        */

        // Law of cosines
        double d = Math.acos(Math.sin(phi1)*Math.sin(phi2) + Math.cos(phi1)*Math.cos(phi2) * Math.cos(deltaLambda)) * R;

        return d;
    }

    // Called when the "Calculate" button is pressed
    public void onCalculateDistance(View view) {
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100,
                    5, mLocationListener);
            if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.appstart_no_gps_title);
                builder.setMessage(R.string.appstart_no_gps);
                builder.setPositiveButton(R.string.alertdialog_button_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
                builder.setNegativeButton(R.string.alertdialog_button_no, null);
                builder.create().show();
            }
        }
        catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Please grant location permission in settings.",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public String getLatLonLink(double lat, double lon) {
        //to read settings
        Spinner mapServiceSpinner = (Spinner) findViewById(R.id.mapServiceSpinner);
        String mapService = mapServiceSpinner.getSelectedItem().toString();

        String[] spinnerItems = getResources().getStringArray(R.array.mapServiceArray);
        String[] mapServiceValues = getResources().getStringArray(R.array.mapServiceValues);

        int mapServiceIndex = Arrays.asList(spinnerItems).indexOf(mapService); //get index of value in spinner

        String mapLink = mapServiceValues[mapServiceIndex]; //get URL string
        mapLink = mapLink.replace("YYY", String.valueOf(lat)); //replace in latitude
        mapLink = mapLink.replace("XXX", String.valueOf(lon)); //replace in longitude

        return mapLink;
    }

    // Called when the "Open" button is pressed
    public void onOpenWithPressed(View view) {
        // TODO Error: No activity found to handle intent
        String url = getLatLonLink(lat, lon);
        Log.d("URL", url);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}
