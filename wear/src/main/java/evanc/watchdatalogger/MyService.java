package evanc.watchdatalogger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

public class MyService extends Service {
    private int NOTIFICATION = 1; // Unique identifier for our notification

    public static boolean isRunning = false;
    public static MyService instance = null;

    private NotificationHelper helper;


    private NotificationManager notificationManager = null;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        instance = this;
        isRunning = true;

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        helper = new NotificationHelper(this);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The PendingIntent to launch our activity if the user selects this notification
        sendNotification(1101, "Service is running");

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        isRunning = false;
        instance = null;
        cancelNotification(1101);
        //notificationManager.cancel(NOTIFICATION); // Remove notification

        super.onDestroy();
    }


    public void doSomething() {
        makeToast("Doing stuff from service...");
    }

    void makeToast(String str) {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }

    public void sendNotification(int id, String title) {
        NotificationCompat.Builder nb = null;
        nb = helper.getNotification(title, "Click here to open app");
        nb.setOngoing(true);

        if (nb != null) {
            helper.notify(id, nb);
        }
    }

    public void cancelNotification(int id) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }
}

