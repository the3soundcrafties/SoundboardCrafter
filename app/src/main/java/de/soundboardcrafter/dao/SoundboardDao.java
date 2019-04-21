package de.soundboardcrafter.dao;

import android.content.ContentValues;
import android.content.Context;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

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
        }

        return instance;
    }

    private SoundboardDao(@Nonnull Context context) {
        super(context);

        soundDao = SoundDao.getInstance(context);
    }

    public void clearDatabase() {
        unlinkAllSounds();
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
                Soundboard soundboard = new Soundboard(firstLevelFile.getName(), soundList);
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
            Soundboard soundboard = new Soundboard(automaticCreatedDir.getName(), sounds);
            insert(soundboard);
        }
    }

    private static Sound createSound(File soundFile) {
        return SoundFromFileCreationUtil.createSound(
                soundFile.getName(), soundFile.getAbsolutePath());
    }

    public ImmutableList<Soundboard> findAll() {
        final FullJoinSoundboardCursorWrapper cursor =
                new FullJoinSoundboardCursorWrapper(
                        rawQueryOrThrow(FullJoinSoundboardCursorWrapper.queryString()));
        try {
            ImmutableList.Builder<Soundboard> res = ImmutableList.builder();
            // The same Sound shall result in the same object
            Map<UUID, Sound> sounds = new HashMap<>();

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
                    // Reuse existing sounds.
                    @Nullable Sound sound = sounds.get(soundId);
                    if (sound == null) {
                        sound = new Sound(soundId, path, name, volumePercentage, loop);
                        sounds.put(soundId, sound);
                    }

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
     * Inserts this <code>soundboard</code> an all its sounds into the database; <i>each of the
     * sounds is newly inserted, existing sounds cannot be reused in this method</i>.
     *
     * @throws RuntimeException if inserting does not succeed
     */
    private void insert(Soundboard soundboard) {
        insertSoundboard(soundboard.getId(), soundboard.getName());

        int index = 0;
        for (Sound sound : soundboard.getSounds()) {
            soundDao.insertSound(sound);
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
}
