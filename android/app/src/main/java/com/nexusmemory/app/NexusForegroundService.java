package com.nexusmemory.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.content.Context;
import android.content.pm.ServiceInfo;
import androidx.core.app.NotificationCompat;

public class NexusForegroundService extends Service {
    private static final String CHANNEL_ID = "NexusMemorySovereignUltimateChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannel();
            startForegroundServiceInstance();
            scheduleHeartbeat();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            createNotificationChannel();
            startForegroundServiceInstance();
            scheduleHeartbeat();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "NexusMemory Ultimate Persistence",
                        NotificationManager.IMPORTANCE_HIGH
                );
                serviceChannel.setDescription("Canal souverain haute priorité 24/7");
                serviceChannel.setShowBadge(true);
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void startForegroundServiceInstance() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NexusMemory [Souverain 24/7]")
                .setContentText("Noyau permanent et pulsations actives.")
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
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void scheduleHeartbeat() {
        try {
            Intent intent = new Intent(this, NexusBootReceiver.class);
            intent.setAction("com.nexusmemory.app.RESTART_SERVICE");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                long triggerAtMillis = System.currentTimeMillis() + (15 * 60 * 1000);
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Intent broadcastIntent = new Intent(this, NexusBootReceiver.class);
            sendBroadcast(broadcastIntent);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
