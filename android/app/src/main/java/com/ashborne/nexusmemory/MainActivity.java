package com.ashborne.nexusmemory;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.util.Log;
import com.getcapacitor.BridgeActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "NexusMain";
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Isolation asynchrone des charges lourdes pour garantir un HomeLauncher instantané
        backgroundExecutor.execute(() -> {
            try {
                Thread.sleep(150); // Stabilisation initiale du démon
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 2. Injection sécurisée du bridge et accélération matérielle GPU sur le thread principal
        runOnUiThread(() -> {
            try {
                WebView webView = findViewById(com.getcapacitor.R.id.webview);
                if (webView != null) {
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.addJavascriptInterface(new NexusBridge(this), "AndroidNexus");
                    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    Log.e(TAG, "NexusBridge injecté et accélération GPU activée avec succès.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l injection du bridge: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
    }
}
