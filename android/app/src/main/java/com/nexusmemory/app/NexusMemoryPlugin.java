package com.nexusmemory.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NexusMemoryPlugin")
public class NexusMemoryPlugin extends Plugin {
    private NexusDatabaseHelper dbHelper;

    @Override
    public void load() {
        super.load();
        dbHelper = new NexusDatabaseHelper(getContext());
    }

    @PluginMethod
    public void saveMemory(PluginCall call) {
        String title = call.getString("title");
        String content = call.getString("content");

        if (title == null || content == null) {
            call.reject("Title and content are required");
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        long id = db.insert("memories_fts", null, values);

        JSObject ret = new JSObject();
        ret.put("success", id != -1);
        ret.put("id", id);
        call.resolve(ret);
    }

    @PluginMethod
    public void searchMemories(PluginCall call) {
        String query = call.getString("query");
        if (query == null) {
            call.reject("Query string is required");
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, content FROM memories_fts WHERE memories_fts MATCH ?;", new String[]{query + "*"});
        
        JSArray results = new JSArray();
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    JSObject item = new JSObject();
                    item.put("title", cursor.getString(0));
                    item.put("content", cursor.getString(1));
                    results.put(item);
                }
            } finally {
                cursor.close();
            }
        }

        JSObject ret = new JSObject();
        ret.put("results", results);
        call.resolve(ret);
    }
}
