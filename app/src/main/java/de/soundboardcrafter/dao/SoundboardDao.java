package de.soundboardcrafter.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Database Access Object for accessing Soundboards in the database
 */
public class SoundboardDao {
    private SQLiteDatabase database;

    private SoundboardDao(Context context) {
        database = new DBHelper(context.getApplicationContext()).getWritableDatabase();
    }
}
