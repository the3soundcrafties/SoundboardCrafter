package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;

import com.google.common.collect.ImmutableList;

import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.dao.DBSchema.SoundboardGameTable;
import de.soundboardcrafter.model.Game;
import de.soundboardcrafter.model.GameWithSoundboards;
import de.soundboardcrafter.model.Soundboard;

public class GameDao extends AbstractDao {
    private static GameDao instance;
    private static SoundboardDao soundboardDao;

    public static GameDao getInstance(final Context context) {
        if (instance == null) {
            instance = new GameDao(context);
            instance.init(context);
        }
        return instance;
    }

    private GameDao(@Nonnull Context context) {
        super(context);
    }

    private void init(@Nonnull Context context) {
        soundboardDao = SoundboardDao.getInstance(context);
    }


    /**
     * Updates this gameWithSoundboards (which must already exist in the database) and updates the
     * soundboard links.
     */
    public void updateGameWithSoundboards(GameWithSoundboards gameWithSoundboards) {
        updateGame(gameWithSoundboards.getGame());
        //unlink soundboards
        getDatabase().delete(SoundboardGameTable.NAME, DBSchema.GameTable.Cols.ID + " = ?",
                new String[]{gameWithSoundboards.getGame().getId().toString()});
        linkSoundboardsToGame(gameWithSoundboards);
    }

    /**
     * Updates this sound which has to exist in the database.
     */
    private void updateGame(Game game) {
        int rowsUpdated = getDatabase().update(DBSchema.GameTable.NAME,
                buildContentValues(game),
                DBSchema.GameTable.Cols.ID + " = ?",
                new String[]{game.getId().toString()});

        if (rowsUpdated != 1) {
            throw new RuntimeException("Not exactly one sound with ID + " + game.getId());
        }
    }

    public void insertWithSoundboards(GameWithSoundboards gameWithSoundboards) {
        insert(gameWithSoundboards.getGame());
        linkSoundboardsToGame(gameWithSoundboards);
    }

    private void insert(Game game) {
        // TODO throw exception if game name already exists
        insertOrThrow(DBSchema.GameTable.NAME, buildContentValues(game));
    }

    private void linkSoundboardsToGame(GameWithSoundboards gameWithSoundboards) {
        for (Soundboard soundboard : gameWithSoundboards.getSoundboards()) {
            linkGameToSoundboard(soundboard.getId(), gameWithSoundboards.getGame().getId());
        }
    }

    private void linkGameToSoundboard(UUID soundboardId, UUID gameId) {
        // TODO throw exception if the game is already contained in the soundboard
        // (at any index)
        ContentValues values = new ContentValues();
        values.put(SoundboardGameTable.Cols.SOUNDBOARD_ID, soundboardId.toString());
        values.put(SoundboardGameTable.Cols.GAME_ID, gameId.toString());

        insertOrThrow(SoundboardGameTable.NAME, values);
    }

    private ContentValues buildContentValues(Game game) {
        ContentValues values = new ContentValues();
        values.put(DBSchema.GameTable.Cols.ID, game.getId().toString());
        values.put(DBSchema.GameTable.Cols.NAME, game.getName());
        return values;
    }

    void deleteAllGames() {
        getDatabase().delete(DBSchema.GameTable.NAME, null, new String[]{});
    }

    public GameWithSoundboards findGameWithSoundboards(UUID gameId) {
        final GameCursorWrapper cursor =
                new GameCursorWrapper(
                        rawQueryOrThrow(GameCursorWrapper.queryString(gameId)));
        ImmutableList<GameWithSoundboards> result = findGamesWithSoundboards(cursor);
        if (result.size() > 1) {
            throw new IllegalStateException("More than one game was found");
        }
        return result.get(0);
    }

    public ImmutableList<GameWithSoundboards> findAllGamesWithSoundboards() {
        final GameCursorWrapper cursor =
                new GameCursorWrapper(
                        rawQueryOrThrow(GameCursorWrapper.queryString(null)));
        return findGamesWithSoundboards(cursor);
    }


    private ImmutableList<GameWithSoundboards> findGamesWithSoundboards(GameCursorWrapper cursor) {
        ImmutableList.Builder<GameWithSoundboards> res = ImmutableList.builder();
        GameWithSoundboards currentGame = null;
        while (cursor.moveToNext()) {
            GameCursorWrapper.Row row = cursor.getRow();
            if (currentGame == null || !currentGame.getGame().getId().equals(row.getGame().getId())) {
                currentGame = new GameWithSoundboards(row.getGame());
                res.add(currentGame);
            }
            if (row.getSoundboard() != null) {
                currentGame.addSoundboard(row.getSoundboard());
            }
        }
        return res.build();
    }


    void unlinkAllGames() {
        getDatabase().delete(SoundboardGameTable.NAME, null, new String[]{});
    }


}
