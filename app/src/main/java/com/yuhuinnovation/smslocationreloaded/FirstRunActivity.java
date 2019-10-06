package com.yuhuinnovation.smslocationreloaded;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirstRunActivity extends AppCompatActivity {

    // Array of strings for ListView Title
    String[] listviewTitle = new String[]{
            "Quick Start", "Grant location permission", "Accept privacy policy", "Continue",
    };

    int[] listviewImage = new int[]{
            R.drawable.ic_close_red_24dp, R.drawable.ic_close_red_24dp, R.drawable.ic_close_red_24dp, R.drawable.ic_no_red_24dp,
    };

    String[] listviewId = new String[]{
            "1", "2", "3", "4",
    };

    String isPrivacyAccepted = "false";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_run);

        drawListView();

        AlertDialog.Builder builder = new AlertDialog.Builder(FirstRunActivity.this);
        builder.setTitle(R.string.alertdialog_title_welcomemessage);
        builder.setMessage(R.string.alertdialog_text_welcomemessage);

        builder.setPositiveButton(R.string.alertdialog_button_ok, null);
        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadListView();
    }

    private void reloadListView() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            listviewImage[1] = R.drawable.ic_check_green_24dp;
        }
        else {
            listviewImage[1] = R.drawable.ic_close_red_24dp;
        }



        // Accepted privacy policy?
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        // Viewed the quick start guide?
        if (SP.getString("viewedIntro", "false").equals("true")) {
            listviewImage[0] = R.drawable.ic_check_green_24dp;
        }
        else {
            listviewImage[0] = R.drawable.ic_close_red_24dp;
        }

        isPrivacyAccepted = SP.getString("acceptedPrivacyPolicy","false");
        if (isPrivacyAccepted.equals("true")) {
            listviewImage[2] = R.drawable.ic_check_green_24dp;
        }
        else {
            listviewImage[2] = R.drawable.ic_close_red_24dp;
        }

        if (isPrivacyAccepted.equals("true") && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && SP.getString("viewedIntro","false").equals("true")) {
            listviewImage[3] = R.drawable.ic_forward_green_24dp;
        }
        else {
            listviewImage[3] = R.drawable.ic_no_red_24dp;
        }

        drawListView();
    }

    private void drawListView() {
        List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();

        // loop as many times as there are array values
        try {
            for (int i = 0; i < 4; i++) {
                HashMap<String, String> hm = new HashMap<String, String>();
                hm.put("listview_title", listviewTitle[i]);
                hm.put("listview_image", Integer.toString(listviewImage[i]));
                hm.put("listview_id", listviewId[i]);
                aList.add(hm);
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        String[] from = {"listview_image", "listview_title", "listview_id"};
        int[] to = {R.id.listview_image, R.id.listview_item_title, R.id.listview_id};
        SimpleAdapter simpleAdapter = new SimpleAdapter(getBaseContext(), aList, R.layout.listview, from, to);
        final ListView firstRunListView = (ListView) findViewById(R.id.firstrun_list_view);
        firstRunListView.setClickable(true);
        firstRunListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                final Map<String, String> item = (Map<String, String>) firstRunListView.getItemAtPosition(position);
                String chosenOption = item.get("listview_id"); // should convert to int but too lazy
                if (chosenOption == null) {
                    // Used to prevent NullPointerException
                }
                else if (chosenOption.equals("1")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(FirstRunActivity.this);
                            builder.setTitle(R.string.alertdialog_title_quickstart);
                            builder.setMessage(R.string.alertdialog_text_quickstart);

                            builder.setCancelable(false);
                            builder.setPositiveButton(R.string.alertdialog_button_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(FirstRunActivity.this);
                                    SharedPreferences.Editor editor = SP.edit();
                                    editor.putString("viewedIntro", "true");
                                    editor.apply();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            reloadListView();
                                        }
                                    });
                                }
                            });
                            builder.create().show();
                        }
                    });
                }
                else if (chosenOption.equals("2")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(FirstRunActivity.this);
                            SharedPreferences.Editor editor = SP.edit();
                            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(FirstRunActivity.this, R.string.firstrun_text_permissiongranted,
                                        Toast.LENGTH_LONG).show();
                            }
                            else if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                                    && SP.getString("firstAskLocation", "true").equals("false")) {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},101);
                                Toast.makeText(FirstRunActivity.this, R.string.firstrun_text_permissiondisabled,
                                        Toast.LENGTH_LONG).show();
                            }
                            else {
                                // User did NOT check "Don't ask again"
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},101);
                                editor.putString("firstAskLocation", "false");
                                editor.apply();
                            }
                            reloadListView();
                        }
                    });
                }
                else if (chosenOption.equals("3")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(FirstRunActivity.this);
                            builder.setTitle(R.string.alertdialog_title_privacypolicy);
                            builder.setMessage(R.string.alertdialog_text_privacypolicy);

                            builder.setCancelable(false);
                            builder.setPositiveButton(R.string.alertdialog_button_agree, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(FirstRunActivity.this);
                                    SharedPreferences.Editor editor = SP.edit();
                                    editor.putString("acceptedPrivacyPolicy", "true");
                                    editor.apply();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            reloadListView();
                                        }
                                    });
                                }
                            });
                            builder.setNegativeButton(R.string.alertdialog_button_disagree, new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(FirstRunActivity.this);
                                    SharedPreferences.Editor editor = SP.edit();
                                    editor.putString("acceptedPrivacyPolicy", "false");
                                    editor.apply();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(FirstRunActivity.this, R.string.firstrun_text_disagreewithprivacy,
                                                    Toast.LENGTH_LONG).show();
                                            reloadListView();
                                        }
                                    });
                                }
                            });
                            builder.create().show();
                        }
                    });
                }
                else if (chosenOption.equals("4")) {
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(FirstRunActivity.this);
                    if (isPrivacyAccepted.equals("true") && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && SP.getString("viewedIntro","false").equals("true")) {
                        Intent intent = new Intent(FirstRunActivity.this, MainActivity.class);
                        finishAffinity(); //clear the stack (so user cannot back into this activity)
                        startActivity(intent);
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(FirstRunActivity.this, R.string.firstrun_text_tasksincomplete,
                                        Toast.LENGTH_LONG).show();
                                reloadListView();
                            }
                        });
                    }
                }
                Log.d("firstrun", chosenOption);
            }
        });
        firstRunListView.setAdapter(simpleAdapter);
    }
}
