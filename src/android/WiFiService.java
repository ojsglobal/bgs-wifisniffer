package com.schwartzer.bgs.wifisniff;


import com.red_folder.phonegap.plugin.backgroundservice.BackgroundService;
//import com.schwartzer.bgs.wifisniff.R;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import android.R;

import android.support.v4.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;


public class WiFiService extends BackgroundService {
    private static final String TAG = "WiFiService";
    private static final int NOTIFICATION_ID = 7124;

    private WifiManager wifi;
    private String wifiSSID;
    private boolean listening;
    private long workingTimestamp, delay;

    private Notification notification;
    private String notificationTitle, notificationText;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationText = "Time to workout.";
        notificationTitle = "PT Push";
        notification = null;
        wifiSSID = "NONE";
        listening = false;
        delay = 0;
        workingTimestamp = 0;

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!listening) {
                    cancelNotification();
                    return;
                }

                // Do not detect anything before delay time passes
                if ((new Date()).getTime() < workingTimestamp)
                    return;

                List<ScanResult> results = wifi.getScanResults();
                boolean networkDetected = false;
                for (int i = 0; i < results.size(); i++) {
                    Log.d("WIFIDSSID", results.get(i).SSID + " = " + wifiSSID);
                    if (results.get(i).SSID.toUpperCase().equals(wifiSSID.toUpperCase())) {
                        workingTimestamp = (new Date()).getTime() + delay;
                        showNotification();
                        networkDetected = true;
                    }
                }
                if (!networkDetected)
                    cancelNotification();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void showPersistentNotification() {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        //Intent intent = new Intent(this.getApplicationContext(), this.getApplicationContext().getPackageName());
        //Intent intent = new Intent(this.getBaseContext(), MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, 0);
        NotificationCompat.Builder nBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.divider_horizontal_dark_opaque)
                        .setContentTitle(notificationTitle)
                        .setContentText("Working correctly.")
                        .setOngoing(true)
                        .setContentIntent(pIntent);

        NotificationManager manager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID + 1, nBuilder.build());
    }

    private void cancelPersistentNotification() {
        NotificationManager manager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID + 1);
        Log.d("WIFIDNOTIFICATION", "Cancelled");
    }

    private void showNotification() {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        //Intent intent = new Intent(this.getBaseContext(), MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, 0);
        NotificationCompat.Builder nBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.ic_popup_reminder)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setContentIntent(pIntent);

        NotificationManager manager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, nBuilder.build());
    }

    private void cancelNotification() {
        NotificationManager manager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
        Log.d("WIFIDNOTIFICATION", "Cancelled");
    }

    @Override
    protected JSONObject initialiseLatestResult() {
        return null;
    }

    @Override
    protected JSONObject doWork() {
        if(!listening)
            return null;
        if(wifi.isWifiEnabled())
            wifi.startScan();

        return null;
    }

    @Override
    protected JSONObject getConfig() {
        JSONObject result = new JSONObject();

        try {
            result.put("ssid", wifiSSID);
        } catch(JSONException _e) {
            return null;
        }

        return result;
    }

    @Override
    protected void setConfig(JSONObject config) {
        try {
            for(int i=0; i<config.names().length(); i++)
                Log.d("WIFIDCONFIG", config.names().getString(i) + ": " + config.getString(config.names().getString(i)));
            if(config.has("ssid"))
                wifiSSID = config.getString("ssid");
            if(config.has("cancel"))
                cancelNotification();
            if(config.has("listening")) {
                listening = (config.getInt("listening") == 0 ? false : true);
                workingTimestamp = (new Date()).getTime();
                if(listening) {
                    showPersistentNotification();
                } else {
                    cancelNotification();
                    cancelPersistentNotification();
                }
            }
            if(config.has("delay"))
                delay = config.getInt("delay") * 60000;
            if(config.has("message"))
                notificationText = config.getString("message");
        } catch(JSONException _e) {
            wifiSSID = null;
        }
    }
}
