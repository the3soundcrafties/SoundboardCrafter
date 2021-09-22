package de.soundboardcrafter.dao;

import static com.google.common.base.Preconditions.checkNotNull;

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

import de.soundboardcrafter.dao.DBSchema.SoundboardFavoritesTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;
import de.soundboardcrafter.model.Soundboard;
import de.soundboardcrafter.model.SoundboardWithSounds;
import de.soundboardcrafter.model.audio.AudioSelectionChanges;
import de.soundboardcrafter.model.audio.BasicAudioModel;

/**
 * Database Access Object for accessing Soundboards in the database
 */
@WorkerThread
public class SoundboardDao extends AbstractDao {
    private SoundDao soundDao;
    private FavoritesDao favoritesDao;

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
        favoritesDao = FavoritesDao.getInstance(context);
    }

    public void clearDatabase() {
        unlinkAllSounds();
        favoritesDao.unlinkAllFavorites();
        favoritesDao.deleteAllFavorites();
        soundDao.deleteAllSounds();
        deleteAllSoundboards();
    }

    /**
     * Returns whether there are any soundboards. Might be slow.
     */
    public boolean areAny() {
        return !findAll().isEmpty();
    }

    public ImmutableList<SoundboardWithSounds> findAllWithSounds() {
        return findAllWithSounds(null);
    }

    public ImmutableList<SoundboardWithSounds> findAllWithSounds(@Nullable UUID favoritesId) {
        Object[] params;
        if (favoritesId != null) {
            params = new Object[]{favoritesId};
        } else {
            params = new Object[0];
        }

        Cursor rawCursor = rawQueryOrThrow(
                FullJoinSoundboardCursorWrapper.queryString(favoritesId, null), params);
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
                    FullJoinSoundboardCursorWrapper.IndexedSound indexedSound =
                            row.getIndexedSound();
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
                        res.add(new SoundboardWithSounds(lastSoundboard, lastSounds));
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
                res.add(new SoundboardWithSounds(lastSoundboard, lastSounds));
            }

            return res.build();
        }
    }

    @Nonnull
    private Sound putAdditionalSound(
            @NonNull Map<UUID, Sound> sounds, int lastIndex,
            Soundboard soundboard,
            @NonNull FullJoinSoundboardCursorWrapper.IndexedSound indexedSound) {
        checkNotNull(indexedSound, "indexedSound was null");

        UUID soundId = indexedSound.getSound().getId();
        Sound sound = sounds.computeIfAbsent(soundId, k -> indexedSound.getSound());

        if (indexedSound.getIndex() != lastIndex + 1) {
            throw new IllegalStateException("Gap in indexes of soundboard " +
                    soundboard.getId() + ". Expected next index " +
                    (lastIndex + 1) + ", but was " + indexedSound.getIndex());
        }
        return sound;
    }

    /**
     * Retrieves all soundboards, each with a mark, whether this sound is included.
     */
    ImmutableList<SelectableModel<Soundboard>> findAllSelectable(@NonNull Sound sound) {
        Cursor rawCursor = rawQueryOrThrow(SelectableSoundboardCursorWrapper.queryString(),
                SelectableSoundboardCursorWrapper.selectionArgs(sound.getId()));
        return findAllSelectable(rawCursor);
    }

    /**
     * Retrieves all soundboards, each with a mark, whether a certain sound is included.
     */
    private ImmutableList<SelectableModel<Soundboard>> findAllSelectable(Cursor rawCursor) {
        try (SelectableSoundboardCursorWrapper cursor =
                     new SelectableSoundboardCursorWrapper(rawCursor)) {
            final ImmutableList.Builder<SelectableModel<Soundboard>> res = ImmutableList.builder();

            while (cursor.moveToNext()) {
                res.add(cursor.getSelectableSoundboard());
            }

            return res.build();
        }
    }

    public void updateProvidedSoundboardWithAudios(String name, List<BasicAudioModel> audioModels) {
        @Nullable
        Soundboard soundboard = findProvidedByName(name);

        if (soundboard == null) {
            soundboard = new Soundboard(name, true);
            insert(soundboard);
        }

        link(soundboard, audioModels);
    }

    public void deleteProvidedSoundboardAndSounds(String name) {
        @Nullable
        Soundboard soundboard = findProvidedByName(name);
        if (soundboard == null) {
            return;
        }

        final SoundboardWithSounds soundboardWithSounds = findWithSounds(soundboard.getId());

        unlinkAllSounds(soundboard.getId());

        for (Sound sound : soundboardWithSounds.getSounds()) {
            soundDao.delete(sound.getId());
        }

        delete(soundboard.getId());
    }

    public void insertWithSounds(Soundboard soundboard, List<BasicAudioModel> audios) {
        insert(soundboard);

        int i = 0;
        for (BasicAudioModel audioModel : audios) {
            @Nullable
            Sound sound = soundDao.find(audioModel.getAudioLocation());
            if (sound == null) {
                sound = createSound(audioModel);
                soundDao.insert(sound);
            }

            linkSoundToSoundboard(soundboard.getId(), i, sound.getId());
            i++;
        }
    }

    /**
     * Updates the soundboard links for this sound. The soundboards must already exist.
     */
    void updateLinks(@NonNull SoundWithSelectableSoundboards sound) {
        for (SelectableModel<Soundboard> selectableSoundboard : sound.getSoundboards()) {
            updateLink(sound, selectableSoundboard);
        }
    }

    /**
     * Creates or deletes the soundboard link for this sound, if necessary.
     */
    private void updateLink(SoundWithSelectableSoundboards sound,
                            @NonNull SelectableModel<Soundboard> selectableSoundboard) {
        if (selectableSoundboard.isSelected()) {
            linkSound(selectableSoundboard.getModel(), sound.getSound());
            return;
        }

        unlinkSound(selectableSoundboard.getModel(), sound.getSound().getId());
    }

    /**
     * Inserts the soundboard and all its sounds - they must not be contained in the
     * database before - sound duplicates in the soundboard are not supported!
     * <p>
     * (This method is only useful for initialization purposes.)
     */
    public void insertSoundboardAndInsertAllSounds(
            @NonNull SoundboardWithSounds soundboardWithSounds) {
        soundDao.insert(soundboardWithSounds.getSounds());
        insert(soundboardWithSounds.getSoundboard());
        linkSoundsInOrder(soundboardWithSounds);
    }

    public void relinkSoundsInOrder(@NonNull SoundboardWithSounds soundboardWithSounds) {
        unlinkAllSounds(soundboardWithSounds.getId());
        linkSoundsInOrder(soundboardWithSounds);
    }

    private void linkSoundsInOrder(@NonNull SoundboardWithSounds soundboardWithSounds) {
        for (int i = 0; i < soundboardWithSounds.getSounds().size(); i++) {
            Sound sound = soundboardWithSounds.getSounds().get(i);
            linkSoundToSoundboard(soundboardWithSounds.getId(), i, sound.getId());
        }
    }

    public void updateWithChanges(Soundboard soundboard,
                                  AudioSelectionChanges audioSelectionChanges) {
        update(soundboard);

        unlink(soundboard, audioSelectionChanges.getImmutableRemovals());
        link(soundboard, audioSelectionChanges.getImmutableAdditions());
    }

    private void link(Soundboard soundboard, Iterable<BasicAudioModel> audios) {
        for (BasicAudioModel audioModel : audios) {
            @Nullable
            Sound sound = soundDao.find(audioModel.getAudioLocation());
            if (sound == null) {
                sound = createSound(audioModel);
                soundDao.insert(sound);
            }

            linkSound(soundboard, sound);
        }
    }

    @NonNull
    private Sound createSound(BasicAudioModel audioModel) {
        return new Sound(audioModel.getAudioLocation(), audioModel.getName());
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
    private int findMaxIndex(@NonNull Soundboard soundboard) {
        return findMaxIndex(soundboard.getId());
    }

    /**
     * Returns the maximum index in the soundboard - or <code>-1</code>, if the sound
     * does not contain any sounds.
     */
    private int findMaxIndex(UUID soundboardId) {
        try (final Cursor cursor = rawQueryOrThrow(
                "SELECT MAX(sbs." + SoundboardSoundTable.Cols.POS_INDEX + ") " +
                        "FROM " + SoundboardSoundTable.NAME + " sbs " +
                        "WHERE sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? " +
                        "GROUP BY sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID,
                soundboardId)) {
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

    public void moveSound(UUID soundboardId, int oldIndex, int newIndex) {
        @Nullable UUID soundId = findSoundId(soundboardId, oldIndex);
        if (soundId == null) {
            throw new IllegalStateException("There was no sound at index " + oldIndex + ".");
        }

        unlinkSound(soundboardId, oldIndex);

        linkSoundToSoundboard(soundboardId, newIndex, soundId);
    }

    /**
     * Returns the ID of the sound with this <code>index</code> in this soundboard - if any.
     */
    @Nullable
    private UUID findSoundId(UUID soundboardId, int index) {
        try (final Cursor cursor = rawQueryOrThrow(
                "SELECT " + SoundboardSoundTable.Cols.SOUND_ID + " " +
                        "FROM " + SoundboardSoundTable.NAME + " sbs " +
                        "WHERE sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? " +
                        "AND sbs." + SoundboardSoundTable.Cols.POS_INDEX + " = ?",
                soundboardId, index)) {
            if (cursor.moveToNext()) {
                return UUID.fromString(cursor.getString(0));
            }

            return null;
        }
    }

    /**
     * Adds this sound at this <code>index</code> in this
     * soundboard.
     *
     * @throws IllegalStateException if it does not succeed
     */
    private void linkSoundToSoundboard(UUID soundboardId, int index, @NonNull UUID soundId) {
        // TODO throw exception if the sound is already contained in the soundboard
        // (at any index)

        makeSoundGap(soundboardId, index);

        ContentValues values = new ContentValues();
        values.put(SoundboardSoundTable.Cols.SOUNDBOARD_ID, soundboardId.toString());
        values.put(SoundboardSoundTable.Cols.SOUND_ID, soundId.toString());
        values.put(SoundboardSoundTable.Cols.POS_INDEX, index);

        insertOrThrow(SoundboardSoundTable.NAME, values);
    }

    private void unlinkAllSounds() {
        getDatabase().delete(SoundboardSoundTable.NAME, null, new String[]{});
    }

    private void unlinkAllSounds(@NonNull UUID soundboardId) {
        getDatabase()
                .delete(SoundboardSoundTable.NAME, SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ?",
                        new String[]{soundboardId.toString()});
    }

    private void unlinkAllFavorites(@NonNull UUID soundboardId) {
        getDatabase()
                .delete(SoundboardFavoritesTable.NAME,
                        SoundboardFavoritesTable.Cols.SOUNDBOARD_ID + " = ?",
                        new String[]{soundboardId.toString()});
    }

    void unlinkSound(UUID soundId) {
        try (Cursor cursor = getDatabase().query(
                SoundboardSoundTable.NAME,
                new String[]{SoundboardSoundTable.Cols.SOUNDBOARD_ID},
                SoundboardSoundTable.Cols.SOUND_ID + " = ?",
                new String[]{soundId.toString()},
                null,
                null,
                null)) {
            while (cursor.moveToNext()) {
                unlinkSound(UUID.fromString(cursor.getString(0)), soundId);
            }
        }
    }

    private void unlink(Soundboard soundboard,
                        ImmutableList<AbstractAudioLocation> audioLocations) {
        for (AbstractAudioLocation audioLocation : audioLocations) {
            @Nullable
            Sound sound = soundDao.find(audioLocation);
            if (sound != null) {
                unlinkSound(soundboard, sound.getId());
            }
        }
    }

    private void unlinkSound(@NonNull Soundboard soundboard, UUID soundId) {
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
    private Cursor queryIndex(@NonNull UUID soundboardId, @NonNull UUID soundId) {
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

    public void unlinkSound(@NonNull UUID soundboardId, int index) {
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
     * Fills the gap at this index - 1 and lets the following sounds - if any - move up.
     */
    private void fillSoundGap(@NonNull UUID soundboardId, int gapIndex) {
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

    /**
     * Makes a gap at the index.
     */
    private void makeSoundGap(UUID soundboardId, int gapIndex) {
        int i = findMaxIndex(soundboardId);

        while (i >= gapIndex) {
            ContentValues values = new ContentValues();
            values.put(SoundboardSoundTable.Cols.POS_INDEX, i + 1);

            int rowsUpdated = getDatabase().update(SoundboardSoundTable.NAME,
                    values,
                    SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? and " +
                            SoundboardSoundTable.Cols.POS_INDEX + " = ? ",
                    new String[]{soundboardId.toString(),
                            Integer.toString(i)});

            if (rowsUpdated > 1) {
                throw new RuntimeException("More than one row at index " + i +
                        " in soundboard " + soundboardId);
            }

            i--;
        }
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

    public void delete(UUID soundboardId) {
        unlinkAllFavorites(soundboardId);
        unlinkAllSounds(soundboardId);
        getDatabase().delete(SoundboardTable.NAME, SoundboardTable.Cols.ID + " = ?",
                new String[]{soundboardId.toString()});
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

    public ImmutableList<Soundboard> findAllProvided() {
        try (SoundboardCursorWrapper cursorWrapper = queryAllProvided()) {
            ImmutableList.Builder<Soundboard> res = new ImmutableList.Builder<>();
            while (cursorWrapper.moveToNext()) {
                res.add(cursorWrapper.getSoundboard());
            }
            return res.build();
        }
    }

    @NonNull
    private SoundboardCursorWrapper queryAll() {
        return querySoundboards(null, null);
    }

    @NonNull
    private SoundboardCursorWrapper queryAllProvided() {
        return querySoundboards(SoundboardTable.Cols.PROVIDED + " = 1", null);
    }

    @Nullable
    private Soundboard findProvidedByName(String name) {
        try (SoundboardCursorWrapper cursor = querySoundboards(
                SoundboardTable.Cols.NAME + " = ? "
                        + "AND " + SoundboardTable.Cols.PROVIDED + " = 1",
                new String[]{name})) {
            if (!cursor.moveToNext()) {
                return null;
            }

            Soundboard res = cursor.getSoundboard();
            if (cursor.moveToNext()) {
                throw new IllegalStateException(
                        "More than one provided soundboard with name " + name);
            }

            return res;
        }
    }

    public Soundboard find(UUID soundboardId) {
        try (SoundboardCursorWrapper cursor = querySoundboards(
                DBSchema.SoundboardTable.Cols.ID + " = ?",
                new String[]{soundboardId.toString()})) {
            if (!cursor.moveToNext()) {
                throw new IllegalStateException("No soundboard with ID " + soundboardId);
            }

            Soundboard res = cursor.getSoundboard();
            if (cursor.moveToNext()) {
                throw new IllegalStateException("More than one soundboard with ID " + soundboardId);
            }

            return res;
        }
    }

    public SoundboardWithSounds findWithSounds(UUID soundboardId) {
        checkNotNull(soundboardId, "soundboardId");

        Object[] params = new Object[]{soundboardId};

        Cursor rawCursor = rawQueryOrThrow(
                FullJoinSoundboardCursorWrapper.queryString(null, soundboardId), params);
        final ImmutableList<SoundboardWithSounds> resList = find(rawCursor);
        if (resList.isEmpty()) {
            throw new IllegalStateException("No soundboard with ID " + soundboardId);
        }
        if (resList.size() > 1) {
            throw new IllegalStateException("More than one soundboard with ID " + soundboardId);
        }

        return resList.iterator().next();
    }

    @NonNull
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
        // TODO throw exception if soundboard name already exists
        insertOrThrow(DBSchema.SoundboardTable.NAME, buildContentValues(soundboard));
    }

    @NonNull
    private ContentValues buildContentValues(@NonNull Soundboard soundboard) {
        ContentValues values = new ContentValues();
        values.put(DBSchema.SoundboardTable.Cols.ID, soundboard.getId().toString());
        values.put(DBSchema.SoundboardTable.Cols.NAME, soundboard.getFullName());
        values.put(SoundboardTable.Cols.PROVIDED, soundboard.isProvided() ? 1 : 0);

        return values;
    }
}
