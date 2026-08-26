package com.ashborne.nexusmemory;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class NexusForegroundService extends Service {
    private static final String TAG = "NexusDaemon";
    private static final String CHANNEL_ID = "NexusDaemonChannel";
    private static final int NOTIFICATION_ID = 1337;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, ">>> NEXUS DAEMON ON_CREATE ACTIF <<<");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, ">>> NEXUS DAEMON ON_START_COMMAND ACTIF <<<");
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NexusMemory • Noyau Souverain")
                .setContentText("Daemon Principal: ACTIF & IMMORTEL")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nexus Daemon Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
