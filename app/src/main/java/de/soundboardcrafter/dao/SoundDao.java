package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.WorkerThread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import de.soundboardcrafter.model.SelectableSoundboard;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.SoundWithSelectableSoundboards;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

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
     * Finds all sounds, mapped on their respective path.
     */
    public ImmutableMap<String, Sound> findAllByPath() {
        ImmutableMap.Builder<String, Sound> res = ImmutableMap.builder();

        try (SoundCursorWrapper cursor = querySounds(null, new String[]{})) {
            while (cursor.moveToNext()) {
                Sound sound = cursor.getSound();
                res.put(sound.getPath(), sound);
            }
        }

        return res.build();
    }


    /**
     * Finds a sound by ID, includes all soundboards and a mark, which of them are
     * selected.
     *
     * @throws IllegalStateException if no sound with this ID exists - or more than one
     */
    public SoundWithSelectableSoundboards findSoundWithSelectableSoundboards(UUID soundId) {
        Sound sound = find(soundId);
        ImmutableList<SelectableSoundboard> selectableSoundboards =
                soundboardDao.findAllSelectable(sound);

        return new SoundWithSelectableSoundboards(sound, selectableSoundboards);

    }

    /**
     * Finds these sounds by their IDs.
     *
     * @throws IllegalStateException if for some id, no sound exists (or more than one)
     */
    public ImmutableList<Sound> findSounds(UUID... soundIds) {
        return Stream.of(soundIds)
                .map(this::find)
                .collect(collectingAndThen(toList(), ImmutableList::copyOf));
        // TODO Use .collect(ImmutableList::toImmutableList) - why doesn't that work?
    }

    /**
     * Finds a sound by ID.
     *
     * @throws IllegalStateException if no sound with this ID exists - or more than one
     */
    private Sound find(UUID soundId) {
        try (SoundCursorWrapper cursor = querySounds(DBSchema.SoundTable.Cols.ID + " = ?",
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

    private SoundCursorWrapper querySounds(String whereClause, String[] whereArgs) {
        final Cursor cursor =
                getDatabase().query(
                        DBSchema.SoundTable.NAME,
                        null, // all columns
                        whereClause, whereArgs,
                        null,
                        null,
                        null
                );

        return new SoundCursorWrapper(cursor);
    }

    /**
     * Inserts the <code>sound</code>.
     *
     * @throws RuntimeException if inserting does not succeed
     */
    public void insert(Sound sound) {
        // TODO throw exception if sound name already exists

        insertOrThrow(DBSchema.SoundTable.NAME, buildContentValues(sound));
    }

    /**
     * Updates this sound (which must already exist in the database) and updates the
     * soundboard links.
     */
    public void updateSoundAndSounboardLinks(SoundWithSelectableSoundboards sound) {
        update(sound.getSound());
        soundboardDao.updateLinks(sound);
    }

    /**
     * Updates this sound which has to exist in the database.
     */
    public void update(Sound sound) {
        int rowsUpdated = getDatabase().update(DBSchema.SoundTable.NAME,
                buildContentValues(sound),
                DBSchema.SoundTable.Cols.ID + " = ?",
                new String[]{sound.getId().toString()});

        if (rowsUpdated != 1) {
            throw new RuntimeException("Not exactly one sound with ID + " + sound.getId());
        }
    }

    private ContentValues buildContentValues(Sound sound) {
        ContentValues values = new ContentValues();
        values.put(DBSchema.SoundTable.Cols.ID, sound.getId().toString());
        values.put(DBSchema.SoundTable.Cols.NAME, sound.getName());
        // https://stackoverflow.com/questions/5861460/why-does-contentvalues-have-a-put-method-that-supports-boolean
        values.put(DBSchema.SoundTable.Cols.LOOP, sound.isLoop() ? 1 : 0);
        values.put(DBSchema.SoundTable.Cols.PATH, sound.getPath());
        values.put(DBSchema.SoundTable.Cols.VOLUME_PERCENTAGE, sound.getVolumePercentage());
        return values;
    }

    void deleteAllSounds() {
        getDatabase().delete(DBSchema.SoundTable.NAME, null, new String[]{});
    }
}
