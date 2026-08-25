package com.nexusmemory.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerPlugin(NexusTelemetryBridge.class);
        startNexusDaemon();
    }

    private void startNexusDaemon() {
        try {
            Intent serviceIntent = new Intent(this, NexusForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
