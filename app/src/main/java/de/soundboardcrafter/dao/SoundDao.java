package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.IAudioFileSelection;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;
import de.soundboardcrafter.model.Soundboard;

/**
 * Database Access Object for accessing sounds in the database
 */
@WorkerThread
public class SoundDao extends AbstractDao {
    private static SoundDao instance;
    private static SoundboardDao soundboardDao;

    public static SoundDao getInstance(final Context context) {
        if (instance == null) {
            instance = new SoundDao(context);
            instance.init(context);
        }

        return instance;
    }

    private SoundDao(@Nonnull Context context) {
        super(context.getApplicationContext());
    }

    private void init(@Nonnull Context context) {
        soundboardDao = SoundboardDao.getInstance(context);
    }

    /**
     * Finds all sounds, mapped on their respective {@link AbstractAudioLocation}.
     */
    public ImmutableMap<IAudioFileSelection, Sound> findAllByAudioLocation() {
        ImmutableMap.Builder<IAudioFileSelection, Sound> res = ImmutableMap.builder();

        try (SoundCursorWrapper cursor = queryAll()) {
            while (cursor.moveToNext()) {
                Sound sound = cursor.getSound();
                res.put(sound.getAudioLocation(), sound);
            }
        }

        return res.build();
    }

    public ImmutableList<Sound> findAllProvided() {
        ImmutableList.Builder<Sound> res = ImmutableList.builder();

        try (SoundCursorWrapper cursor = queryAllProvided()) {
            while (cursor.moveToNext()) {
                Sound sound = cursor.getSound();
                res.add(sound);
            }
        }

        return res.build();
    }

    /**
     * Finds all sounds, each with a mark, whether the sound is part of this soundboard, and each
     * mapped on their respective {@link AbstractAudioLocation}.
     */
    public ImmutableMap<IAudioFileSelection, SelectableModel<Sound>>
    findAllSelectableByAudioLocation(UUID soundboardId) {
        ImmutableMap.Builder<IAudioFileSelection, SelectableModel<Sound>> res =
                ImmutableMap.builder();

        try (SelectableSoundCursorWrapper cursor = queryAllSelectable(soundboardId)) {
            while (cursor.moveToNext()) {
                SelectableModel<Sound> selectableSound = cursor.getSelectableSound();
                res.put(selectableSound.getModel().getAudioLocation(), selectableSound);
            }
        }

        return res.build();
    }

    /**
     * Queries all sounds, each with a mark, whether the sound is part of this soundboard.
     */
    @NonNull
    private SelectableSoundCursorWrapper queryAllSelectable(UUID soundboardId) {
        Cursor rawCursor = rawQueryOrThrow(SelectableSoundCursorWrapper.queryString(),
                SelectableSoundCursorWrapper.selectionArgs(soundboardId));
        return new SelectableSoundCursorWrapper(rawCursor);
    }

    /**
     * Finds a sound by ID, includes all soundboards and a mark, which of them are
     * selected.
     *
     * @throws IllegalStateException if no sound with this ID exists - or more than one
     */
    public SoundWithSelectableSoundboards findSoundWithSelectableSoundboards(UUID soundId) {
        @Nullable
        Sound sound = find(soundId);

        if (sound == null) {
            throw new IllegalStateException("No sound with ID " + soundId);
        }

        ArrayList<SelectableModel<Soundboard>> selectableSoundboards =
                new ArrayList<>(soundboardDao.findAllSelectable(sound));

        selectableSoundboards.sort(SelectableModel.byModel(
                Soundboard.PROVIDED_LAST_THEN_BY_COLLATION_KEY));

        return new SoundWithSelectableSoundboards(sound, selectableSoundboards);

    }

    /**
     * Finds a sound by audio location.
     */
    @Nullable
    public Sound find(AbstractAudioLocation audioLocation) {
        try (SoundCursorWrapper cursor = querySounds(SoundTable.Cols.PATH + " = ?",
                new String[]{audioLocation.getInternalPath()})) {
            if (!cursor.moveToNext()) {
                return null;
            }

            Sound res = cursor.getSound();
            if (cursor.moveToNext()) {
                throw new IllegalStateException(
                        "More than one sound for audio location " + audioLocation);
            }

            return res;
        }
    }

    /**
     * Finds a sound by ID.
     *
     * @throws IllegalStateException if more than one sound with this ID exists
     */
    @Nullable
    public Sound find(UUID soundId) {
        try (SoundCursorWrapper cursor = querySounds(SoundTable.Cols.ID + " = ?",
                new String[]{soundId.toString()})) {
            if (!cursor.moveToNext()) {
                return null;
            }

            Sound res = cursor.getSound();
            if (cursor.moveToNext()) {
                throw new IllegalStateException("More than one sound with ID " + soundId);
            }

            return res;
        }
    }

