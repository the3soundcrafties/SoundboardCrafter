package de.soundboardcrafter.dao;

import android.database.Cursor;

import androidx.annotation.WorkerThread;

/**
 * Essentially a cursor over sounds.
 */
@WorkerThread
class SoundboardCursorWrapper extends AbstractSimpleSoundboardCursorWrapper {
    SoundboardCursorWrapper(Cursor cursor) {
        super(cursor);
    }
}
