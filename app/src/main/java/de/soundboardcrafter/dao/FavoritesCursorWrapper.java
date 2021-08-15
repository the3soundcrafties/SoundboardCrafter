package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.FavoritesTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardFavoritesTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.Favorites;
import de.soundboardcrafter.model.Soundboard;

/**
 * Essentially a cursor over favorites.
 */
@WorkerThread
class FavoritesCursorWrapper extends CursorWrapper {
    FavoritesCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    /**
     * SQL for getting all favorites if favoritesId ist Null or for a certain favoritesId
     */
    static String queryString(@Nullable UUID favoritesId) {
        String query = "SELECT sb." + SoundboardTable.Cols.ID
                + ", sb." + SoundboardTable.Cols.NAME
                + ", g." + FavoritesTable.Cols.ID
                + ", g." + FavoritesTable.Cols.NAME
                + " " //
                + "FROM " + FavoritesTable.NAME + " g "
                + "LEFT JOIN " + SoundboardFavoritesTable.NAME + " sbg "
                + "ON sbg." + SoundboardFavoritesTable.Cols.FAVORITES_ID + " = g."
                + FavoritesTable.Cols.ID
                + " "
                + "LEFT JOIN " + SoundboardTable.NAME + " sb "
                + "ON sb." + SoundboardTable.Cols.ID + " = sbg."
                + SoundboardFavoritesTable.Cols.SOUNDBOARD_ID + " ";
        if (favoritesId != null) {
            query += "WHERE g." + FavoritesTable.Cols.ID + "= ?";
        }
        query += "ORDER BY g." + FavoritesTable.Cols.ID;
        return query;
    }

    Row getRow() {
        Favorites favorites = getFavorites();
        @Nullable Soundboard soundboard = getSoundboard();

        return new Row(favorites, soundboard);
    }

    @NonNull
    private Favorites getFavorites() {
        UUID favoritesId =
                UUID.fromString(getString(2));
        String favoritesName = getString(3);

        return new Favorites(favoritesId, favoritesName);
    }

    @Nullable
    private Soundboard getSoundboard() {
        if (isNull(0)) {
            return null;
        }

        UUID soundboardId = UUID.fromString(getString(0));

        String soundboardName = getString(1);

        return new Soundboard(soundboardId, soundboardName);
    }

    /**
     * A row of this wrapped cursor, containing favorites
     * and maybe a soundboard.
     */
    static class Row {
        @NonNull
        private final Favorites favorites;

        @Nullable
        private final Soundboard soundboard;

        Row(@NonNull Favorites favorites, @Nullable Soundboard soundboard) {
            this.favorites = favorites;
            this.soundboard = soundboard;
        }

        @NonNull
        public Favorites getFavorites() {
            return favorites;
        }

        @Nullable
        public Soundboard getSoundboard() {
            return soundboard;
        }

        @Override
        @NonNull
        public String toString() {
            return "Row{" +
                    "favorites=" + favorites +
                    ", soundboard=" + soundboard +
                    '}';
        }
    }


}
