package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

/**
 * Abstract superclass for data access objects.
 */
abstract class AbstractDao {
    private final SQLiteDatabase database;

    AbstractDao(@Nonnull Context context) {
        Context appContext = context.getApplicationContext();
        // "If you’re using Sqlite on Android, you do not need to close your db connection.  You
        // *can*, but managing that will be difficult for you for a number of reasons.  You
        // *should* simply create a singleton instance of SQLiteOpenHelper, or some derivative,
        // and reuse that as needed."
        // (https://kpgalligan.tumblr.com/post/109546839958/single-database-connection )
        database = new DBHelper(appContext).getWritableDatabase();
    }

    SQLiteDatabase getDatabase() {
        return database;
    }

    Cursor rawQueryOrThrow(String queryString, Object... selectionArgObjects) {
        return rawQueryOrThrow(queryString,
                Stream.of(selectionArgObjects)
                        .map(Object::toString)
                        .toArray(String[]::new));
    }

    Cursor rawQueryOrThrow(String queryString, String... selectionArgs) {
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