    @NonNull
    private SoundCursorWrapper queryAll() {
        return querySounds(null, new String[]{});
    }

    @NonNull
    private SoundCursorWrapper queryAllProvided() {
        return querySounds(
                SoundTable.Cols.LOCATION_TYPE + " = '" + SoundTable.LocationType.ASSET
                        + "'");
    }

    @NonNull
    private SoundCursorWrapper querySounds(String whereClause) {
        return querySounds(whereClause, null);
    }

    @NonNull
    private SoundCursorWrapper querySounds(String whereClause, String[] whereArgs) {
        final Cursor cursor =
                getDatabase().query(
                        SoundTable.NAME,
                        null, // all columns
                        whereClause, whereArgs,
                        null,
                        null,
                        null
                );

        return new SoundCursorWrapper(cursor);
    }

    /**
     * Inserts these sounds - they must not be contained in the
     * database before - duplicates in the collection are not supported!
     * <p>
     * (This method is only useful for initialization purposes.)
     */
    public void insert(@NonNull Collection<Sound> sounds) {
        for (Sound sound : sounds) {
            insert(sound);
        }
    }

    /**
     * Inserts the <code>sound</code>.
     *
     * @throws IllegalStateException if inserting does not succeed
     */
    public void insert(Sound sound) {
        // TODO throw exception if sound name already exists

        insertOrThrow(SoundTable.NAME, buildContentValues(sound));
    }

    /**
     * Updates this sound (which must already exist in the database) and updates the
     * soundboard links.
     */
    public void updateSoundAndSoundboardLinks(@NonNull SoundWithSelectableSoundboards sound) {
        update(sound.getSound());
        soundboardDao.updateLinks(sound);
    }

    /**
     * Updates this sound - which has to exist in the database.
     */
    public void update(Sound sound) {
        int rowsUpdated = getDatabase().update(SoundTable.NAME,
                buildContentValues(sound),
                SoundTable.Cols.ID + " = ?",
                new String[]{sound.getId().toString()});

        if (rowsUpdated != 1) {
            throw new RuntimeException("Not exactly one sound with ID + " + sound.getId());
        }
    }

    @NonNull
    private ContentValues buildContentValues(@NonNull Sound sound) {
        ContentValues values = new ContentValues();
        values.put(SoundTable.Cols.ID, sound.getId().toString());
        values.put(SoundTable.Cols.NAME, sound.getName());
        // https://stackoverflow.com/questions/5861460/why-does-contentvalues-have-a-put-method
        // -that-supports-boolean
        values.put(SoundTable.Cols.LOOP, sound.isLoop() ? 1 : 0);
        values.put(SoundTable.Cols.LOCATION_TYPE,
                toLocationType(sound.getAudioLocation()).name());
        values.put(SoundTable.Cols.PATH, toPath(sound.getAudioLocation()));
        values.put(SoundTable.Cols.VOLUME_PERCENTAGE, sound.getVolumePercentage());
        return values;
    }

    @NonNull
    @Contract("null -> fail")
    private String toPath(AbstractAudioLocation audioLocation) {
        if (audioLocation instanceof FileSystemFolderAudioLocation) {
            return ((FileSystemFolderAudioLocation) audioLocation).getInternalPath();
        }

        if (audioLocation instanceof AssetFolderAudioLocation) {
            return ((AssetFolderAudioLocation) audioLocation).getInternalPath();
        }

        throw new IllegalStateException("Unexpected audio location type " +
                audioLocation.getClass());
    }

    private SoundTable.LocationType toLocationType(AbstractAudioLocation audioLocation) {
        if (audioLocation instanceof FileSystemFolderAudioLocation) {
            return SoundTable.LocationType.FILE;
        }

        if (audioLocation instanceof AssetFolderAudioLocation) {
            return SoundTable.LocationType.ASSET;
        }

        throw new IllegalStateException("Unexpected audio location type " +
                audioLocation.getClass());
    }


    /**
     * Deletes this sound and all its soundboard links.
     */
    public void delete(UUID soundId) {
        soundboardDao.unlinkSound(soundId);

        getDatabase().delete(SoundTable.NAME,
                SoundTable.Cols.ID + " = ?",
                new String[]{soundId.toString()});
    }


    public void deleteAllSounds() {
        getDatabase().delete(SoundTable.NAME, null, new String[]{});
    }
}
