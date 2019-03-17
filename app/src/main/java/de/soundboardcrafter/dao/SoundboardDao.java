package de.soundboardcrafter.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.collect.ImmutableList;

import de.soundboardcrafter.model.Soundboard;

/**
 * Database Access Object for accessing Soundboards in the database
 */
public class SoundboardDao {
    private static SoundboardDao instance;

    private SQLiteDatabase database;

    public static SoundboardDao getInstance(final Context context) {
        if (instance == null) {
            instance = new SoundboardDao(context);
        }

        return instance;
    }

    private SoundboardDao(Context context) {
        database = new DBHelper(context.getApplicationContext()).getWritableDatabase();
    }

    public static ImmutableList<Soundboard> findAll() {
        // TODO Read from database
        return ImmutableList.of();
    }
}
