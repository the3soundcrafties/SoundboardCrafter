package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.model.IAudioLocation;
import de.soundboardcrafter.model.Sound;

/**
 * Essentially a cursor over sounds.
 */
@WorkerThread
class SoundCursorWrapper extends CursorWrapper {
    SoundCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    Sound getSound() {
        final UUID uuid = UUID.fromString(getString(getColumnIndex(SoundTable.Cols.ID)));
        final String name = getString(getColumnIndex(SoundTable.Cols.NAME));
        final IAudioLocation location = getLocation();
        final int volumePercentage = getInt(getColumnIndex(SoundTable.Cols.VOLUME_PERCENTAGE));
        final boolean loop = getInt(getColumnIndex(SoundTable.Cols.LOOP)) != 0;

        return new Sound(uuid, location, name, volumePercentage, loop);
    }

    /**
     * Retrieves the sound's location (in the file system or the asset folder).
     */
    private IAudioLocation getLocation() {
        final SoundTable.LocationType locationType =
                SoundTable.LocationType.valueOf(
                        getString(getColumnIndex(SoundTable.Cols.LOCATION_TYPE)));
        final String path = getString(getColumnIndex(SoundTable.Cols.PATH));

        return SoundTable.toAudioLocation(locationType, path);
    }
}
