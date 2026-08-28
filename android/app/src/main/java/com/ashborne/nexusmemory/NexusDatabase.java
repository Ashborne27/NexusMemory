package com.ashborne.nexusmemory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NexusDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "nexus_core.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "corpus_index";
    private static final String TAG = "NexusDB";

    public NexusDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "key TEXT PRIMARY KEY NOT NULL, " +
                "value TEXT NOT NULL, " +
                "timestamp INTEGER NOT NULL)";
        db.execSQL(createTableQuery);
        Log.e(TAG, "Table native corpus_index initialisée avec succès.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public synchronized void putData(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        values.put("timestamp", System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized String getData(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{"value"}, "key = ?", new String[]{key}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }
}
