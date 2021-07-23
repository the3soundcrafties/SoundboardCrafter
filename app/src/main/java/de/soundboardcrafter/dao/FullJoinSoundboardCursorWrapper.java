package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardGameTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.IAudioLocation;
import de.soundboardcrafter.model.Sound;
import de.soundboardcrafter.model.Soundboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Essentially a cursor over a soundboard that's joined with all its sounds.
 */
@WorkerThread
class FullJoinSoundboardCursorWrapper extends CursorWrapper {
    static String queryString(@Nullable UUID gameId) {
        String res = "SELECT sb." + SoundboardTable.Cols.ID
                + ", sb." + SoundboardTable.Cols.NAME
                + ", sbs." + SoundboardSoundTable.Cols.POS_INDEX
                + ", s." + SoundTable.Cols.ID
                + ", s." + SoundTable.Cols.NAME
                + ", s." + SoundTable.Cols.LOCATION_TYPE
                + ", s." + SoundTable.Cols.PATH
                + ", s." + SoundTable.Cols.VOLUME_PERCENTAGE
                + ", s." + SoundTable.Cols.LOOP
                + " " //
                + "FROM " + SoundboardTable.NAME + " sb ";
        if (gameId != null) {
            res = res
                    + "JOIN " + SoundboardGameTable.NAME + " sg "
                    + "ON sg." + SoundboardGameTable.Cols.SOUNDBOARD_ID + " = sb."
                    + SoundboardTable.Cols.ID + " " //
                    + "AND sg." + SoundboardGameTable.Cols.GAME_ID + " = ? ";
        }

        res = res
                + "LEFT JOIN " + SoundboardSoundTable.NAME + " sbs "
                + "ON sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = sb."
                + SoundboardTable.Cols.ID + " "
                + "LEFT JOIN " + SoundTable.NAME + " s "
                + "ON s." + SoundTable.Cols.ID + " = sbs." + SoundboardSoundTable.Cols.SOUND_ID
                + " " //
                + "ORDER BY sb." + SoundboardTable.Cols.ID + ", sbs."
                + SoundboardSoundTable.Cols.POS_INDEX;

        return res;
    }

    FullJoinSoundboardCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    /**
     * Gets the current {@link Row} from the cursor.
     */
    Row getRow() {
        Soundboard soundboard = getSoundboard();
        @Nullable IndexedSound indexedSound = getIndexedSound();

        return new Row(soundboard, indexedSound);
    }

    /**
     * Gets the current {@link Soundboard} from the cursor.
     */
    private Soundboard getSoundboard() {
        UUID soundboardId = UUID.fromString(getString(0));
        String soundboardName = getString(1);
        return new Soundboard(soundboardId, soundboardName);
    }

    /**
     * Gets the current {@link IndexedSound} from the cursor.
     */
    @Nullable
    private IndexedSound getIndexedSound() {
        if (isNull(2)) {
            return null;
        }
        int index = getInt(2);

        UUID soundId = UUID.fromString(getString(3));
        String soundName = getString(4);

        SoundTable.LocationType soundLocationType =
                SoundTable.LocationType.valueOf(getString(5));
        String soundPath = getString(6);

        final IAudioLocation soundLocation =
                SoundTable.toAudioLocation(soundLocationType, soundPath);

        int soundVolumePercentage = getInt(7);
        boolean soundLoop = getInt(8) != 0;
        Sound sound = new Sound(soundId, soundLocation, soundName,
                soundVolumePercentage, soundLoop);

        return new IndexedSound(index, sound);
    }

    /**
     * A row of this wrapped cursor, containing a soundboard
     * and maybe an indexed sound.
     */
    static class Row {
        @NonNull
        private final Soundboard soundboard;

        @Nullable
        private final IndexedSound indexedSound;

        Row(@NonNull Soundboard soundboard,
            @Nullable IndexedSound indexedSound) {
            this.soundboard = checkNotNull(soundboard, "soundboard was null");
            this.indexedSound = indexedSound;
        }

        @NonNull
        public Soundboard getSoundboard() {
            return soundboard;
        }

        @Nullable
        IndexedSound getIndexedSound() {
            return indexedSound;
        }

        @Override
        @NonNull
        public String toString() {
            return "Row{" +
                    "soundboard=" + soundboard +
                    ", indexedSound=" + indexedSound +
                    '}';
        }
    }

    /**
     * Index and sound
     */
    static class IndexedSound {
        private final int index;

        @NonNull
        private final Sound sound;

        IndexedSound(int index, @NonNull Sound sound) {
            this.index = index;
            this.sound = checkNotNull(sound, "sound was null");
        }

        int getIndex() {
            return index;
        }

        @NonNull
        public Sound getSound() {
            return sound;
        }

        @Override
        @NonNull
        public String toString() {
            return "IndexedSound{" +
                    "index=" + index +
                    ", sound=" + sound +
                    '}';
        }
    }
}