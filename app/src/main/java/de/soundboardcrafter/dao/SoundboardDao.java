package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import de.soundboardcrafter.dao.DBSchema.GameTable;
import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardGameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.SelectableSoundboard;
import de.soundboardcrafter.model.Game;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;
import de.soundboardcrafter.model.Soundboard;
import de.soundboardcrafter.model.SoundboardWithSounds;

/**
 * Database Access Object for accessing Soundboards in the database
 */
@WorkerThread
public class SoundboardDao extends AbstractDao {
    private SoundDao soundDao;

    private static SoundboardDao instance;

    public static SoundboardDao getInstance(final Context context) {
        if (instance == null) {
            instance = new SoundboardDao(context);
            instance.init(context);
        }

        return instance;
    }

    private SoundboardDao(@Nonnull Context context) {
        super(context);
    }

    private void init(@Nonnull Context context) {
        soundDao = SoundDao.getInstance(context);
    }

    public void clearDatabase() {
        unlinkAllSounds();
        unlinkAllGames();
        deleteAllGames();
        soundDao.deleteAllSounds();

        // TODO unlink and delete all games

        deleteAllSoundboards();
    }

    public void insertDummyData() {
        File directory = new File("/storage/emulated/0/soundboard crafter test songs");
        // get all the files from a directory
        File[] fList = directory.listFiles();

        List<File> notInSubdirectory = new ArrayList<>();
        for (File firstLevelFile : fList) {
            if (firstLevelFile.isDirectory()) {
                ArrayList<Sound> soundList = new ArrayList<>();
                File[] soundFiles = firstLevelFile.listFiles();
                for (File soundFile : soundFiles) {
                    Sound newSound = createSound(soundFile);
                    soundList.add(newSound);
                }
                SoundboardWithSounds soundboard = new SoundboardWithSounds(firstLevelFile.getName(), soundList, Lists.newArrayList(new Game("Spiel Cool")));
                insert(soundboard);
            } else {
                notInSubdirectory.add(firstLevelFile);
            }

        }
        if (!notInSubdirectory.isEmpty()) {
            ArrayList<Sound> sounds = new ArrayList<>();
            File automaticCreatedDir = new File(directory.getAbsolutePath() + "/automatic_created_dir");
            automaticCreatedDir.mkdir();
            // TODO What if mkdir did not succeed?
            for (File notInSubdirectoryFile : notInSubdirectory) {
                File to = new File(automaticCreatedDir.getAbsolutePath() + "/" + notInSubdirectoryFile.getName());
                notInSubdirectoryFile.renameTo(to);
                // TODO What if renameTo did not succeed?
                sounds.add(createSound(to));
            }
            SoundboardWithSounds soundboard = new SoundboardWithSounds(automaticCreatedDir.getName(), sounds, new ArrayList<>());
            insert(soundboard);
        }

    private static Sound createSound(File soundFile) {
        return SoundFromFileCreationUtil.createSound(
                soundFile.getName(), soundFile.getAbsolutePath());
    }

    private Collection<UUID> findSoundboardLinksBySound(Sound sound) {
        ImmutableList.Builder<UUID> res = ImmutableList.builder();

        try (final Cursor cursor =
                     getDatabase().query(
                             DBSchema.SoundboardSoundTable.NAME,
                             new String[]{SoundboardSoundTable.Cols.SOUNDBOARD_ID},
                             SoundboardSoundTable.Cols.SOUND_ID + " = ?",
                             new String[]{sound.getId().toString()},
                             null,
                             null,
                             null)) {
            while (cursor.moveToNext()) {
                UUID soundboardId =
                        UUID.fromString(cursor.getString(0));

                res.add(soundboardId);
            }
        }

        return res.build();
    }

    public ImmutableList<SoundboardWithSounds> findAllWithSounds() {
        Cursor rawCursor = rawQueryOrThrow(FullJoinSoundboardCursorWrapper.queryString());
        return find(rawCursor);
    }


    private ImmutableList<SoundboardWithSounds> find(Cursor rawCursor) {
        try (final FullJoinSoundboardCursorWrapper cursor =
                     new FullJoinSoundboardCursorWrapper(rawCursor)) {
            ImmutableList.Builder<SoundboardWithSounds> res = ImmutableList.builder();
            // The same Sound shall result in the same object
            Map<UUID, Sound> sounds = new HashMap<>();
            Map<UUID, Game> games = new HashMap<>();

            UUID lastSoundboardId = null;
            String lastSoundboardName = null;
            ArrayList<Sound> lastSounds = Lists.newArrayList();
            ArrayList<Game> lastGames = Lists.newArrayList();
            int lastIndex = -1; // index of the sound on the soundboard

            while (cursor.moveToNext()) {
                final UUID soundboardId = cursor.getSoundboardId();
                final String soundboardName = cursor.getSoundboardName();
                //sound
                final int index;
                final UUID soundId;
                final String path;
                final String name;
                final int volumePercentage;
                final boolean loop;
                //game
                final UUID gameId;
                final String gameName;

                if (cursor.hasSound()) {
                    index = cursor.getIndex();
                    soundId = cursor.getSoundId();
                    path = cursor.getSoundPath();
                    name = cursor.getSoundName();
                    volumePercentage = cursor.getSoundVolumePercentage();
                    loop = cursor.isSoundLoop();
                } else {
                    index = -1;
                    soundId = null;
                    path = null;
                    name = null;
                    volumePercentage = -1;
                    loop = false;
                }
                if (cursor.hasGame()) {
                    gameId = cursor.getGameId();
                    gameName = cursor.getGameName();
                } else {
                    gameId = null;
                    gameName = "";
                }

                if (soundboardId.equals(lastSoundboardId)) {
                    // Reuse existing sounds.
                    @Nullable Sound sound = sounds.get(soundId);
                    if (sound == null) {
                        sound = new Sound(soundId, path, name, volumePercentage, loop);
                        sounds.put(soundId, sound);
                    }
                    @Nullable Game game = null;
                    if (gameId != null) {
                        // Reuse existing lastGames.
                        game = games.get(gameId);
                        if (game == null) {
                            game = new Game(gameId, gameName, new ArrayList<>());
                            games.put(gameId, game);
                        }
                    }

                    if (index != lastIndex + 1) {
                        throw new IllegalStateException("Gap in indexes of soundboard " + soundboardId + ". Expected next index " +
                                (lastIndex + 1) + ", but was " + index);
                    }

                    lastSoundboardId = soundboardId;
                    lastSoundboardName = soundboardName;
                    lastSounds.add(sound);
                    if (game != null) {
                        lastGames.add(game);
                    }
                    lastIndex = index;
                } else {
                    if (lastSoundboardId != null) {
                        lastSounds.trimToSize();
                        res.add(new SoundboardWithSounds(lastSoundboardId, lastSoundboardName, Lists.newArrayList(lastSounds), Lists.newArrayList(lastGames)));
                    }

                    lastSoundboardId = soundboardId;
                    lastSoundboardName = soundboardName;
                    lastSounds = Lists.newArrayList();
                    lastGames = Lists.newArrayList();

                    if (index != -1) {
                        if (index > 0) {
                            throw new IllegalStateException("Lowest sound index of soundboard " + soundboardId + " invalid. Expected 0, but was " + index);
                        }

                        lastSounds.add(new Sound(soundId, path, name, volumePercentage, loop));
                    }

                    lastIndex = index;
                }
            }

            if (lastSoundboardId != null) {
                lastSounds.trimToSize();
                lastGames.trimToSize();
                res.add(new SoundboardWithSounds(lastSoundboardId, lastSoundboardName, Lists.newArrayList(lastSounds), Lists.newArrayList(lastGames)));
            }

            return res.build();
        }
    }

    /**
     * Retrieves all soundboard, each with a mark, whether this sound is included.
     */
    ImmutableList<SelectableSoundboard> findAllSelectable(Sound sound) {
        Cursor rawCursor = rawQueryOrThrow(SelectableSoundboardCursorWrapper.queryString(),
                SelectableSoundboardCursorWrapper.selectionArgs(sound.getId()));
        return findAllSelectable(rawCursor);
    }

    /**
     * Retrieves all soundboards, each with a mark, whether this sound is included.
     */
    private ImmutableList<SelectableSoundboard> findAllSelectable(Cursor rawCursor) {
        final SelectableSoundboardCursorWrapper cursor =
                new SelectableSoundboardCursorWrapper(rawCursor);

        try {
            final ImmutableList.Builder<SelectableSoundboard> res = ImmutableList.builder();

            while (cursor.moveToNext()) {
                res.add(cursor.getSelectableSoundboard());
            }

            return res.build();
        } finally {
            cursor.close();
        }
    }


    /**
     * Inserts this <code>soundboard</code> an all its sounds into the database; <i>each of the
     * sounds is newly inserted, existing sounds cannot be reused in this method</i>.
     *
     * @throws RuntimeException if inserting does not succeed
     */
    private void insert(SoundboardWithSounds soundboard) {
        insertSoundboard(soundboard.getId(), soundboard.getName());

        int index = 0;
        for (Sound sound : soundboard.getSounds()) {
            soundDao.insertSound(sound);
            linkSoundToSoundboard(soundboard.getId(), index, sound.getId());
            index++;
        }

        for (Game game : soundboard.getGames()) {
            insertGame(game);
            linkGameToSoundboard(soundboard.getId(), game.getId());
        }
    }

    private void insertGame(Game game) {
        // TODO throw exception if sound name already exists
        insertOrThrow(GameTable.NAME, buildContentValues(game));
    }

    private void linkGameToSoundboard(UUID soundboardId, UUID gameId) {
        // TODO throw exception if the game is already contained in the soundboard
        // (at any index)
        ContentValues values = new ContentValues();
        values.put(SoundboardGameTable.Cols.SOUNDBOARD_ID, soundboardId.toString());
        values.put(SoundboardGameTable.Cols.GAME_ID, gameId.toString());

        insertOrThrow(SoundboardGameTable.NAME, values);
    }

    /**
     * Inserts an empty soundboard with this name.
     *
     * @throws RuntimeException if inserting does not succeed
     */
    private void insertSoundboard(UUID id, String name) {
        // TODO throw exception if soundboard name already exists

        ContentValues values = new ContentValues();
        values.put(SoundboardTable.Cols.ID, id.toString());
        values.put(SoundboardTable.Cols.NAME, name);

        insertOrThrow(SoundboardTable.NAME, values);
    }


    /**
     * Updates the soundboard links for this sound. The soundboards must already exist.
     */
    void updateLinks(SoundWithSelectableSoundboards sound) {
        for (SelectableSoundboard selectableSoundboard : sound.getSoundboards()) {
            updateLink(sound, selectableSoundboard);
        }
    }

    /**
     * Creates or deletes the soundboard link for this sound, if necessary.
     */
    private void updateLink(SoundWithSelectableSoundboards sound,
                            SelectableSoundboard selectableSoundboard) {
        if (selectableSoundboard.isSelected()) {
            linkSound(selectableSoundboard.getSoundboard(), sound.getSound());
            return;
        }

        unlinkSound(selectableSoundboard.getSoundboard(), sound.getSound());
    }

    private void linkSound(Soundboard soundboard, Sound sound) {
        if (isLinked(soundboard, sound)) {
            return;
        }

        int index = findMaxIndex(soundboard) + 1;

        linkSoundToSoundboard(soundboard.getId(), index, sound.getId());
    }

    /**
     * Returns the maximum index in the soundboard - or <code>-1</code>, if the sound
     * does not contain any sounds.
     */
    private int findMaxIndex(Soundboard soundboard) {
        try (final Cursor cursor = rawQueryOrThrow(
                "SELECT MAX(sbs." + SoundboardSoundTable.Cols.POS_INDEX + ") " +
                        "FROM " + SoundboardSoundTable.NAME + " sbs " +
                        "WHERE sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? " +
                        "GROUP BY sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID,
                new String[]{soundboard.getId().toString()})) {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }

            return -1;
        }
    }

