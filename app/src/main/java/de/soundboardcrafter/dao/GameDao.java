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

    void insertWithSoundboards(GameWithSoundboards gameWithSoundboards) {
        insert(gameWithSoundboards.getGame());
        for (Soundboard soundboard : gameWithSoundboards.getSoundboards()) {
            linkGameToSoundboard(soundboard.getId(), gameWithSoundboards.getGame().getId());
        }
    }

    private void insert(Game game) {
        // TODO throw exception if sound name already exists
        insertOrThrow(DBSchema.GameTable.NAME, buildContentValues(game));
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

    public ImmutableList<GameWithSoundboards> findAllGamesWithSoundboards() {
        final GameCursorWrapper cursor =
                new GameCursorWrapper(
                        rawQueryOrThrow(GameCursorWrapper.queryString()));
        ImmutableList.Builder<GameWithSoundboards> res = ImmutableList.builder();
        GameWithSoundboards currentGame = null;
        while (cursor.moveToNext()) {
            UUID uuid = cursor.getGameId();
            String name = cursor.getGameName();
            if (currentGame == null || !currentGame.getGame().getId().equals(uuid)) {
                currentGame = new GameWithSoundboards(uuid, name);
                res.add(currentGame);
            }
            if (cursor.hasSoundboard()) {
                UUID uuidSoundboard = cursor.getSoundboardId();
                String nameSoundboard = cursor.getSoundboardName();
                Soundboard soundboard = new Soundboard(uuidSoundboard, nameSoundboard);
                currentGame.addSoundboard(soundboard);
            }
        }

        return res.build();
    }

    void unlinkAllGames() {
        getDatabase().delete(SoundboardGameTable.NAME, null, new String[]{});

    }
}
