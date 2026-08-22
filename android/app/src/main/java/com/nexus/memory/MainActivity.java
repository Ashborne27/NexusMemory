package com.nexus.memory;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Déclenchement souverain et immédiat du Foreground Service
        Intent serviceIntent = new Intent(this, NexusForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // Optimisation souveraine : Libération proactive de la RAM en cas de pression système
        if (level >= TRIM_MEMORY_RUNNING_MODERATE || level >= TRIM_MEMORY_COMPLETE) {
            System.gc();
        }
    }
}
