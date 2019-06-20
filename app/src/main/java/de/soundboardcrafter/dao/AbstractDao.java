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

    SQLiteDatabase getDatabase() {
        return database;
    }

    Cursor rawQueryOrThrow(String queryString) {
        return rawQueryOrThrow(queryString, new String[]{});
    }

    Cursor rawQueryOrThrow(String queryString, String[] selectionArgs) {
        final Cursor cursor = database.rawQuery(queryString, selectionArgs);
        if (cursor == null) {
            throw new RuntimeException("Could not query database: " + queryString);
        }
        return cursor;
    }

    /**
     * Inserts these values as a new entry into this table.
     *
     * @throws IllegalStateException if inserting does not succeed
     */
    void insertOrThrow(final String table, final ContentValues values) {
        final long rowId = database.insertOrThrow(table, null, values);
        if (rowId == -1) {
            throw new IllegalStateException("Could not insert into database: " + values);
        }
    }
}
