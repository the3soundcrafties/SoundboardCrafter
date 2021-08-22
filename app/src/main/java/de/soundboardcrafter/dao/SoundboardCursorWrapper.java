package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.model.Soundboard;

/**
 * Essentially a cursor over sounds.
 */
@WorkerThread
class SoundboardCursorWrapper extends CursorWrapper {
    SoundboardCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    Soundboard getSoundboard() {
        UUID uuid = UUID.fromString(getString(getColumnIndex(DBSchema.SoundboardTable.Cols.ID)));
        String name = getString(getColumnIndex(DBSchema.SoundboardTable.Cols.NAME));
        boolean provided =
                getInt(getColumnIndex(DBSchema.SoundboardTable.Cols.PROVIDED)) == 0 ? false : true;

        return new Soundboard(uuid, name, provided);
    }
}
