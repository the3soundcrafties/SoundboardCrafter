package de.soundboardcrafter.dao;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.UUID;

import de.soundboardcrafter.activity.sound.edit.common.SelectableSoundboard;
import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.Soundboard;

/**
 * Essentially a cursor over soundboards that might or might not be linked to one specific sound.
 */
class SelectableSoundboardCursorWrapper extends CursorWrapper {
    static String queryString() {
        return "SELECT sb." + SoundboardTable.Cols.ID
                + ", sb." + SoundboardTable.Cols.NAME
                + ", sbs." + SoundboardSoundTable.Cols.SOUND_ID
                + " " //
                + "FROM " + SoundboardTable.NAME + " sb "
                + "LEFT JOIN " + SoundboardSoundTable.NAME + " sbs "
                + "ON sbs." +
                SoundboardSoundTable.Cols.SOUNDBOARD_ID +
                " = sb." + SoundboardTable.Cols.ID + " "
                + "WHERE sbs." + SoundboardSoundTable.Cols.SOUND_ID + " = ? "
                + "ORDER BY sb." + SoundboardTable.Cols.NAME;
    }

    static String[] selectionArgs(UUID soundId) {
        return new String[]{soundId.toString()};
    }

    SelectableSoundboardCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    SelectableSoundboard getSelectableSoundboard() {
        UUID id = UUID.fromString(getString(getColumnIndex(SoundboardTable.Cols.ID)));
        String name = getString(getColumnIndex(SoundboardTable.Cols.NAME));
        boolean selected = !isNull(3);

        Soundboard soundboard = new Soundboard(id, name);
        return new SelectableSoundboard(soundboard, selected);
    }
}
