package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.UUID;

import javax.annotation.Nonnull;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.model.AssetFolderAudioLocation;
import de.soundboardcrafter.model.FileSystemFolderAudioLocation;
import de.soundboardcrafter.model.IAudioFileSelection;
import de.soundboardcrafter.model.IAudioLocation;
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
        super(context);
    }

    private void init(@Nonnull Context context) {
        soundboardDao = SoundboardDao.getInstance(context);
    }

    /**
     * Finds all sounds, mapped on their respective {@link IAudioLocation}.
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


    /**
     * Finds all sounds, each with a mark, whether the sound is part of this soundboard, and each
     * mapped on their respective {@link IAudioLocation}.
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
        Sound sound = find(soundId);
        ImmutableList<SelectableModel<Soundboard>> selectableSoundboards =
                soundboardDao.findAllSelectable(sound);

        return new SoundWithSelectableSoundboards(sound, selectableSoundboards);

    }

    /**
     * Finds a sound by ID.
     *
     * @throws IllegalStateException if no sound with this ID exists - or more than one
     */
    private Sound find(UUID soundId) {
        try (SoundCursorWrapper cursor = querySounds(SoundTable.Cols.ID + " = ?",
                new String[]{soundId.toString()})) {
            if (!cursor.moveToNext()) {
                throw new IllegalStateException("No sound with ID " + soundId);
            }

            Sound res = cursor.getSound();
            if (cursor.moveToNext()) {
                throw new IllegalStateException("More than one sound with ID " + soundId);
            }

            return res;
        }
    }

    private SoundCursorWrapper queryAll() {
        return querySounds(null, new String[]{});
    }

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
    public void insert(Collection<Sound> sounds) {
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
    public void updateSoundAndSoundboardLinks(SoundWithSelectableSoundboards sound) {
        update(sound.getSound());
        soundboardDao.updateLinks(sound);
    }

    /**
     * Updates this sound which has to exist in the database.
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

    private ContentValues buildContentValues(Sound sound) {
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

    private String toPath(IAudioLocation audioLocation) {
        if (audioLocation instanceof FileSystemFolderAudioLocation) {
            return ((FileSystemFolderAudioLocation) audioLocation).getInternalPath();
        }

        if (audioLocation instanceof AssetFolderAudioLocation) {
            return ((AssetFolderAudioLocation) audioLocation).getInternalPath();
        }

        throw new IllegalStateException("Unexpected audio location type " +
                audioLocation.getClass());
    }

    private SoundTable.LocationType toLocationType(IAudioLocation audioLocation) {
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
