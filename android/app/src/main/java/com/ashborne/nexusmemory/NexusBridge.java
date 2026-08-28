package com.ashborne.nexusmemory;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;
import android.util.Log;

public class NexusBridge {
    private static final String TAG = "NexusBridge";
    private static final String PREF_NAME = "NexusSecureStorage";
    private Context context;

    public NexusBridge(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void saveData(String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, value).apply();
        // Log supprimé pour garantir une fluidité absolue à haute fréquence
    }

    @JavascriptInterface
    public String getData(String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, "{}");
    }

    @JavascriptInterface
    public void logToNative(String message) {
        Log.e("NexusWebLog", message);
    }
}
