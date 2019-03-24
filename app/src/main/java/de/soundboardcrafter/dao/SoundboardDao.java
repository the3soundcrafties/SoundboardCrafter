package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Database Access Object for accessing Soundboards in the database
 */
public class SoundboardDao {
    private static SoundboardDao instance;

    private final SQLiteDatabase database;

    public static SoundboardDao getInstance(final Context context) {
        if (instance == null) {
            instance = new SoundboardDao(context);
        }

        return instance;
    }

    private SoundboardDao(Context context) {
        Context appContext = context.getApplicationContext();
        database = new DBHelper(appContext).getWritableDatabase();
    }

    public void clearDatabase() {
        unlinkAllSounds();
        deleteAllSounds();

        // TODO unlink and delete all games

        deleteAllSoundboards();
    }

    public void insertDummyData() {
        File directory = new File("/storage/emulated/0/soundboard crafter test songs");
        // get all the files from a directory
        File[] fList = directory.listFiles();
        ArrayList<Sound> soundList = new ArrayList<>();
        int volume = 10;
        for (File file : fList) {
            volume += 30 % 90 + 10;
            final String name;
            if (file.getName().contains("-")) {
                name = file.getName().substring(file.getName().indexOf("-") + 1, file.getName().indexOf("."));
            } else {
                name = file.getName().substring(0, file.getName().indexOf("."));
            }

            Sound newSound = new Sound(file.getAbsolutePath(), name, volume, false);
            soundList.add(newSound);
        }

        Soundboard soundboard = new Soundboard("my new Soundboard", soundList);

        insert(soundboard);
    }

    public ImmutableList<Soundboard> findAll() {
        final FullJoinSoundboardCursorWrapper cursor =
                new FullJoinSoundboardCursorWrapper(
                        rawQueryOrThrow(FullJoinSoundboardCursorWrapper.queryString()));
        try {
            ImmutableList.Builder<Soundboard> res = ImmutableList.builder();

            UUID lastSoundboardId = null;
            String lastSoundboardName = null;
            ArrayList<Sound> lastSounds = Lists.newArrayList();
            int lastIndex = -1; // index of the sound on the soundboard

            while (cursor.moveToNext()) {
                final UUID soundboardId = cursor.getSoundboardId();
                final String soundboardName = cursor.getSoundboardName();
                final int index;
                final UUID soundId;
                final String path;
                final String name;
                final int volumePercentage;
                final boolean loop;

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

                if (soundboardId.equals(lastSoundboardId)) {
                    // FIXME reuse existing sounds!
                    Sound sound = new Sound(soundId, path, name, volumePercentage, loop);

                    if (index != lastIndex + 1) {
                        throw new IllegalStateException("Gap in indexes of soundboard " + soundboardId + ". Expected next index " +
                                (lastIndex + 1) + ", but was " + index);
                    }

                    lastSoundboardId = soundboardId;
                    lastSoundboardName = soundboardName;
                    lastSounds.add(sound);
                    lastIndex = index;
                } else {
                    if (lastSoundboardId != null) {
                        lastSounds.trimToSize();
                        res.add(new Soundboard(lastSoundboardId, lastSoundboardName, Lists.newArrayList(lastSounds)));
                    }

                    lastSoundboardId = soundboardId;
                    lastSoundboardName = soundboardName;
                    lastSounds = Lists.newArrayList();

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
                res.add(new Soundboard(lastSoundboardId, lastSoundboardName, Lists.newArrayList(lastSounds)));
            }

            return res.build();
        } finally {
            cursor.close();
        }
    }

    /**
     * Finds these sounds by their IDs.
     *
     * @throws IllegalStateException if for some id, no sound exists (or more than one)
     */
    public ImmutableList<Sound> findSounds(UUID... soundIds) {
        return Stream.of(soundIds)
                .map(this::findSound)
                .collect(collectingAndThen(toList(), ImmutableList::copyOf));
        // TODO Use .collect(ImmutableList::toImmutableList) - why doesn't that work?
    }

    /**
     * Finds a sound by ID.
     *
     * @throws IllegalStateException if no sound with this ID exists - or more than one
     */
    public Sound findSound(UUID soundId) {
        SoundCursorWrapper cursor = querySounds(SoundTable.Cols.ID + " = ?",
                new String[]{soundId.toString()});
        try {
            if (!cursor.moveToNext()) {
                throw new IllegalStateException("No sound with ID " + soundId);
            }

            Sound res = cursor.getSound();
            if (cursor.moveToNext()) {
                throw new IllegalStateException("More than one sound with ID " + soundId);
            }

            return res;
        } finally {
            cursor.close();
        }
    }

    private SoundCursorWrapper querySounds(String whereClause, String[] whereArgs) {
        final Cursor cursor =
                database.query(
                        SoundTable.NAME,
                        null, // all columns
                        whereClause, whereArgs,
                        null,
                        null,
                        null
                );

        return new SoundCursorWrapper(cursor);
    }

    private Cursor rawQueryOrThrow(String queryString) {
        final Cursor cursor = database.rawQuery(queryString, new String[]{});
        if (cursor == null) {
            throw new RuntimeException("Could not query database: " + queryString);
        }
        return cursor;
    }

    /**
     * Inserts this <code>soundboard</code> an all its sounds into the database; <i>each of the
     * sounds is newly inserted, existing sounds cannot be reused in this method</i>.
     *
     * @throws RuntimeException if inserting does not succeed
     */
    private void insert(Soundboard soundboard) {
        insertSoundboard(soundboard.getId(), soundboard.getName());

        int index = 0;
        for (Sound sound : soundboard.getSounds()) {
            insertSound(sound);
            linkSoundToSoundboard(soundboard.getId(), index, sound.getId());
            index++;
        }
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
     * Inserts a the <code>sound</code> at this <code>position</code> in this
     * soundboard.
     *
     * @throws RuntimeException if inserting does not succeed
     */
    private void insertSound(Sound sound) {
        // TODO throw exception if sound name already exists

        insertOrThrow(SoundTable.NAME, buildContentValues(sound));
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
        database.delete(SoundboardSoundTable.NAME, null, new String[]{});
    }

    public void unlinkSound(Soundboard soundboard, int index) {
        int numDeleted = database.delete(SoundboardSoundTable.NAME,
                SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = ? and " +
                        SoundboardSoundTable.Cols.POS_INDEX + " = ? ",
                new String[]{soundboard.getId().toString(),
                        Integer.toString(index)});

        if (numDeleted != 1) {
            throw new RuntimeException("There wasn't exactly one sound at index " + index + ".");
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

            rowsUpdated = database.update(SoundboardSoundTable.NAME,
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


    public void updateSound(Sound sound) {
        int rowsUpdated = database.update(SoundTable.NAME,
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
        // https://stackoverflow.com/questions/5861460/why-does-contentvalues-have-a-put-method-that-supports-boolean
        values.put(SoundTable.Cols.LOOP, sound.isLoop() ? 1 : 0);
        values.put(SoundTable.Cols.PATH, sound.getPath());
        values.put(SoundTable.Cols.VOLUME_PERCENTAGE, sound.getVolumePercentage());
        return values;
    }

    private void deleteAllSounds() {
        database.delete(SoundTable.NAME, null, new String[]{});
    }

    private void deleteAllSoundboards() {
        database.delete(SoundboardTable.NAME, null, new String[]{});
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
}