    /**
     * Returns <code>true</code> if this sound is already linked to this soundboard,
     * otherwise <code>false</code>.
     */
    private boolean isLinked(Soundboard soundboard, Sound sound) {
        try (final Cursor cursor = queryIndex(soundboard, sound)) {
            if (cursor.moveToNext()) {
                return true;
            }

            return false;
        }
    }

    /**
     * Adds this sound at this <code>index</code> in this
     * soundboard.
     *
     * @throws RuntimeException if it does not succeed
     */
    private void linkSoundToSoundboard(UUID soundboardId, int index, UUID soundId) {
        // TODO throw exception if the sound is already contained in the soundboard
        // (at any index)

        ContentValues values = new ContentValues();
        values.put(SoundboardSoundTable.Cols.SOUNDBOARD_ID, soundboardId.toString());
        values.put(SoundboardSoundTable.Cols.SOUND_ID, soundId.toString());
        values.put(SoundboardSoundTable.Cols.POS_INDEX, index);

        insertOrThrow(SoundboardSoundTable.NAME, values);
    }

    private void unlinkAllSounds() {
        getDatabase().delete(SoundboardSoundTable.NAME, null, new String[]{});
    }

    private void unlinkSound(Soundboard soundboard, Sound sound) {
        boolean lookAgain = true;
        while (lookAgain) {
            try (final Cursor cursor =
                         queryIndex(soundboard, sound)) {
                if (cursor.moveToNext()) {
                    int index = cursor.getInt(0);
                    unlinkSound(soundboard, index);
                } else {
                    lookAgain = false;
                }
            }
        }
    }

