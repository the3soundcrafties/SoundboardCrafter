package de.soundboardcrafter.dao;

import android.database.Cursor;

import androidx.annotation.WorkerThread;

/**
 * Essentially a cursor over sounds.
 */
@WorkerThread
class SoundCursorWrapper extends AbstractSimpleSoundCursorWrapper {
    SoundCursorWrapper(Cursor cursor) {
        super(cursor);
    }

}
