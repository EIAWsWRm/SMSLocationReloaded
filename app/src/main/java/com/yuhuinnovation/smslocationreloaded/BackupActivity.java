package com.yuhuinnovation.smslocationreloaded;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupActivity extends AppCompatActivity {

    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int REQUEST_REQUEST_CODE = 42;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
    }

    private void readFileContent(Uri uri) {
        String jsonString = "undefined";
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String readline;
            while ((readline = reader.readLine()) != null) {
                jsonString = readline;
            }
            inputStream.close();
            restoreSettings(jsonString);
        }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "", Toast.LENGTH_LONG).show();
        }
    }

    private void writeFileContent(Uri uri, String fileText) {
        try {
            ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());
            fos.write(fileText.getBytes());
            fos.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private String getBackupString() {
        // TODO Convert more settings to JSON
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String mapService = SP.getString("mapService",
                "https://www.bing.com/maps?q=YYY,XXX"); //get URL string
        String locale = SP.getString("locale", "default");

        JSONObject outputJson = new JSONObject();
        try {
            outputJson.put("backupApiVersion", "1");
            outputJson.put("locale", locale);
            outputJson.put("mapService", mapService);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return outputJson.toString(); //final string
    }

    private void restoreSettings(String jsonString) {
        // TODO Add support for more settings
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String mapService = jsonObject.getString("mapService"); //map service string
            String locale = jsonObject.getString("locale"); //locale string
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = SP.edit();
            editor.putString("mapService", mapService);
            editor.putString("locale", locale);
            editor.apply();

            // Prompt to restart app
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.alertdialog_title_restartapp);
            builder.setMessage(R.string.alertdialog_text_restartapp);

            builder.setCancelable(false);
            builder.setPositiveButton(R.string.alertdialog_button_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent mStartActivity = new Intent(BackupActivity.this, MainActivity.class);
                            int mPendingIntentId = 5382233;
                            PendingIntent mPendingIntent = PendingIntent.getActivity(BackupActivity.this,
                                    mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager mgr = (AlarmManager)BackupActivity.this.getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                            //System.exit(0);
                            finishAffinity(); // Finish and remove ALL activities
                        }
                    });
                }
            });
            builder.setNegativeButton(R.string.alertdialog_button_no, null);
            builder.create().show();
        }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.backupactivity_invalid_format,
                    Toast.LENGTH_LONG).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Uri currentUri = null;
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_REQUEST_CODE) {
                if (resultData != null) {
                    currentUri = resultData.getData();
                    readFileContent(currentUri);
                }
            }
            else if (requestCode == CREATE_REQUEST_CODE) {
                if (resultData != null) {
                    currentUri = resultData.getData();
                    writeFileContent(currentUri, getBackupString());
                }
            }
        }
    }

    // Called when the "Backup" button is pressed
    public void onBackupPressed(View view) {
        // Set current time as a string to a variable
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyymmdd_HHmmss", Locale.US);
        String currentTime = dateFormat.format(date);

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "ShareLocationBackup_" + currentTime + ".txt"); //rename file to date
        startActivityForResult(intent, CREATE_REQUEST_CODE);
    }

    // Called when the "Restore" button is pressed
    public void onRestorePressed(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }
}
