package com.yuhuinnovation.smslocationreloaded;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Variables that store latitude and longitude
    double lat = 0.00;
    double lon = 0.00;

    // Strings denoting the phone numbers of the contacts
    String contactNumber1 = "";
    String contactNumber2 = "";
    String contactNumber3 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

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

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the preferences
        //SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        /*
        String strUserName = SP.getString("username", "null");
        boolean bAppUpdates = SP.getBoolean("applicationUpdates",false);
        String downloadType = SP.getString("locale","default");
        */

        //request permissions
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // Show dialog explaining why location permission is needed.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.alertdialog_title_firstrun);
            builder.setMessage(R.string.alertdialog_text_firstrun);

            builder.setCancelable(false);
            builder.setPositiveButton(R.string.alertdialog_button_continue, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Request permissions
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},101);
                }
            });
            builder.setNeutralButton(R.string.alertdialog_button_uninstall, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            });;
            builder.setNegativeButton(R.string.alertdialog_button_quit, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finishAndRemoveTask();
                }
            });
            builder.create().show();
        }
        else {
            LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

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

        // Auto-fill phone number
        String autoFillPhone = SP.getString("autoFillPhone", "lastSent"); //get setting value
        if (autoFillPhone.equals("lastSent")) {
            String lastSentPhoneNo = "";
            try {
                //try to get the selected resource
                InputStream in = openFileInput("lastSentPhoneNo");
                String input = MyGlobals.streamToString(in);
                lastSentPhoneNo = input;
            } catch (Exception e) {
                e.printStackTrace();
                /* // Uncomment if needed
                Toast.makeText(getApplicationContext(), "Error: Failed to get resource \"lastSentPhoneNo\"",
                        Toast.LENGTH_SHORT).show();
                        */
            }
            EditText editText = (EditText) findViewById(R.id.phoneNo);
            editText.setText(lastSentPhoneNo);
        }
        else if (autoFillPhone.equals("favorite")) {
            String favPhoneNo = SP.getString("favPhoneNo", "null");
            if (favPhoneNo.equals("null") || favPhoneNo.replaceAll("\\s","").equals("")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alertdialog_title_error);
                builder.setMessage(R.string.alertdialog_text_nofavphone);
                builder.setPositiveButton(R.string.alertdialog_button_ok, null);
                builder.create().show();
            }
            else if (favPhoneNo.contains("[A-Z]") || favPhoneNo.contains("[a-z]")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alertdialog_title_error);
                builder.setMessage(R.string.alertdialog_text_nofavphone);
                builder.setPositiveButton(R.string.alertdialog_button_ok, null);
                builder.create().show();
            }
            else {
                EditText editText = (EditText) findViewById(R.id.phoneNo);
                editText.setText(favPhoneNo);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_contacts:
                // Go to contacts
                Intent contactsIntent = new Intent(this, ContactsActivity.class);
                startActivity(contactsIntent);
                return true;
            case R.id.settings:
                // Go to settings
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.add_shortcut:
                onAddShortcutPressed();
                return true;
            case R.id.quit_app:
                finishAndRemoveTask(); // Close app
            default:
                return super.onContextItemSelected(item);
        }
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            lat = location.getLatitude();
            lon = location.getLongitude();

            String message = "";
            if (lat == 0 && lon == 0) {
                message = "Location Not Available";
            }
            else {
                message = String.valueOf(lat) + ", " + String.valueOf(lon);
            }

            TextView textView = findViewById(R.id.currentLocation);
            textView.setText(message);
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

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("mainActivity", "onResume");

        String contactName1 = "";
        String contactName2 = "";
        String contactName3 = "";

        // Get contact names
        try {
            //try to get the selected resource
            InputStream in = openFileInput("contact1");
            String input = MyGlobals.streamToString(in);
            String[] inputArray = input.split(",");
            if (inputArray.length >= 2) {
                Log.d("inputarrayLength", "2");
                Log.d("inputarray1", inputArray[1]);
                contactNumber1 = inputArray[1];
            }
            else {
                contactNumber1 = "";
            }
            if (inputArray.length >= 1) {
                Log.d("inputarrayLength", "1");
                contactName1 = inputArray[0];
            }
            else {
                contactName1 = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            //try to get the selected resource
            InputStream in = openFileInput("contact2");
            String input = MyGlobals.streamToString(in);
            String[] inputArray = input.split(",");
            if (inputArray.length >= 2) {
                contactNumber2 = inputArray[1];
            }
            else {
                contactNumber2 = "";
            }
            if (inputArray.length >= 1) {
                contactName2 = inputArray[0];
            }
            else {
                contactName2 = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            //try to get the selected resource
            InputStream in = openFileInput("contact3");
            String input = MyGlobals.streamToString(in);
            String[] inputArray = input.split(",");
            if (inputArray.length >= 2) {
                contactNumber3 = inputArray[1];
            }
            else {
                contactNumber3 = "";
            }
            if (inputArray.length >= 1) {
                contactName3 = inputArray[0];
            }
            else {
                contactName3 = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set to visible by default
        findViewById(R.id.fillContact1).setVisibility(View.VISIBLE);
        findViewById(R.id.contactName1).setVisibility(View.VISIBLE);
        findViewById(R.id.fillContact2).setVisibility(View.VISIBLE);
        findViewById(R.id.contactName2).setVisibility(View.VISIBLE);
        findViewById(R.id.fillContact3).setVisibility(View.VISIBLE);
        findViewById(R.id.contactName3).setVisibility(View.VISIBLE);

        // Set to gone by default
        findViewById(R.id.noContactsMessage).setVisibility(View.GONE);

        // Display contact names
        if (contactName1.trim().equals("")) {
            if (contactNumber1.trim().equals("")) {
                Button button = (Button) findViewById(R.id.fillContact1);
                TextView textView = (TextView) findViewById(R.id.contactName1);
                button.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
            }
            else {
                TextView textView = (TextView) findViewById(R.id.contactName1);
                textView.setText(contactNumber1);
            }
        }
        else {
            TextView textView = (TextView) findViewById(R.id.contactName1);
            textView.setText(contactName1);
        }
        if (contactName2.trim().equals("")) {
            if (contactNumber2.trim().equals("")) {
                Button button = (Button) findViewById(R.id.fillContact2);
                TextView textView = (TextView) findViewById(R.id.contactName2);
                button.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
            }
            else {
                TextView textView = (TextView) findViewById(R.id.contactName2);
                textView.setText(contactNumber2);
            }
        }
        else {
            TextView textView = (TextView) findViewById(R.id.contactName2);
            textView.setText(contactName2);
        }
        if (contactName3.trim().equals("")) {
            if (contactNumber3.trim().equals("")) {
                Button button = (Button) findViewById(R.id.fillContact3);
                TextView textView = (TextView) findViewById(R.id.contactName3);
                button.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
            }
            else {
                TextView textView = (TextView) findViewById(R.id.contactName3);
                textView.setText(contactNumber3);
            }
        }
        else {
            TextView textView = (TextView) findViewById(R.id.contactName3);
            textView.setText(contactName3);
        }
        if (contactNumber1.trim().equals("") && contactNumber2.trim().equals("") && contactNumber3.trim().equals("")) {
            TextView textView = (TextView) findViewById(R.id.noContactsMessage);
            textView.setVisibility(View.VISIBLE);
        }


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

    public int sendSms(String phoneNo, String content) {
        /** Exit Codes:
         * 0 - Successful
         * 1 - Unknown Failure
         * 3 - Failed because no phone number was provided
         */
        if (phoneNo.equals("")) {
            return 2;
        }
        else {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNo, null, content, null, null);
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Successfully sent SMS",
                                Toast.LENGTH_LONG).show();
                    }
                });

                return 0; // no error
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Failed to send, try again later.",
                                Toast.LENGTH_LONG).show();
                    }
                });
                e.printStackTrace();

                return 1; // error encountered
            }
        }
    }

    public void fillNumber(int id) {
        // TODO Code to fill phone number into textbox
        String message = "";
        String filename = "contact" + String.valueOf(id);

        try {
            //try to get the selected resource
            InputStream in = openFileInput(filename);
            String input = MyGlobals.streamToString(in);
            if (input.split(",").length >= 2) {
                message = input.split(",")[1];
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // If message is not blank
        if (!message.trim().equals("")) {
            EditText editText = (EditText) findViewById(R.id.phoneNo);
            editText.setText(message);
        }
    }

    public String getLatLonString(double lat, double lon) {
        //to read settings
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String finalString = String.valueOf(lat) + ", " + String.valueOf(lon);
        String mapLink = SP.getString("mapService",
                "https://www.bing.com/maps?q=YYY,XXX"); //get URL string
        mapLink = mapLink.replace("YYY", String.valueOf(lat)); //replace in latitude
        mapLink = mapLink.replace("XXX", String.valueOf(lon)); //replace in longitude
        finalString = finalString + " // " + mapLink; //concatenate the strings

        return finalString;
    }

    // Called when the "Add Shortcut" button is pressed
    public void onAddShortcutPressed() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.alertdialog_title_shortcutcheck);
            builder.setMessage(R.string.alertdialog_text_shortcutcheck);

            builder.setPositiveButton(R.string.alertdialog_button_looksgood, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Create ShortcutManager class
                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                    ShortcutInfo shortcut = new ShortcutInfo.Builder(MainActivity.this, "pinned-shortcut")
                            .setShortLabel(getResources().getString(R.string.shortcut_title_quicksend))
                            .setLongLabel(getResources().getString(R.string.shortcut_desc_quicksend))
                            .setIcon(Icon.createWithResource(MainActivity.this, R.mipmap.launch_quick_send))
                            .setIntent(new Intent(MainActivity.this, QuickSendActivity.class).setAction("QUICK_SEND_SHORTCUT"))
                            .build();

                    shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));

                    if (shortcutManager.isRequestPinShortcutSupported()) {
                        ShortcutInfo pinShortcutInfo = new ShortcutInfo
                                .Builder(MainActivity.this, "pinned-shortcut")
                                .build();
                        Intent pinnedShortcutCallbackIntent =
                                shortcutManager.createShortcutResultIntent(pinShortcutInfo);

                        //get the callback of the shortcut
                        PendingIntent successCallback = PendingIntent.getBroadcast(MainActivity.this, 0,
                                pinnedShortcutCallbackIntent, 0);
                        shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.getIntentSender());

                    }

                    finishAndRemoveTask(); // Close app (to prevent caching bugs)

                }
            });
            builder.setNegativeButton(R.string.alertdialog_button_checkagain, null);
            builder.create().show();
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.alertdialog_title_error);
            builder.setMessage(R.string.alertdialog_text_unsupported_android8);

            builder.setPositiveButton(R.string.alertdialog_button_ok, null);
            builder.create().show();
        }
    }

    // Called when the "Share Location" button is pressed
    public void onShareLocationPressed(View view) {
        if (lat == 0.00 && lon == 0.00) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.alertdialog_title_error);
            builder.setMessage(R.string.alertdialog_text_nolocation);

            builder.setPositiveButton(R.string.alertdialog_button_ok, null);
            builder.create().show();
        }
        else {
            String shareBody = getLatLonString(lat, lon); // Contents to be shared
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My Location");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.sharedialog_title_sendlocation)));
        }
    }

    // Called when the "Send SMS" button is pressed
    public void onSendSmsPressed(View view) {

        final Integer PERMISSION_SMS = 102; // TODO Add a function for this permission code

        //request permissions
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) {
                Toast.makeText(this, "SMS permission is needed to send text messages. " +
                                "We will never send messages without your permission."
                        , Toast.LENGTH_LONG).show();
            }
            // Ask for permission
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SMS);
        }

        boolean shouldShowSmsWarning = true;
        try {
            //try to get the selected resource
            InputStream in = openFileInput("setting_smsWarning");
            String input = MyGlobals.streamToString(in);
            Log.d("input",input);
            if (input.equals("true")) {
                shouldShowSmsWarning = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (shouldShowSmsWarning) {
            // Inflate the checkbox
            View checkBoxView = View.inflate(this, R.layout.checkbox, null);
            CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    //push the text to a file

                    Log.d("checkbox", String.valueOf(isChecked));
                    String filename = "setting_smsWarning";
                    String fileContents = "true";
                    if (!isChecked) {
                        fileContents = "false";
                    }
                    FileOutputStream outputStream;
                    try {
                        outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                        outputStream.write(fileContents.getBytes());
                        outputStream.close();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "Whoops! Something went wrong.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            checkBox.setText(R.string.alertdialog_checkbox_nosmswarning); //checkbox text

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.alertdialog_title_warning);
            builder.setMessage(R.string.alertdialog_text_smswarning);

            builder.setView(checkBoxView); //set checkbox into dialog

            builder.setPositiveButton(R.string.alertdialog_button_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String latLonString = getLatLonString(lat, lon); // String to send
                    EditText editText = (EditText) findViewById(R.id.phoneNo);
                    String phoneNo = editText.getText().toString();

                    int smsErrorCode = sendSms(phoneNo,latLonString);

                    // If there is an error, probably because of insufficient permissions.
                    if (smsErrorCode == 1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.alertdialog_title_smserror);
                        builder.setMessage(R.string.alertdialog_text_phonepermission);

                        builder.setCancelable(false);

                        builder.setPositiveButton(R.string.alertdialog_button_continue, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Request permissions
                                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 103);
                            }
                        });
                        builder.setNegativeButton(R.string.alertdialog_button_learnmore_english, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Open webpage explaining permissions
                                String url = "https://stackoverflow.com/a/27300529";
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(url));
                                startActivity(i);
                            }
                        });
                        builder.create().show();
                    }
                    else if (smsErrorCode == 2) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.alertdialog_title_error);
                        builder.setMessage(R.string.alertdialog_text_nophonenumber);
                        builder.setPositiveButton(R.string.alertdialog_button_ok, null);
                        builder.create().show();
                    }
                    else {
                        // Write the last sent number to a file
                        String filename = "lastSentPhoneNo";
                        String fileContents = phoneNo;
                        FileOutputStream outputStream;
                        try {
                            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                            outputStream.write(fileContents.getBytes());
                            outputStream.close();
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(),
                                            "Whoops! Something went wrong.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }
            });
            builder.setNegativeButton(R.string.alertdialog_button_no, null);
            builder.create().show();
        }
        else {
            String latLonString = getLatLonString(lat, lon); // String to send
            EditText editText = (EditText) findViewById(R.id.phoneNo);
            String phoneNo = editText.getText().toString();

            int smsErrorCode = sendSms(phoneNo,latLonString);

            // If there is an error, probably because of insufficient permissions.
            if (smsErrorCode == 1) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alertdialog_title_smserror);
                builder.setMessage(R.string.alertdialog_text_phonepermission);

                builder.setCancelable(false);

                builder.setPositiveButton(R.string.alertdialog_button_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Request permissions
                        requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 103);
                    }
                });
                builder.setNegativeButton(R.string.alertdialog_button_learnmore_english, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Open webpage explaining permissions
                        String url = "https://stackoverflow.com/a/27300529";
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                });
                builder.create().show();
            }
            else if (smsErrorCode == 2) {
                // Phone number was left blank
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alertdialog_title_error);
                builder.setMessage(R.string.alertdialog_text_nophonenumber);
                builder.setPositiveButton(R.string.alertdialog_button_ok, null);
                builder.create().show();
            }
            else {
                // Write the last sent number to a file
                String filename = "lastSentPhoneNo";
                String fileContents = phoneNo;
                FileOutputStream outputStream;
                try {
                    outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(fileContents.getBytes());
                    outputStream.close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Whoops! Something went wrong.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }

    // Called when the first Fill Number button is pressed
    public void onFillNumber1Pressed(View view) {
        fillNumber(1);
    }

    // Called when the second Fill Number button is pressed
    public void onFillNumber2Pressed(View view) {
        fillNumber(2);
    }

    // Called when the third Fill Number button is pressed
    public void onFillNumber3Pressed(View view) {
        fillNumber(3);
    }
}
