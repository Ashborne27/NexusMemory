package com.ashborne.nexusmemory;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NexusBridge {
    private final Context context;
    private final NexusDatabase nexusDatabase;
    private final ExecutorService executorService;
    private static final String TAG = "NexusBridge";

    public NexusBridge(Context context) {
        this.context = context;
        this.nexusDatabase = new NexusDatabase(context);
        this.executorService = Executors.newFixedThreadPool(4);
    }

    @JavascriptInterface
    public void saveData(String key, String value) {
        executorService.execute(() -> {
            try {
                nexusDatabase.putData(key, value);
            } catch (Exception e) {
                Log.e(TAG, "Erreur d ecriture asynchrone: " + e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public String getData(String key) {
        try {
            return nexusDatabase.getData(key);
        } catch (Exception e) {
            Log.e(TAG, "Erreur de lecture: " + e.getMessage());
            return "";
        }
    }

    @JavascriptInterface
    public void logToNative(String message) {
        Log.e("NexusWebLog", message);
    }
}