    /**
     * Returns a query cursor for the column index that this sound has in this soundboard
     */
    private Cursor queryIndex(Soundboard soundboard, Sound sound) {
        return getDatabase().query(
                SoundboardSoundTable.NAME,
                new String[]{SoundboardSoundTable.Cols.POS_INDEX},
                SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? AND " +
                        SoundboardSoundTable.Cols.SOUND_ID + " = ?",
                new String[]{soundboard.getId().toString(), sound.getId().toString()},
                null,
                null,
                null);
    }

    private void unlinkAllGames() {
        database.delete(SoundboardGameTable.NAME, null, new String[]{});
    }

    public void unlinkSound(Soundboard soundboard, int index) {
        int numDeleted = getDatabase().delete(SoundboardSoundTable.NAME,
                SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? and " +
                        SoundboardSoundTable.Cols.POS_INDEX + " = ? ",
                new String[]{soundboard.getId().toString(),
                        Integer.toString(index)});

        if (numDeleted == 0) {
            throw new RuntimeException("There was no sound at index " + index + ".");
        }

        if (numDeleted > 1) {
            throw new RuntimeException("There was more than one sound at index " + index + ".");
        }

        fillSoundGap(soundboard, index);
    }

    /**
     * Fills the gap at index - 1 and lets the following sounds - if any - move up.
     */
    private void fillSoundGap(Soundboard soundboard, int gapIndex) {
        int i = gapIndex + 1;

        int rowsUpdated;
        do {
            ContentValues values = new ContentValues();
            values.put(SoundboardSoundTable.Cols.POS_INDEX, i - 1);

            rowsUpdated = getDatabase().update(SoundboardSoundTable.NAME,
                    values,
                    SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? and " +
                            SoundboardSoundTable.Cols.POS_INDEX + " = ? ",
                    new String[]{soundboard.getId().toString(),
                            Integer.toString(i)});

            if (rowsUpdated > 1) {
                throw new RuntimeException("More than one row at index " + (i - 1) +
                        " in soundboard " + soundboard.getName());
            }

            i++;
        } while (rowsUpdated > 0);
    }

