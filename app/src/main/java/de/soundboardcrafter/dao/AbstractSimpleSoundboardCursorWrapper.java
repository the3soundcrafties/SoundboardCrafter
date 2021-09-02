package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.annotation.NonNull;

import java.util.UUID;

import de.soundboardcrafter.model.Soundboard;

public class AbstractSimpleSoundboardCursorWrapper extends CursorWrapper {
    public AbstractSimpleSoundboardCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    @NonNull
    Soundboard getSoundboard() {
        UUID uuid = UUID.fromString(getString(getColumnIndex(DBSchema.SoundboardTable.Cols.ID)));
        String name = getString(getColumnIndex(DBSchema.SoundboardTable.Cols.NAME));
        boolean provided = getInt(getColumnIndex(DBSchema.SoundboardTable.Cols.PROVIDED)) != 0;

        return new Soundboard(uuid, name, provided);
    }
}
