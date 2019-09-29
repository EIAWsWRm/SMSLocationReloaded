package com.yuhuinnovation.smslocationreloaded;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.Locale;

public class BackgroundSendService extends Service {

    private static String TAG = "BackgroundSendService";
    private Handler handler;
    private Runnable runnable;
    private final int runTime = 2000; // amount of time before the main handler loops
    private boolean taskFinished = false;

    double lat = 0.00;
    double lon = 0.00;

    private static int FOREGROUND_ID = 2731;

    @Override
    public void onCreate() {
        Log.d("service", "onCreate");
        super.onCreate();

        // Make the service a foreground service
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        int notifyID = 1;// Sets an ID for the notification, so it can be updated.
        String CHANNEL_ID = "my_channel_01";// The id of the channel.
        CharSequence name = getString(R.string.notification_title_channelname);// The user-visible name of the channel.
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(BackgroundSendService.this)
                    .setContentTitle(getResources().getString(R.string.notification_title_sending))
                    .setContentText(getResources().getString(R.string.notification_content_sending))
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setChannelId(CHANNEL_ID)
                    .setOngoing(true)
                    .build();
        }
        else {
            notification = new Notification.Builder(BackgroundSendService.this)
                    .setContentTitle(getResources().getString(R.string.notification_title_sending))
                    .setContentText(getResources().getString(R.string.notification_content_sending))
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setOngoing(true)
                    .build();
        }
        startForeground(FOREGROUND_ID, notification);


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


        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                Log.d("service", String.valueOf(taskFinished));
                if (taskFinished) {
                    // Task is finished; we are done
                    stopForeground(true);
                    Log.d("service", "stopSelf");
                    BackgroundSendService.this.stopSelf(); // stop the service
                }
                else {
                    // Loop again
                    handler.postDelayed(runnable, runTime);
                }
            }
        };
        handler.post(runnable);

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000,
                    0, mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000,
                    0, mLocationListener);
        }

        Handler destroyHandler = new Handler();
        Runnable destroyRunnable = new Runnable() {
            @Override
            public void run() {
                if (!taskFinished) {
                    // If task is not already finished
                    Log.d("service", "Timed out");
                    taskFinished = true;
                    // Send notification of failure
                    int notifyID = 1;// Sets an ID for the notification, so it can be updated.
                    String CHANNEL_ID = "my_channel_01";// The id of the channel.
                    CharSequence name = getString(R.string.notification_title_channelname);// The user-visible name of the channel.
                    int importance = NotificationManager.IMPORTANCE_HIGH;
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    Notification notification;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                        mNotificationManager.createNotificationChannel(mChannel);
                        notification = new Notification.Builder(BackgroundSendService.this)
                                .setContentTitle(getResources().getString(R.string.notification_title_failure))
                                .setContentText(getResources().getString(R.string.notification_content_failure))
                                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                                .setChannelId(CHANNEL_ID)
                                .build();
                    } else {
                        notification = new Notification.Builder(BackgroundSendService.this)
                                .setContentTitle(getResources().getString(R.string.notification_title_failure))
                                .setContentText(getResources().getString(R.string.notification_content_failure))
                                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                                .build();
                    }
                    mNotificationManager.notify(notifyID, notification);
                }
            }
        };
        destroyHandler.postDelayed(destroyRunnable, 30*1000); // 30 second doom timer ;)
    }

    @Override
    public void onDestroy() {
        Log.d("service", "onDestroy");

        // Remove location listener
        LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.removeUpdates(mLocationListener);

        super.onDestroy();
    }

    private void sendSms() {

        Log.d("service", "sendSms()");

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String phoneNo = SP.getString("favPhoneNo", "null"); // favorite number
        String finalString = String.valueOf(lat) + ", " + String.valueOf(lon);
        String mapLink = SP.getString("mapService",
                "https://www.bing.com/maps?q=YYY,XXX"); //get URL string
        mapLink = mapLink.replace("YYY", String.valueOf(lat)); //replace in latitude
        mapLink = mapLink.replace("XXX", String.valueOf(lon)); //replace in longitude
        finalString = finalString + " // " + mapLink; //concatenate the strings
        String content = finalString;

        if (phoneNo.equals("")) {
            // Send notification of failure
            int notifyID = 1;// Sets an ID for the notification, so it can be updated.
            String CHANNEL_ID = "my_channel_01";// The id of the channel.
            CharSequence name = getString(R.string.notification_title_channelname);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mNotificationManager.createNotificationChannel(mChannel);
                notification = new Notification.Builder(BackgroundSendService.this)
                        .setContentTitle(getResources().getString(R.string.notification_title_failure))
                        .setContentText(getResources().getString(R.string.notification_content_failure))
                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                        .setChannelId(CHANNEL_ID)
                        .build();
            }
            else {
                notification = new Notification.Builder(BackgroundSendService.this)
                        .setContentTitle(getResources().getString(R.string.notification_title_failure))
                        .setContentText(getResources().getString(R.string.notification_content_failure))
                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                        .build();
            }
            mNotificationManager.notify(notifyID , notification);
        }
        else {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNo, null, content, null, null);

                // Send notification of success
                int notifyID = 1;// Sets an ID for the notification, so it can be updated.
                String CHANNEL_ID = "my_channel_01";// The id of the channel.
                CharSequence name = getString(R.string.notification_title_channelname);// The user-visible name of the channel.
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                    mNotificationManager.createNotificationChannel(mChannel);
                    notification = new Notification.Builder(BackgroundSendService.this)
                            .setContentTitle(getResources().getString(R.string.notification_title_success))
                            .setContentText(getResources().getString(R.string.notification_content_success))
                            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                            .setChannelId(CHANNEL_ID)
                            .build();
                }
                else {
                    notification = new Notification.Builder(BackgroundSendService.this)
                            .setContentTitle(getResources().getString(R.string.notification_title_success))
                            .setContentText(getResources().getString(R.string.notification_content_success))
                            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                            .build();
                }
                mNotificationManager.notify(notifyID , notification);
            } catch (Exception e) {
                e.printStackTrace();

                // Send notification of failure
                int notifyID = 1;// Sets an ID for the notification, so it can be updated.
                String CHANNEL_ID = "my_channel_01";// The id of the channel.
                CharSequence name = getString(R.string.notification_title_channelname);// The user-visible name of the channel.
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                    mNotificationManager.createNotificationChannel(mChannel);
                    notification = new Notification.Builder(BackgroundSendService.this)
                            .setContentTitle(getResources().getString(R.string.notification_title_failure))
                            .setContentText(getResources().getString(R.string.notification_content_failure))
                            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                            .setChannelId(CHANNEL_ID)
                            .build();
                }
                else {
                    notification = new Notification.Builder(BackgroundSendService.this)
                            .setContentTitle(getResources().getString(R.string.notification_title_failure))
                            .setContentText(getResources().getString(R.string.notification_content_failure))
                            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                            .build();
                }
                mNotificationManager.notify(notifyID , notification);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    public BackgroundSendService() {

    }

    private final LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(final Location location) {
            Log.d("service", "location changed");
            lat = location.getLatitude();
            lon = location.getLongitude();

            Log.d("service/taskFinished", String.valueOf(taskFinished));

            // TODO remove check for task finished when looping problem fixed
            if (lat != 0.00 && lon != 0.00 && !taskFinished) {
                taskFinished = true;
                sendSms();
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

            // TODO Why is this code running regardless of whether the provider is disabled or enabled?

            /*
            Log.d("service", "Provider Disabled");
            taskFinished = true;

            // Send notification of failure
            int notifyID = 1;// Sets an ID for the notification, so it can be updated.
            String CHANNEL_ID = "my_channel_01";// The id of the channel.
            CharSequence name = getString(R.string.notification_title_channelname);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mNotificationManager.createNotificationChannel(mChannel);
                notification = new Notification.Builder(BackgroundSendService.this)
                        .setContentTitle(getResources().getString(R.string.notification_title_failure))
                        .setContentText(getResources().getString(R.string.notification_content_failure))
                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                        .setChannelId(CHANNEL_ID)
                        .build();
            }
            else {
                notification = new Notification.Builder(BackgroundSendService.this)
                        .setContentTitle(getResources().getString(R.string.notification_title_failure))
                        .setContentText(getResources().getString(R.string.notification_content_failure))
                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                        .build();
            }
            mNotificationManager.notify(notifyID , notification);
            */
        }
    };
}
