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
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
        Log.e(TAG, "Donnée persistée [" + key + "] : " + value);
    }

    @JavascriptInterface
    public String getData(String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(key, "{}");
        Log.e(TAG, "Donnée récupérée [" + key + "] : " + value);
        return value;
    }

    @JavascriptInterface
    public void logToNative(String message) {
        Log.e("NexusWebLog", message);
    }
}
