package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;

import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.dao.DBSchema.SoundboardFavoritesTable;
import de.soundboardcrafter.model.Favorites;
import de.soundboardcrafter.model.FavoritesWithSoundboards;
import de.soundboardcrafter.model.Soundboard;

public class FavoritesDao extends AbstractDao {
    private static FavoritesDao instance;

    private static final String SELECT_FAVORITES_NAME =
            "SELECT g." + DBSchema.FavoritesTable.Cols.NAME
                    + " " //
                    + "FROM " + DBSchema.FavoritesTable.NAME + " g "
                    + "WHERE g." + DBSchema.FavoritesTable.Cols.ID + "= ?";

    public static FavoritesDao getInstance(final Context context) {
        if (instance == null) {
            instance = new FavoritesDao(context);
        }
        return instance;
    }

    private FavoritesDao(@Nonnull Context context) {
        super(context);
    }

    /**
     * Updates this favoritesWithSoundboards (which must already exist in the database) and
     * updates the
     * soundboard links.
     */
    public void updateWithSoundboards(@NonNull FavoritesWithSoundboards favoritesWithSoundboards) {
        update(favoritesWithSoundboards.getFavorites());
        //unlink soundboards
        getDatabase().delete(SoundboardFavoritesTable.NAME,
                SoundboardFavoritesTable.Cols.FAVORITES_ID + " = ?",
                new String[]{favoritesWithSoundboards.getFavorites().getId().toString()});
        linkSoundboardsToFavorites(favoritesWithSoundboards);
    }

    /**
     * Updates this sound which has to exist in the database.
     */
    private void update(Favorites favorites) {
        int rowsUpdated = getDatabase().update(DBSchema.FavoritesTable.NAME,
                buildContentValues(favorites),
                DBSchema.FavoritesTable.Cols.ID + " = ?",
                new String[]{favorites.getId().toString()});

        if (rowsUpdated != 1) {
            throw new RuntimeException(
                    "Not exactly one favorites instance with ID + " + favorites.getId());
        }
    }

    public void insertWithSoundboards(@NonNull FavoritesWithSoundboards favoritesWithSoundboards) {
        insert(favoritesWithSoundboards.getFavorites());
        linkSoundboardsToFavorites(favoritesWithSoundboards);
    }


    private void insert(Favorites favorites) {
        // TODO throw exception if favorites name already exists
        insertOrThrow(DBSchema.FavoritesTable.NAME, buildContentValues(favorites));
    }

    private void linkSoundboardsToFavorites(
            @NonNull FavoritesWithSoundboards favoritesWithSoundboards) {
        for (Soundboard soundboard : favoritesWithSoundboards.getSoundboards()) {
            linkFavoritesToSoundboard(soundboard.getId(),
                    favoritesWithSoundboards.getFavorites().getId());
        }
    }

    private void linkFavoritesToSoundboard(@NonNull UUID soundboardId, @NonNull UUID favoritesId) {
        // TODO throw exception if the favorites are already linked to the soundboard
        //  (at any index)
        ContentValues values = new ContentValues();
        values.put(SoundboardFavoritesTable.Cols.SOUNDBOARD_ID, soundboardId.toString());
        values.put(SoundboardFavoritesTable.Cols.FAVORITES_ID, favoritesId.toString());

        insertOrThrow(SoundboardFavoritesTable.NAME, values);
    }

    @NonNull
    private ContentValues buildContentValues(@NonNull Favorites favorites) {
        ContentValues values = new ContentValues();
        values.put(DBSchema.FavoritesTable.Cols.ID, favorites.getId().toString());
        values.put(DBSchema.FavoritesTable.Cols.NAME, favorites.getName());
        return values;
    }

    void deleteAllFavorites() {
        getDatabase().delete(DBSchema.FavoritesTable.NAME, null, new String[]{});
    }

    public String findFavoritesName(UUID favoritesId) {
        try (Cursor cursor = rawQueryOrThrow(
                SELECT_FAVORITES_NAME,
                favoritesId)) {
            if (cursor.moveToNext()) {
                return cursor.getString(0);
            }

            throw new IllegalStateException("No favorites with id " + favoritesId + " found");
        }
    }

    public FavoritesWithSoundboards findFavoritesWithSoundboards(UUID favoritesId) {
        try (FavoritesCursorWrapper cursor =
                     new FavoritesCursorWrapper(
                             rawQueryOrThrow(
                                     FavoritesCursorWrapper.queryString(favoritesId),
                                     favoritesId))) {
            ImmutableList<FavoritesWithSoundboards> result = findFavoritesWithSoundboards(cursor);
            if (result.size() > 1) {
                throw new IllegalStateException("More than one favorites instance was found");
            }
            return result.get(0);
        }
    }

    public ImmutableList<FavoritesWithSoundboards> findAllFavoritesWithSoundboards() {
        try (FavoritesCursorWrapper cursor = new FavoritesCursorWrapper(
                rawQueryOrThrow(FavoritesCursorWrapper.queryString(null)))) {
            return findFavoritesWithSoundboards(cursor);
        }

    }

    private ImmutableList<FavoritesWithSoundboards> findFavoritesWithSoundboards(
            @NonNull FavoritesCursorWrapper cursor) {
        ImmutableList.Builder<FavoritesWithSoundboards> res = ImmutableList.builder();
        FavoritesWithSoundboards currentFavorites = null;
        while (cursor.moveToNext()) {
            FavoritesCursorWrapper.Row row = cursor.getRow();
            if (currentFavorites == null || !currentFavorites.getFavorites().getId()
                    .equals(row.getFavorites().getId())) {
                currentFavorites = new FavoritesWithSoundboards(row.getFavorites());
                res.add(currentFavorites);
            }
            if (row.getSoundboard() != null) {
                currentFavorites.addSoundboard(row.getSoundboard());
            }
        }
        return res.build();
    }


    void unlinkAllFavorites() {
        getDatabase().delete(SoundboardFavoritesTable.NAME, null, new String[]{});
    }

    public void remove(UUID favoritesId) {
        unlinkAllSoundboards(favoritesId);
        getDatabase().delete(DBSchema.FavoritesTable.NAME, DBSchema.FavoritesTable.Cols.ID + " = ?",
                new String[]{favoritesId.toString()});

    }

    private void unlinkAllSoundboards(@NonNull UUID favoritesId) {
        getDatabase().delete(SoundboardFavoritesTable.NAME,
                SoundboardFavoritesTable.Cols.FAVORITES_ID + " = ?",
                new String[]{favoritesId.toString()});
    }
}
