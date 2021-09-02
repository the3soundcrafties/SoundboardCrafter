package de.soundboardcrafter.dao;

import android.database.Cursor;

import androidx.annotation.WorkerThread;

import java.util.UUID;

import de.soundboardcrafter.dao.DBSchema.SoundboardSoundTable;
import de.soundboardcrafter.dao.DBSchema.SoundboardTable;
import de.soundboardcrafter.model.SelectableModel;
import de.soundboardcrafter.model.Soundboard;

/**
 * Essentially a cursor over soundboards that might or might not be linked to one specific sound.
 */
@WorkerThread
class SelectableSoundboardCursorWrapper extends AbstractSimpleSoundboardCursorWrapper {
    static String queryString() {
        return "SELECT sb." + SoundboardTable.Cols.ID
                + ", sb." + SoundboardTable.Cols.NAME
                + ", sb." + SoundboardTable.Cols.PROVIDED
                + ", sbs." + SoundboardSoundTable.Cols.SOUND_ID
                + " " //
                + "FROM " + SoundboardTable.NAME + " sb "
                + "LEFT JOIN " + SoundboardSoundTable.NAME + " sbs "
                + "ON sbs." + SoundboardSoundTable.Cols.SOUNDBOARD_ID +
                " = sb." + SoundboardTable.Cols.ID + " " +
                "AND sbs." + SoundboardSoundTable.Cols.SOUND_ID + " = ? " +
                "ORDER BY sb." + SoundboardTable.Cols.NAME;
    }

    static String[] selectionArgs(UUID soundId) {
        return new String[]{soundId.toString()};
    }

    SelectableSoundboardCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    SelectableModel<Soundboard> getSelectableSoundboard() {
        boolean selected = !isNull(getColumnIndex(SoundboardSoundTable.Cols.SOUND_ID));

        return new SelectableModel<>(getSoundboard(), selected);
    }
}
