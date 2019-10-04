package com.yuhuinnovation.smslocationreloaded;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class LocationReceiver extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_receiver);

        String intentMessage = "no intent message found";
        Intent intent = getIntent();
        String action = intent.getAction();

        Uri data = intent.getData();

        try {
            intentMessage = data.getQueryParameter("lat") + data.getQueryParameter("lon");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        TextView textview = (TextView) findViewById(R.id.dummyTextView);

        textview.setText(intentMessage);
    }
}
