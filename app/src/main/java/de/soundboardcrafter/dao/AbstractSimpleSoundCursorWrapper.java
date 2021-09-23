package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.UUID;

import de.soundboardcrafter.model.AbstractAudioLocation;
import de.soundboardcrafter.model.Sound;

class AbstractSimpleSoundCursorWrapper extends CursorWrapper {
    AbstractSimpleSoundCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    Sound getSound() {
        final UUID uuid = UUID.fromString(getString(getColumnIndex(DBSchema.SoundTable.Cols.ID)));
        final String name = getString(getColumnIndex(DBSchema.SoundTable.Cols.NAME));
        final AbstractAudioLocation location = getLocation();
        final int volumePercentage =
                getInt(getColumnIndex(DBSchema.SoundTable.Cols.VOLUME_PERCENTAGE));
        final boolean loop = getInt(getColumnIndex(DBSchema.SoundTable.Cols.LOOP)) != 0;

        return new Sound(uuid, location, name, volumePercentage, loop);
    }

    /**
     * Retrieves the sound's location (in the file system or the asset folder).
     */
    private AbstractAudioLocation getLocation() {
        final DBSchema.SoundTable.LocationType locationType =
                DBSchema.SoundTable.LocationType.valueOf(
                        getString(getColumnIndex(DBSchema.SoundTable.Cols.LOCATION_TYPE)));
        final String path = getString(getColumnIndex(DBSchema.SoundTable.Cols.PATH));

        return DBSchema.SoundTable.toAudioLocation(locationType, path);
    }
}