    private void deleteAllSoundboards() {
        getDatabase().delete(SoundboardTable.NAME, null, new String[]{});
    }

        private ContentValues buildContentValues(Game game) {
            ContentValues values = new ContentValues();
            values.put(SoundTable.Cols.ID, game.getId().toString());
            values.put(SoundTable.Cols.NAME, game.getName());
            return values;
        }

        private void deleteAllGames() {
            database.delete(GameTable.NAME, null, new String[]{});
        }

        /**
         * Inserts these values as a new entry into this table.
         *
         * @throws RuntimeException if inserting does not succeed
         */
        private void insertOrThrow(final String table, final ContentValues values) {
            final long rowId = database.insertOrThrow(table, null, values);
            if (rowId == -1) {
                throw new RuntimeException("Could not insert into database: " + values);
            }
        }

    public ImmutableList<Game> findAllGames() {
        final GameCursorWrapper cursor =
                new GameCursorWrapper(
                        rawQueryOrThrow(GameCursorWrapper.queryString()));
        ImmutableList.Builder<Game> res = ImmutableList.builder();
        Game currentGame = null;
        while (cursor.moveToNext()) {
            UUID uuid = cursor.getGameId();
            String name = cursor.getGameName();
            if (currentGame == null || currentGame.getId() != uuid) {
                currentGame = new Game(uuid, name);
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

    private GameCursorWrapper queryGame(String whereClause, String[] whereArgs) {
        final Cursor cursor =
                database.query(
                        SoundTable.NAME,
                        null, // all columns
                        whereClause, whereArgs,
                        null,
                        null,
                        null
                );

        return new GameCursorWrapper(cursor);
    }
}
