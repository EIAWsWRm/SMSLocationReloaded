package com.yuhuinnovation.smslocationreloaded;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class QuickSendActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_send);

        // Stop service (if it is currently running)
        try {
            stopService(new Intent(QuickSendActivity.this, BackgroundSendService.class));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // TODO Move ALL code below here into a background process
        /*
        Background process will:
        * Be started by this activity
        * Initialize LocationManager
        * Wait 30 seconds for a valid location before timing out
        * If location found:
        * Send message
        * finish();
        * Else --------------
        * Toast("No location found");
        * finish();
         */

        // Start the service that will send in the background
        startService(new Intent(QuickSendActivity.this, BackgroundSendService.class));

        finish(); // Quit activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
