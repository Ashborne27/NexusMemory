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
        Log.d(TAG, "NexusForegroundService -> onCreate() initialisé avec succès.");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "NexusForegroundService -> onStartCommand() actif en arrière-plan.");
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NexusMemory • Noyau Souverain")
                .setContentText("Daemon: ACTIVE | RAM: Optimisée")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        // Le service redémarrera automatiquement si le système le tue par manque de mémoire
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NexusForegroundService -> onDestroy() - Le démon s est éteint.");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nexus Daemon Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
