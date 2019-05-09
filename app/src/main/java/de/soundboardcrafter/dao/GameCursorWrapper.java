package de.soundboardcrafter.dao;

import android.database.Cursor;

import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.GameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardGameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;

/**
 * Essentially a cursor over games.
 */
@WorkerThread
class GameCursorWrapper {
    private final Cursor cursor;

    GameCursorWrapper(Cursor cursor) {
        this.cursor = cursor;
    }


    UUID getSoundboardId() {
        return UUID.fromString(cursor.getString(cursor.getColumnIndex(SoundboardTable.Cols.ID)));
    }

    String getSoundboardName() {
        return cursor.getString(cursor.getColumnIndex(SoundboardTable.Cols.NAME));
    }

    String getGameName() {
        return cursor.getString(cursor.getColumnIndex(GameTable.Cols.NAME));
    }

    UUID getGameId() {
        return UUID.fromString(cursor.getString(cursor.getColumnIndex(GameTable.Cols.ID)));
    }

    boolean hasSoundboard() {
        return !cursor.isNull(cursor.getColumnIndex(SoundboardTable.Cols.ID));
    }

    static String queryString() {
        return "SELECT sb." + SoundboardTable.Cols.ID
                + ", sb." + SoundboardTable.Cols.NAME
                + ", g." + GameTable.Cols.ID
                + ", g." + GameTable.Cols.NAME
                + " " //
                + "FROM " + GameTable.NAME + " g "
                + "LEFT JOIN " + SoundboardGameTable.NAME + " sbg "
                + "ON sbg." + SoundboardGameTable.Cols.GAME_ID + " = g." + GameTable.Cols.ID + " "
                + "LEFT JOIN " + SoundboardTable.NAME + " sb "
                + "ON sb." + SoundboardTable.Cols.ID + " = sbg." + SoundboardGameTable.Cols.SOUNDBOARD_ID + " " //
                + "ORDER BY g." + GameTable.Cols.ID;
    }

    boolean moveToNext() {
        return cursor.moveToNext();
    }
}
