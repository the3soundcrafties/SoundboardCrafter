package de.soundboardcrafter.dao;

import android.database.Cursor;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.GameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardGameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;

/**
 * Essentially a cursor over sounds.
 */
@WorkerThread
class GameCursorWrapper {
    private final Cursor cursor;

    GameCursorWrapper(Cursor cursor) {
        this.cursor = cursor;
    }


    UUID getSoundboardId() {
        return UUID.fromString(cursor.getString(0));
    }

    String getSoundboardName() {
        return cursor.getString(1);
    }

    UUID getGameId() {
        return UUID.fromString(cursor.getString(2));
    }

    String getGameName() {
        return cursor.getString(3);
    }


    boolean hasSoundboard() {
        return !cursor.isNull(0);
    }

    /**
     * SQL for getting all games if gameId ist Null or for a certain gameId
     */
    static String queryString(@Nullable UUID gameId) {
        String query = "SELECT sb." + SoundboardTable.Cols.ID
                + ", sb." + SoundboardTable.Cols.NAME
                + ", g." + GameTable.Cols.ID
                + ", g." + GameTable.Cols.NAME
                + " " //
                + "FROM " + GameTable.NAME + " g "
                + "LEFT JOIN " + SoundboardGameTable.NAME + " sbg "
                + "ON sbg." + SoundboardGameTable.Cols.GAME_ID + " = g." + GameTable.Cols.ID + " "
                + "LEFT JOIN " + SoundboardTable.NAME + " sb "
                + "ON sb." + SoundboardTable.Cols.ID + " = sbg." + SoundboardGameTable.Cols.SOUNDBOARD_ID + " ";
        if (gameId != null) {
            query += "WHERE g." + SoundboardGameTable.Cols.GAME_ID + "=" + gameId;
        }
        query += "ORDER BY g." + GameTable.Cols.ID;
        return query;
    }

    boolean moveToNext() {
        return cursor.moveToNext();
    }
}
