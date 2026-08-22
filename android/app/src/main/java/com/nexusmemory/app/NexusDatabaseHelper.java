package com.nexusmemory.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NexusDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "nexus_memory.db";
    private static final int DATABASE_VERSION = 1;

    public NexusDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE VIRTUAL TABLE memories_fts USING fts5(title, content);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS memories_fts;");
        onCreate(db);
    }
}
