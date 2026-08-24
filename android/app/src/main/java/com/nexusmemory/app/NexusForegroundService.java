package com.nexusmemory.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import androidx.core.app.NotificationCompat;

public class NexusForegroundService extends Service {
    private static final String CHANNEL_ID = "NexusMemorySovereignUltimateChannel";
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        acquireWakeLock();
        createNotificationChannel();
        startForegroundServiceInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        acquireWakeLock();
        startForegroundServiceInstance();
        return START_STICKY;
    }

    private void acquireWakeLock() {
        try {
            if (wakeLock == null) {
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NexusMemory::SovereignWakeLock");
                    wakeLock.acquire();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startForegroundServiceInstance() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NexusMemory [Souverain 24/7]")
                .setContentText("Noyau permanent et verrouillage CPU actif.")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1337, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(1337, notification);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "NexusMemory Ultimate Persistence",
                    NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setDescription("Canal souverain haute priorité 24/7");
            serviceChannel.setShowBadge(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
