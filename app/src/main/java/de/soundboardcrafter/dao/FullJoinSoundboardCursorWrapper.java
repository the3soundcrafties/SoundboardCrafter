package de.soundboardcrafter.dao;

import android.database.Cursor;

import androidx.annotation.WorkerThread;

import java.io.Closeable;
import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;

/**
 * Essentially a cursor over a soundboard that's joined with all its sounds.
 */
@WorkerThread
class FullJoinSoundboardCursorWrapper implements Closeable {
    private final Cursor cursor;

    static String queryString() {
        return "SELECT sb." + SoundboardTable.Cols.ID
                + ", sb." + SoundboardTable.Cols.NAME
                + ", sbs." + SoundboardSoundTable.Cols.POS_INDEX
                + ", s." + SoundTable.Cols.ID
                + ", s." + SoundTable.Cols.NAME
                + ", s." + SoundTable.Cols.PATH
                + ", s." + SoundTable.Cols.VOLUME_PERCENTAGE
                + ", s." + SoundTable.Cols.LOOP
                + " " //
                + "FROM " + SoundboardTable.NAME + " sb "
                + "LEFT JOIN " + SoundboardSoundTable.NAME + " sbs "
                + "ON sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID + " = sb." + SoundboardTable.Cols.ID + " "
                + "LEFT JOIN " + SoundTable.NAME + " s "
                + "ON s." + SoundTable.Cols.ID + " = sbs." + SoundboardSoundTable.Cols.SOUND_ID + " " //
                + "ORDER BY sb." + SoundboardTable.Cols.ID + ", sbs." + SoundboardSoundTable.Cols.POS_INDEX;
    }

    FullJoinSoundboardCursorWrapper(Cursor cursor) {
        this.cursor = cursor;
    }

    boolean moveToNext() {
        return cursor.moveToNext();
    }

    UUID getSoundboardId() {
        return UUID.fromString(cursor.getString(0));
    }

    String getSoundboardName() {
        return cursor.getString(1);
    }

    boolean hasSound() {
        return !cursor.isNull(2);
    }

    /**
     * Returns the index of the sound in the soundboard. Call {@link #hasSound()} before
     * to check whether the current entry really has a sound - otherwise this method
     * might result in an exception.
     */
    int getIndex() {
        return cursor.getInt(2);
    }

    /**
     * Returns the ID of the sound. Call {@link #hasSound()} before
     * to check whether the current entry really has a sound - otherwise this method
     * might result in an exception.
     */
    UUID getSoundId() {
        return UUID.fromString(cursor.getString(3));
    }

    /**
     * Returns the name of the sound. Call {@link #hasSound()} before
     * to check whether the current entry really has a sound - otherwise this method
     * might result in an exception.
     */
    String getSoundName() {
        return cursor.getString(4);
    }

    /**
     * Returns the path of the sound. Call {@link #hasSound()} before
     * to check whether the current entry really has a sound - otherwise this method
     * might result in an exception.
     */
    String getSoundPath() {
        return cursor.getString(5);
    }

    /**
     * Returns the relative volume of the sound as a percentage. Call {@link #hasSound()} before
     * to check whether the current entry really has a sound - otherwise this method
     * might result in an exception.
     */
    int getSoundVolumePercentage() {
        return cursor.getInt(6);
    }

    /**
     * Returns whether the sound shall be played in a loop. Call {@link #hasSound()} before
     * to check whether the current entry really has a sound - otherwise this method
     * might result in an exception.
     */
    boolean isSoundLoop() {
        return cursor.getInt(7) != 0;
    }

    @Override
    public void close() {
        cursor.close();
    }
}
