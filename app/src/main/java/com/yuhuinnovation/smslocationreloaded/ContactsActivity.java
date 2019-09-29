package com.yuhuinnovation.smslocationreloaded;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class ContactsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        reloadContacts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_contacts, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                // Show help dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alertdialog_title_help);
                builder.setMessage(R.string.alertdialog_text_contactshelp);

                builder.setPositiveButton(R.string.alertdialog_button_ok, null);
                builder.create().show();
                return true;
            case R.id.action_settings:
                // Go to settings
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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

        setTitle(R.string.title_activity_contacts);  // Set the title of the activity
    }

    private void reloadContacts () {

        Log.d("contactsActivity", "reloadContacts();");

        // Get and display contact names
        String contactName1 = "";
        String contactName2 = "";
        String contactName3 = "";

        String contactNumber1 = "";
        String contactNumber2 = "";
        String contactNumber3 = "";

        // Get contact names
        try {
            //try to get the selected resource
            InputStream in = openFileInput("contact1");
            String input = MyGlobals.streamToString(in);
            String[] inputArray = input.split(",");
            if (inputArray.length >= 2) {
                contactNumber1 = inputArray[1];
            }
            if (inputArray.length >= 1) {
                contactName1 = inputArray[0];
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
            if (inputArray.length >= 1) {
                contactName2 = inputArray[0];
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
            if (inputArray.length >= 1) {
                contactName3 = inputArray[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Display contact names
        if (contactName1.trim().equals("")) {
            if (contactNumber1.trim().equals("")) {
                TextView textView = (TextView) findViewById(R.id.contact_1);
                textView.setText(R.string.contactsactivity_text_nocontact);
            }
            else {
                TextView textView = (TextView) findViewById(R.id.contact_1);
                textView.setText(contactNumber1);
            }
        }
        else {
            TextView textView = (TextView) findViewById(R.id.contact_1);
            textView.setText(contactName1);
        }
        if (contactName2.trim().equals("")) {
            if (contactNumber2.trim().equals("")) {
                TextView textView = (TextView) findViewById(R.id.contact_2);
                textView.setText(R.string.contactsactivity_text_nocontact);
            }
            else {
                TextView textView = (TextView) findViewById(R.id.contact_2);
                textView.setText(contactNumber2);
            }
        }
        else {
            TextView textView = (TextView) findViewById(R.id.contact_2);
            textView.setText(contactName2);
        }
        if (contactName3.trim().equals("")) {
            if (contactNumber3.trim().equals("")) {
                TextView textView = (TextView) findViewById(R.id.contact_3);
                textView.setText(R.string.contactsactivity_text_nocontact);
            }
            else {
                TextView textView = (TextView) findViewById(R.id.contact_3);
                textView.setText(contactNumber3);
            }
        }
        else {
            TextView textView = (TextView) findViewById(R.id.contact_3);
            textView.setText(contactName3);
        }
    }

    //function to edit the contact's phone number
    private void editContact (final int contactID) {

        Context context = getApplicationContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Name field
        final EditText nameEditText = new EditText(context);
        nameEditText.setHint(R.string.alertdialog_hint_name);
        layout.addView(nameEditText); // An add method

        // Phone field
        final EditText numberEditText = new EditText(context);
        numberEditText.setHint(R.string.alertdialog_hint_phonenumber);
        numberEditText.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(numberEditText); // Another add method

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alertdialog_title_editcontact);

        builder.setView(layout);

        builder.setPositiveButton(R.string.alertdialog_button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (nameEditText.getText().toString().contains(",")) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    R.string.contactsactivity_toast_nocomma,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else if (!nameEditText.getText().toString().trim().equals("") && numberEditText.getText().toString().trim().equals("")) {
                    // If name is not empty but phone number is...
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    R.string.alertdialog_text_nophonenumber,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else {
                    String filename = "contact" + String.valueOf(contactID);
                    String fileContents = nameEditText.getText().toString() + "," + numberEditText.getText().toString();

                    FileOutputStream outputStream;
                    try {
                        outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                        outputStream.write(fileContents.getBytes());
                        outputStream.close();
                        reloadContacts();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        R.string.toast_text_success,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
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
        builder.setNegativeButton(R.string.alertdialog_button_cancel, null);

        builder.create().show();

    }

    // Called when the first "Edit" button is pressed
    public void onEdit1Pressed (View view) {
        editContact(1);
    }

    // Called when the second "Edit" button is pressed
    public void onEdit2Pressed (View view) {
        editContact(2);
    }

    // Called when the third "Edit" button is pressed
    public void onEdit3Pressed (View view) {
        editContact(3);
    }
}
