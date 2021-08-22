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
                + ", sb." + SoundboardTable.Cols.PROVIDED
                + ", f." + FavoritesTable.Cols.ID
                + ", f." + FavoritesTable.Cols.NAME
                + " " //
                + "FROM " + FavoritesTable.NAME + " f "
                + "LEFT JOIN " + SoundboardFavoritesTable.NAME + " sbf "
                + "ON sbf." + SoundboardFavoritesTable.Cols.FAVORITES_ID + " = f."
                + FavoritesTable.Cols.ID
                + " "
                + "LEFT JOIN " + SoundboardTable.NAME + " sb "
                + "ON sb." + SoundboardTable.Cols.ID + " = sbf."
                + SoundboardFavoritesTable.Cols.SOUNDBOARD_ID + " ";
        if (favoritesId != null) {
            query += "WHERE f." + FavoritesTable.Cols.ID + "= ?";
        }
        query += "ORDER BY f." + FavoritesTable.Cols.ID;
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
                UUID.fromString(getString(3));
        String favoritesName = getString(4);

        return new Favorites(favoritesId, favoritesName);
    }

    @Nullable
    private Soundboard getSoundboard() {
        if (isNull(0)) {
            return null;
        }

        UUID soundboardId = UUID.fromString(getString(0));
        String soundboardName = getString(1);
        boolean provided = getInt(2) == 0 ? false : true;

        return new Soundboard(soundboardId, soundboardName, provided);
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
