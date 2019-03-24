package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.SoundTable;
import de.soundboardcrafter.model.Sound;

/**
 * Essentially a cursor over sounds.
 */
class SoundCursorWrapper extends CursorWrapper {
    SoundCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    Sound getSound() {
        UUID uuid = UUID.fromString(getString(getColumnIndex(SoundTable.Cols.ID)));
        String name = getString(getColumnIndex(SoundTable.Cols.NAME));
        String path = getString(getColumnIndex(SoundTable.Cols.PATH));
        int volumePercentage = getInt(getColumnIndex(SoundTable.Cols.VOLUME_PERCENTAGE));
        boolean loop = getInt(getColumnIndex(SoundTable.Cols.LOOP)) != 0;

        return new Sound(uuid, path, name, volumePercentage, loop);
    }
}
