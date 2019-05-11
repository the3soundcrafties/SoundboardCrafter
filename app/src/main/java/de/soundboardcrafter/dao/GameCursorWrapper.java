package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.GameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardGameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.Game;
import de.soundboardcrafter.model.Soundboard;

/**
 * Essentially a cursor over games.
 */
@WorkerThread
class GameCursorWrapper extends CursorWrapper {
    GameCursorWrapper(Cursor cursor) {
        super(cursor);


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

    Row getRow() {
        Game game = getGame();
        @Nullable Soundboard soundboard = getSoundboard();

        return new Row(game, soundboard);
    }

    @NonNull
    private Game getGame() {
            UUID gameId =
                    UUID.fromString(getString(2));
        String gameName = getString(3);

        return new Game(gameId, gameName);
    }

    @Nullable
    private Soundboard getSoundboard() {
        if (isNull(getColumnIndex(SoundboardTable.Cols.ID))) {
            return null;
        }

        UUID soundboardId =  UUID.fromString(getString(0));

        String soundboardName = getString(1));

        return new Soundboard(soundboardId, soundboardName);
    }

    /**
     * A row of this wrapped cursor, containing a game
     * and maybe a soundboard.
     */
    static class Row {
        @NonNull
        private final Game game;

        @Nullable
        private final Soundboard soundboard;

        Row(@NonNull Game game, @Nullable Soundboard soundboard) {
            this.game = game;
            this.soundboard = soundboard;
        }

        @NonNull
        public Game getGame() {
            return game;
        }

        @Nullable
        public Soundboard getSoundboard() {
            return soundboard;
        }

        @Override
        public String toString() {
            return "Row{" +
                    "game=" + game +
                    ", soundboard=" + soundboard +
                    '}';
        }
    }


}
