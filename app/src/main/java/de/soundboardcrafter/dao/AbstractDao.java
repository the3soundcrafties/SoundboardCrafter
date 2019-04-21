package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import javax.annotation.Nonnull;

/**
 * Abstract superclass for data access objects.
 */
abstract class AbstractDao {
    private final SQLiteDatabase database;

    AbstractDao(@Nonnull Context context) {
        Context appContext = context.getApplicationContext();
        database = new DBHelper(appContext).getWritableDatabase();
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    Cursor rawQueryOrThrow(String queryString) {
        final Cursor cursor = database.rawQuery(queryString, new String[]{});
        if (cursor == null) {
            throw new RuntimeException("Could not query database: " + queryString);
        }
        return cursor;
    }

    /**
     * Inserts these values as a new entry into this table.
     *
     * @throws RuntimeException if inserting does not succeed
     */
    void insertOrThrow(final String table, final ContentValues values) {
        final long rowId = database.insertOrThrow(table, null, values);
        if (rowId == -1) {
            throw new RuntimeException("Could not insert into database: " + values);
        }
    }

}
