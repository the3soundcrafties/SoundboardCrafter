package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.SelectableSoundboard;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;
import de.soundboardcrafter.model.Soundboard;
import de.soundboardcrafter.model.SoundboardWithSounds;

import static androidx.core.util.Preconditions.checkNotNull;

/**
 * Database Access Object for accessing Soundboards in the database
 */
@WorkerThread
public class SoundboardDao extends AbstractDao {
    private SoundDao soundDao;
    private GameDao gameDao;

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
        gameDao = GameDao.getInstance(context);
    }

    public void clearDatabase() {
        unlinkAllSounds();
        gameDao.unlinkAllGames();
        gameDao.deleteAllGames();
        soundDao.deleteAllSounds();
        deleteAllSoundboards();
    }

    public ImmutableList<SoundboardWithSounds> findAllWithSounds(@Nullable UUID gameId) {
        Object[] params;
        if (gameId != null) {
            params = new Object[]{gameId};
        } else {
            params = new Object[0];
        }

        Cursor rawCursor = rawQueryOrThrow(
                FullJoinSoundboardCursorWrapper.queryString(gameId), params);
        return find(rawCursor);
    }

    private ImmutableList<SoundboardWithSounds> find(Cursor rawCursor) {
        try (final FullJoinSoundboardCursorWrapper cursor =
                     new FullJoinSoundboardCursorWrapper(rawCursor)) {
            ImmutableList.Builder<SoundboardWithSounds> res = ImmutableList.builder();
            // The same Sound shall result in the same object
            Map<UUID, Sound> sounds = new HashMap<>();

            Soundboard lastSoundboard = null;
            ArrayList<Sound> lastSounds = Lists.newArrayList();
            int lastIndex = -1; // index of the sound on the soundboard

            while (cursor.moveToNext()) {
                final FullJoinSoundboardCursorWrapper.Row row = cursor.getRow();
                Soundboard soundboard = row.getSoundboard();

                if (lastSoundboard != null &&
                        soundboard.getId().equals(lastSoundboard.getId())) {
                    // Reuse existing sounds.
                    FullJoinSoundboardCursorWrapper.IndexedSound indexedSound = row.getIndexedSound();
                    if (indexedSound == null) {
                        throw new IllegalStateException(
                                "indexedSound was null for second soundboard row");
                    }

                    Sound sound = putAdditionalSound(sounds, lastIndex, soundboard, indexedSound);

                    lastSoundboard = soundboard;
                    lastSounds.add(sound);
                    lastIndex = indexedSound.getIndex();
                } else {
                    if (lastSoundboard != null) {
                        lastSounds.trimToSize();
                        res.add(new SoundboardWithSounds(lastSoundboard, Lists.newArrayList(lastSounds)));
                    }

                    lastSoundboard = soundboard;
                    lastSounds = Lists.newArrayList();

                    if (row.getIndexedSound() != null) {
                        if (row.getIndexedSound().getIndex() > 0) {
                            throw new IllegalStateException("Lowest sound index of soundboard " +
                                    soundboard.getId() + " invalid. Expected 0, but was "
                                    + row.getIndexedSound().getIndex());
                        }

                        lastSounds.add(row.getIndexedSound().getSound());
                        lastIndex = row.getIndexedSound().getIndex();
                    } else {
                        lastIndex = -1;
                    }
                }
            }

            if (lastSoundboard != null) {
                lastSounds.trimToSize();
                res.add(new SoundboardWithSounds(lastSoundboard, Lists.newArrayList(lastSounds)));
            }

            return res.build();
        }
    }

    private Sound putAdditionalSound(Map<UUID, Sound> sounds, int lastIndex,
                                     Soundboard soundboard,
                                     @NonNull FullJoinSoundboardCursorWrapper.IndexedSound indexedSound) {
        checkNotNull(indexedSound, "indexedSound was null");

        UUID soundId = indexedSound.getSound().getId();
        @Nullable Sound sound = sounds.get(soundId);
        if (sound == null) {
            sound = indexedSound.getSound();
            sounds.put(soundId, sound);
        }

        if (indexedSound.getIndex() != lastIndex + 1) {
            throw new IllegalStateException("Gap in indexes of soundboard " +
                    soundboard.getId() + ". Expected next index " +
                    (lastIndex + 1) + ", but was " + indexedSound.getIndex());
        }
        return sound;
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

        try (SelectableSoundboardCursorWrapper cursor = new SelectableSoundboardCursorWrapper(rawCursor)) {
            final ImmutableList.Builder<SelectableSoundboard> res = ImmutableList.builder();

            while (cursor.moveToNext()) {
                res.add(cursor.getSelectableSoundboard());
            }

            return res.build();
        }
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

        unlinkSound(selectableSoundboard.getSoundboard(), sound.getSound().getId());
    }

    public void linkSoundsInOrder(SoundboardWithSounds soundboardWithSounds) {
        Soundboard soundboard = soundboardWithSounds.getSoundboard();
        unlinkAllSounds(soundboard.getId());
        for (int i = 0; i < soundboardWithSounds.getSounds().size(); i++) {
            Sound sound = soundboardWithSounds.getSounds().get(i);
            linkSoundToSoundboard(soundboard.getId(), i, sound.getId());
        }
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
                soundboard.getId())) {
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
        try (final Cursor cursor = queryIndex(soundboard.getId(), sound.getId())) {
            return cursor.moveToNext();
        }
    }

    /**
     * Adds this sound at this <code>index</code> in this
     * soundboard.
     *
     * @throws IllegalStateException if it does not succeed
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

    private void unlinkAllSounds(UUID soundboardId) {
        getDatabase().delete(SoundboardSoundTable.NAME, SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ?", new String[]{soundboardId.toString()});
    }

    void unlinkSound(UUID soundId) {
        try (Cursor cursor = getDatabase().query(
                SoundboardSoundTable.NAME,
                new String[]{SoundboardSoundTable.Cols.SOUNDBOARD_ID},
                null,
                null,
                null,
                null,
                null)) {
            while (cursor.moveToNext()) {
                unlinkSound(UUID.fromString(cursor.getString(0)), soundId);
            }
        }

    }

    private void unlinkSound(Soundboard soundboard, UUID soundId) {
        unlinkSound(soundboard.getId(), soundId);
    }

    private void unlinkSound(UUID soundboardId, UUID soundId) {
        boolean lookAgain = true;
        while (lookAgain) {
            try (final Cursor cursor =
                         queryIndex(soundboardId, soundId)) {
                if (cursor.moveToNext()) {
                    int index = cursor.getInt(0);
                    unlinkSound(soundboardId, index);
                } else {
                    lookAgain = false;
                }
            }
        }
    }

    /**
     * Returns a query cursor for the column index that this sound has in this soundboard
     */
    private Cursor queryIndex(UUID soundboardId, UUID soundId) {
        return getDatabase().query(
                SoundboardSoundTable.NAME,
                new String[]{SoundboardSoundTable.Cols.POS_INDEX},
                SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? AND " +
                        SoundboardSoundTable.Cols.SOUND_ID + " = ?",
                new String[]{soundboardId.toString(), soundId.toString()},
                null,
                null,
                null);
    }


    public void unlinkSound(UUID soundboardId, int index) {
        int numDeleted = getDatabase().delete(SoundboardSoundTable.NAME,
                SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? and " +
                        SoundboardSoundTable.Cols.POS_INDEX + " = ? ",
                new String[]{soundboardId.toString(),
                        Integer.toString(index)});

        if (numDeleted == 0) {
            throw new RuntimeException("There was no sound at index " + index + ".");
        }

        if (numDeleted > 1) {
            throw new RuntimeException("There was more than one sound at index " + index + ".");
        }

        fillSoundGap(soundboardId, index);
    }

    /**
     * Fills the gap at index - 1 and lets the following sounds - if any - move up.
     */
    private void fillSoundGap(UUID soundboardId, int gapIndex) {
        int i = gapIndex + 1;

        int rowsUpdated;
        do {
            ContentValues values = new ContentValues();
            values.put(SoundboardSoundTable.Cols.POS_INDEX, i - 1);

            rowsUpdated = getDatabase().update(SoundboardSoundTable.NAME,
                    values,
                    SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? and " +
                            SoundboardSoundTable.Cols.POS_INDEX + " = ? ",
                    new String[]{soundboardId.toString(),
                            Integer.toString(i)});

            if (rowsUpdated > 1) {
                throw new RuntimeException("More than one row at index " + (i - 1) +
                        " in soundboard " + soundboardId);
            }

            i++;
        } while (rowsUpdated > 0);
    }

    public void update(Soundboard soundboard) {
        int rowsUpdated = getDatabase().update(DBSchema.SoundboardTable.NAME,
                buildContentValues(soundboard),
                DBSchema.SoundboardTable.Cols.ID + " = ?",
                new String[]{soundboard.getId().toString()});

        if (rowsUpdated != 1) {
            throw new RuntimeException("Not exactly one sound with ID + " + soundboard.getId());
        }
    }

    private void deleteAllSoundboards() {
        getDatabase().delete(SoundboardTable.NAME, null, new String[]{});
    }

    public void remove(UUID soundboardId) {
        unlinkAllSounds(soundboardId);
        getDatabase().delete(SoundboardTable.NAME, SoundboardTable.Cols.ID + " = ?", new String[]{soundboardId.toString()});
    }

    public List<Soundboard> findAll() {
        try (SoundboardCursorWrapper cursorWrapper = queryAll()) {
            ImmutableList.Builder<Soundboard> res = new ImmutableList.Builder<>();
            while (cursorWrapper.moveToNext()) {
                res.add(cursorWrapper.getSoundboard());
            }
            return res.build();
        }
    }

    private SoundboardCursorWrapper queryAll() {
        return querySoundboards(null, null);
    }

    public Soundboard find(UUID soundboardId) {
        try (SoundboardCursorWrapper cursor = querySoundboards(DBSchema.SoundboardTable.Cols.ID + " = ?",
                new String[]{soundboardId.toString()})) {
            if (!cursor.moveToNext()) {
                throw new IllegalStateException("No sound with ID " + soundboardId);
            }

            Soundboard res = cursor.getSoundboard();
            if (cursor.moveToNext()) {
                throw new IllegalStateException("More than one sound with ID " + soundboardId);
            }

            return res;
        }
    }

    private SoundboardCursorWrapper querySoundboards(String whereClause, String[] whereArgs) {
        final Cursor cursor =
                getDatabase().query(
                        DBSchema.SoundboardTable.NAME,
                        null, // all columns
                        whereClause, whereArgs,
                        null,
                        null,
                        null
                );

        return new SoundboardCursorWrapper(cursor);
    }

    public void insert(Soundboard soundboard) {
        // TODO throw exception if sound name already exists
        insertOrThrow(DBSchema.SoundboardTable.NAME, buildContentValues(soundboard));
    }

    private ContentValues buildContentValues(Soundboard soundboard) {
        ContentValues values = new ContentValues();
        values.put(DBSchema.SoundboardTable.Cols.ID, soundboard.getId().toString());
        values.put(DBSchema.SoundboardTable.Cols.NAME, soundboard.getName());

        return values;
    }


}
