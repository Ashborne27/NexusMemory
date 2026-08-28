package com.ashborne.nexusmemory;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.util.Log;

public class NexusBridge {
    private final Context context;
    private final NexusDatabase nexusDatabase;
    private static final String TAG = "NexusBridge";

    public NexusBridge(Context context) {
        this.context = context;
        this.nexusDatabase = new NexusDatabase(context);
    }

    @JavascriptInterface
    public void saveData(String key, String value) {
        try {
            nexusDatabase.putData(key, value);
            Log.e(TAG, "Donnée persistée nativement pour la clé: " + key);
        } catch (Exception e) {
            Log.e(TAG, "Erreur de persistance native: " + e.getMessage());
        }
    }

    @JavascriptInterface
    public String getData(String key) {
        try {
            String val = nexusDatabase.getData(key);
            return val != null ? val : "";
        } catch (Exception e) {
            Log.e(TAG, "Erreur de lecture native: " + e.getMessage());
            return "";
        }
    }

    @JavascriptInterface
    public void logToNative(String message) {
        Log.e("NexusWebLog", message);
    }
}
