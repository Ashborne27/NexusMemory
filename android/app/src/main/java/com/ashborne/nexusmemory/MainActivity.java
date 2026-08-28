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

        // Isolation des tâches lourdes en arrière-plan
        backgroundExecutor.execute(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Injection du bridge et accélération matérielle via l API officielle Capacitor
        backgroundExecutor.execute(() -> {
            int retries = 15;
            while (retries > 0 && (getBridge() == null || getBridge().getWebView() == null)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
                retries--;
            }
            
            if (getBridge() != null && getBridge().getWebView() != null) {
                runOnUiThread(() -> {
                    WebView webView = getBridge().getWebView();
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.addJavascriptInterface(new NexusBridge(MainActivity.this), "AndroidNexus");
                    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    Log.e(TAG, "NexusBridge injecté et accélération GPU activée via Bridge API.");
                });
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
